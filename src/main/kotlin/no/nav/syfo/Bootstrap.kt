package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.prometheus.client.hotspot.DefaultExports
import java.io.StringWriter
import java.time.Duration
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.Marshaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.helse.arena.sykemelding.ArenaSykmelding
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.arena.createArenaSykmelding
import no.nav.syfo.client.HttpClients
import no.nav.syfo.client.SmtssClient
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.metrics.ARENA_EVENT_COUNTER
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.mq.MqTlsUtils
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import no.nav.syfo.util.LoggingMeta
import no.nav.syfo.util.TrackableException
import no.nav.syfo.util.arenaSykmeldingMarshaller
import no.nav.syfo.util.wrapExceptions
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmarena")

data class JournaledReceivedSykmelding(
    val receivedSykmelding: ByteArray,
    val journalpostId: String
)

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val serviceUser = ServiceUser()
    MqTlsUtils.getMqTlsConfig().forEach { key, value ->
        System.setProperty(key as String, value as String)
    }
    val applicationState = ApplicationState()
    val applicationEngine =
        createApplicationEngine(
            env,
            applicationState,
        )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)

    DefaultExports.initialize()

    launchListeners(env, applicationState, serviceUser)
    applicationServer.start()
}

@DelicateCoroutinesApi
fun createListener(
    applicationState: ApplicationState,
    action: suspend CoroutineScope.() -> Unit
): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: TrackableException) {
            log.error(
                "En uhåndtert feil oppstod, applikasjonen restarter {}",
                fields(e.loggingMeta),
                e.cause
            )
        } finally {
            applicationState.ready = false
            applicationState.alive = false
        }
    }

@DelicateCoroutinesApi
fun launchListeners(
    env: Environment,
    applicationState: ApplicationState,
    serviceUser: ServiceUser,
) {
    createListener(applicationState) {
        connectionFactory(env)
            .createConnection(serviceUser.serviceuserUsername, serviceUser.serviceuserPassword)
            .use { connection ->
                connection.start()
                val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                val arenaQueue = session.createQueue(env.arenaQueue)
                val arenaProducer = session.createProducer(arenaQueue)

                val kafkaAivenConsumer =
                    KafkaConsumer<String, String>(
                        KafkaUtils.getAivenKafkaConfig()
                            .toConsumerConfig(
                                "${env.applicationName}-consumer",
                                valueDeserializer = StringDeserializer::class
                            )
                            .also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none" },
                    )
                kafkaAivenConsumer.subscribe(listOf(env.privatArenaInputTopic))

                val httpClients = HttpClients(env)

                blockingApplicationLogic(
                    applicationState,
                    kafkaAivenConsumer,
                    arenaProducer,
                    session,
                    httpClients.smtssClient
                )
            }
    }
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    kafkaAivenConsumer: KafkaConsumer<String, String>,
    arenaProducer: MessageProducer,
    session: Session,
    smtssClient: SmtssClient,
) {
    while (applicationState.ready) {
        kafkaAivenConsumer.poll(Duration.ofMillis(0)).forEach { consumerRecord ->
            val journaledReceivedSykmelding: JournaledReceivedSykmelding =
                objectMapper.readValue(consumerRecord.value())
            val receivedSykmelding: ReceivedSykmelding =
                objectMapper.readValue(journaledReceivedSykmelding.receivedSykmelding)
            val loggingMeta =
                LoggingMeta(
                    mottakId = receivedSykmelding.navLogId,
                    orgNr = receivedSykmelding.legekontorOrgNr,
                    msgId = receivedSykmelding.msgId,
                    sykmeldingId = receivedSykmelding.sykmelding.id,
                )
            handleMessage(
                receivedSykmelding,
                journaledReceivedSykmelding,
                arenaProducer,
                session,
                loggingMeta,
                "aiven",
                smtssClient
            )
        }
        delay(1)
    }
}

suspend fun handleMessage(
    receivedSykmelding: ReceivedSykmelding,
    journaledReceivedSykmelding: JournaledReceivedSykmelding,
    arenaProducer: MessageProducer,
    session: Session,
    loggingMeta: LoggingMeta,
    source: String,
    smtssClient: SmtssClient,
) {
    wrapExceptions(loggingMeta) {
        log.info("Received a SM2013 from $source, going to Arena rules {}", fields(loggingMeta))

        val tssId =
            smtssClient.findBestTssIdArena(
                receivedSykmelding.personNrLege,
                receivedSykmelding.legekontorOrgName,
                loggingMeta,
                receivedSykmelding.sykmelding.id
            )

        val validationRuleResults =
            ValidationRuleChain.values()
                .executeFlow(
                    receivedSykmelding.sykmelding,
                    RuleMetadata(
                        receivedDate = receivedSykmelding.mottattDato,
                        signatureDate = receivedSykmelding.sykmelding.signaturDato,
                        rulesetVersion = receivedSykmelding.rulesetVersion,
                    ),
                )

        val results = listOf(validationRuleResults).flatten()

        log.info("Rules hit {}, {}", results.map { it.name }, fields(loggingMeta))

        when (results.firstOrNull()) {
            null -> log.info("Message is NOT sendt to Arena {}", fields(loggingMeta))
            else ->
                sendArenaSykmelding(
                    arenaProducer,
                    session,
                    createArenaSykmelding(
                        receivedSykmelding,
                        results,
                        journaledReceivedSykmelding.journalpostId,
                        tssId
                    ),
                    loggingMeta,
                )
        }
    }
}

fun Marshaller.toString(input: Any): String =
    StringWriter().use {
        marshal(input, it)
        it.toString()
    }

fun sendArenaSykmelding(
    producer: MessageProducer,
    session: Session,
    arenaSykmelding: ArenaSykmelding,
    loggingMeta: LoggingMeta,
) =
    producer.send(
        session.createTextMessage().apply {
            text = arenaSykmeldingMarshaller.toString(arenaSykmelding)
            ARENA_EVENT_COUNTER.inc()
            log.info("Message sendt to Arena {}", fields(loggingMeta))
        },
    )

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T

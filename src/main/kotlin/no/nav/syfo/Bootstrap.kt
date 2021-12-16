package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.helse.arenaSykemelding.ArenaSykmelding
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.arena.createArenaSykmelding
import no.nav.syfo.kafka.envOverrides
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toStreamsConfig
import no.nav.syfo.metrics.ARENA_EVENT_COUNTER
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import no.nav.syfo.sak.avro.RegisterJournal
import no.nav.syfo.util.LoggingMeta
import no.nav.syfo.util.TrackableException
import no.nav.syfo.util.arenaSykmeldingMarshaller
import no.nav.syfo.util.wrapExceptions
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.Joined
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.Marshaller

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmarena")

data class JournaledReceivedSykmelding(val receivedSykmelding: ByteArray, val journalpostId: String)

fun main() {
    val env = Environment()
    val vaultServiceUser = VaultServiceUser()
    val applicationState = ApplicationState()
    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()

    DefaultExports.initialize()

    val kafkaBaseConfig = loadBaseConfig(env, vaultServiceUser).envOverrides()
    kafkaBaseConfig["auto.offset.reset"] = "none"
    val consumerProperties = kafkaBaseConfig.toConsumerConfig(
        "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    )
    val streamProperties = kafkaBaseConfig.toStreamsConfig(env.applicationName, valueSerde = GenericAvroSerde::class)

    launchListeners(env, consumerProperties, applicationState, vaultServiceUser, streamProperties)
}

fun createKafkaStream(streamProperties: Properties, env: Environment): KafkaStreams {
    val streamsBuilder = StreamsBuilder()
    val specificSerdeConfig = SpecificAvroSerde<RegisterJournal>().apply {
        configure(
            mapOf(
                "schema.registry.url" to streamProperties["schema.registry.url"]!!
            ),
            false
        )
    }

    val sm2013InputStream = streamsBuilder.stream<String, String>(
        listOf(
            env.kafkasm2013ManualHandlingTopic,
            env.kafkaSm2013AutomaticDigitalHandlingTopic
        ),
        Consumed.with(Serdes.String(), Serdes.String())
    )

    val journalCreatedTaskStream = streamsBuilder.stream<String, RegisterJournal>(
        env.kafkasm2013oppgaveJournalOpprettetTopic, Consumed.with(Serdes.String(), specificSerdeConfig)
    )

    val joinWindow = JoinWindows.of(Duration.ofDays(14))
        .grace(Duration.ofDays(31))

    val joined = Joined.with(
        Serdes.String(), Serdes.String(), specificSerdeConfig
    )

    sm2013InputStream.filter { _, value ->
        value?.let { objectMapper.readValue<ReceivedSykmelding>(value).merknader?.any { it.type == "UNDER_BEHANDLING" } != true } ?: true
    }.join(journalCreatedTaskStream, { sm2013, journalCreated ->
        objectMapper.writeValueAsString(
            JournaledReceivedSykmelding(
                receivedSykmelding = sm2013.toByteArray(Charsets.UTF_8),
                journalpostId = journalCreated.journalpostId
            )
        )
    }, joinWindow, joined)
        .to(env.kafkasm2013ArenaInput, Produced.with(Serdes.String(), Serdes.String()))

    return KafkaStreams(streamsBuilder.build(), streamProperties)
}

fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: TrackableException) {
            log.error("En uhåndtert feil oppstod, applikasjonen restarter {}", fields(e.loggingMeta), e.cause)
        } finally {
            applicationState.alive = false
        }
    }

fun launchListeners(
    env: Environment,
    consumerProperties: Properties,
    applicationState: ApplicationState,
    vaultServiceUser: VaultServiceUser,
    streamProperties: Properties
) {
    createListener(applicationState) {
        connectionFactory(env).createConnection(vaultServiceUser.serviceuserUsername, vaultServiceUser.serviceuserPassword).use { connection ->
            connection.start()
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val arenaQueue = session.createQueue(env.arenaQueue)
            val arenaProducer = session.createProducer(arenaQueue)

            val kafkaStream = createKafkaStream(streamProperties, env)

            kafkaStream.setUncaughtExceptionHandler { _, err ->
                log.error("Caught exception in stream: ${err.message}", err)
                kafkaStream.close()
                throw err
            }

            kafkaStream.setStateListener { newState, oldState ->
                log.info("From state={} to state={}", oldState, newState)

                if (newState == KafkaStreams.State.ERROR) {
                    // if the stream has died there is no reason to keep spinning
                    log.error("Closing stream because it went into error state")
                    kafkaStream.close(30, TimeUnit.SECONDS)
                    log.error("Restarter applikasjon")
                    applicationState.ready = false
                    applicationState.alive = false
                }
            }

            kafkaStream.start()

            val kafkaConsumer = KafkaConsumer<String, String>(consumerProperties)
            kafkaConsumer.subscribe(listOf(env.kafkasm2013ArenaInput))

            applicationState.ready = true

            blockingApplicationLogic(applicationState, kafkaConsumer, arenaProducer, session)
        }
    }
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    kafkaconsumer: KafkaConsumer<String, String>,
    arenaProducer: MessageProducer,
    session: Session
) {
    while (applicationState.ready) {
        kafkaconsumer.poll(Duration.ofMillis(0)).forEach { consumerRecord ->
            val journaledReceivedSykmelding: JournaledReceivedSykmelding = objectMapper.readValue(consumerRecord.value())
            val receivedSykmelding: ReceivedSykmelding = objectMapper.readValue(journaledReceivedSykmelding.receivedSykmelding)
            val loggingMeta = LoggingMeta(
                mottakId = receivedSykmelding.navLogId,
                orgNr = receivedSykmelding.legekontorOrgNr,
                msgId = receivedSykmelding.msgId,
                sykmeldingId = receivedSykmelding.sykmelding.id
            )
            handleMessage(receivedSykmelding, journaledReceivedSykmelding, arenaProducer, session, loggingMeta)
        }
        delay(1)
    }
}

suspend fun handleMessage(
    receivedSykmelding: ReceivedSykmelding,
    journaledReceivedSykmelding: JournaledReceivedSykmelding,
    arenaProducer: MessageProducer,
    session: Session,
    loggingMeta: LoggingMeta
) {
    wrapExceptions(loggingMeta) {
        log.info("Received a SM2013, going to Arena rules {}", fields(loggingMeta))

        val validationRuleResults = ValidationRuleChain.values().executeFlow(
            receivedSykmelding.sykmelding,
            RuleMetadata(
                receivedDate = receivedSykmelding.mottattDato,
                signatureDate = receivedSykmelding.sykmelding.signaturDato,
                rulesetVersion = receivedSykmelding.rulesetVersion
            )
        )

        val results = listOf(validationRuleResults).flatten()

        log.info("Rules hit {}, {}", results.map { it.name }, fields(loggingMeta))

        when (results.firstOrNull()) {
            null -> log.info("Message is NOT sendt to arena {}", fields(loggingMeta))
            else -> sendArenaSykmelding(
                arenaProducer, session,
                createArenaSykmelding(receivedSykmelding, results, journaledReceivedSykmelding.journalpostId),
                loggingMeta
            )
        }
    }
}

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

fun sendArenaSykmelding(
    producer: MessageProducer,
    session: Session,
    arenaSykmelding: ArenaSykmelding,
    loggingMeta: LoggingMeta
) = producer.send(
    session.createTextMessage().apply {
        text = arenaSykmeldingMarshaller.toString(arenaSykmelding)
        ARENA_EVENT_COUNTER.inc()
        log.info("Message is sendt to arena {}", fields(loggingMeta))
    }
)

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T

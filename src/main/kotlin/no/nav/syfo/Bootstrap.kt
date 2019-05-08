package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.arenaSykemelding.ArenaSykmelding
import no.nav.syfo.api.registerNaisApi
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
import no.nav.syfo.util.arenaSykmeldingMarshaller
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
import java.nio.file.Paths
import java.time.Duration
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.Marshaller

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmarena")

data class JournaledReceivedSykmelding(val receivedSykmelding: ByteArray, val journalpostId: String)

fun main() = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    DefaultExports.initialize()
    val env = Environment()
    val credentials = objectMapper.readValue<VaultCredentials>(Paths.get("/var/run/secrets/nais.io/vault/credentials.json").toFile())
    val applicationState = ApplicationState()

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState)
    }.start(wait = false)

    val kafkaBaseConfig = loadBaseConfig(env, credentials).envOverrides()
    val consumerProperties = kafkaBaseConfig.toConsumerConfig(
            "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class)
    val streamProperties =
            kafkaBaseConfig.toStreamsConfig(env.applicationName, valueSerde = Serdes.String()::class)
    val kafkaStream = createKafkaStream(streamProperties, env)

    kafkaStream.start()

    connectionFactory(env).createConnection(credentials.mqUsername, credentials.mqPassword).use { connection ->
        connection.start()

        launchListeners(env, consumerProperties, applicationState, connection)

        Runtime.getRuntime().addShutdownHook(Thread {
            kafkaStream.close()
            applicationServer.stop(10, 10, TimeUnit.SECONDS)
        })
    }
}

fun createKafkaStream(streamProperties: Properties, env: Environment): KafkaStreams {
    val streamsBuilder = StreamsBuilder()
    val specificSerdeConfig = SpecificAvroSerde<RegisterJournal>().apply {
        configure(mapOf(
                "schema.registry.url" to streamProperties["schema.registry.url"]!!
        ), false)
    }

    val sm2013InputStream = streamsBuilder.stream<String, String>(listOf(
            env.kafkasm2013ManualHandlingTopic,
            env.kafkaSm2013AutomaticDigitalHandlingTopic), Consumed.with(Serdes.String(), Serdes.String()))

    val journalCreatedTaskStream = streamsBuilder.stream<String, RegisterJournal>(
            env.kafkasm2013oppgaveJournalOpprettetTopic, Consumed.with(Serdes.String(), specificSerdeConfig))

    val joinWindow = JoinWindows.of(TimeUnit.DAYS.toMillis(14))
            .until(TimeUnit.DAYS.toMillis(31))

    val joined = Joined.with(
            Serdes.String(), Serdes.String(), specificSerdeConfig)

    sm2013InputStream.join(journalCreatedTaskStream, { sm2013, journalCreated ->
        objectMapper.writeValueAsString(
                JournaledReceivedSykmelding(
                        receivedSykmelding = sm2013.toByteArray(Charsets.UTF_8),
                        journalpostId = journalCreated.journalpostId
                ))
    }, joinWindow, joined)
            .to(env.kafkasm2013ArenaInput, Produced.with(Serdes.String(), Serdes.String()))

    return KafkaStreams(streamsBuilder.build(), streamProperties)
}

@KtorExperimentalAPI
fun CoroutineScope.launchListeners(
    env: Environment,
    consumerProperties: Properties,
    applicationState: ApplicationState,
    connection: Connection
) {
    try {
        val listeners = (1..env.applicationThreads).map {
            launch {

                val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                val arenaQueue = session.createQueue(env.arenaQueue)
                val arenaProducer = session.createProducer(arenaQueue)

                val kafkaConsumer = KafkaConsumer<String, String>(consumerProperties)
                kafkaConsumer.subscribe(listOf(env.kafkasm2013ArenaInput))

                blockingApplicationLogic(applicationState, kafkaConsumer, arenaProducer, session)
            }
        }.toList()

        applicationState.initialized = true
        runBlocking { listeners.forEach { it.join() } }
    } finally {
        applicationState.running = false
    }
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    kafkaconsumer: KafkaConsumer<String, String>,
    arenaProducer: MessageProducer,
    session: Session
) {
        while (applicationState.running) {
            var logValues = arrayOf(
                    StructuredArguments.keyValue("smId", "missing"),
                    StructuredArguments.keyValue("organizationNumber", "missing"),
                    StructuredArguments.keyValue("msgId", "missing"),
                    StructuredArguments.keyValue("sykmeldingId", "missing")
            )

            val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
                "{}"
            }

            kafkaconsumer.poll(Duration.ofMillis(0)).forEach {
                val journaledReceivedSykmelding: JournaledReceivedSykmelding = objectMapper.readValue(it.value())
                val receivedSykmelding: ReceivedSykmelding = objectMapper.readValue(journaledReceivedSykmelding.receivedSykmelding)
                logValues = arrayOf(
                        StructuredArguments.keyValue("mottakId", receivedSykmelding.navLogId),
                        StructuredArguments.keyValue("organizationNumber", receivedSykmelding.legekontorOrgNr),
                        StructuredArguments.keyValue("msgId", receivedSykmelding.msgId),
                        StructuredArguments.keyValue("sykmeldingId", receivedSykmelding.sykmelding.id)
                )

                log.info("Received a SM2013, going to Arena rules, $logKeys", *logValues)

                val validationRuleResults = ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, RuleMetadata(
                        receivedDate = receivedSykmelding.mottattDato,
                        signatureDate = receivedSykmelding.sykmelding.signaturDato,
                        rulesetVersion = receivedSykmelding.rulesetVersion
                ))

                val results = listOf(validationRuleResults).flatten()

                log.info("Rules hit {}, $logKeys", results.map { it.name }, *logValues)

                when (results.firstOrNull()) {
                    null -> log.info("Message is NOT sendt to arena  $logKeys", *logValues)
                    else -> sendArenaSykmelding(arenaProducer, session,
                            createArenaSykmelding(receivedSykmelding, results, journaledReceivedSykmelding.journalpostId),
                            logKeys, logValues)
                }
            }
            delay(100)
        }
    }

fun Application.initRouting(applicationState: ApplicationState) {
    routing {
        registerNaisApi(
                readynessCheck = {
                    applicationState.initialized
                },
                livenessCheck = {
                    applicationState.running
                }
        )
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
    logKeys: String,
    logValues: Array<StructuredArgument>
) = producer.send(session.createTextMessage().apply {
    text = arenaSykmeldingMarshaller.toString(arenaSykmelding)
    ARENA_EVENT_COUNTER.inc()
    log.info("Message is sendt to arena $logKeys", *logValues)
})

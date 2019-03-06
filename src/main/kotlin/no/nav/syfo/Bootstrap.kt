package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kafka.server.KafkaConfig
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.arenaSykemelding.ArenaSykmelding
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.arena.createArenaSykmelding
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.sak.avro.RegisterJournal
import no.nav.syfo.util.arenaSykmeldingMarshaller
import no.nav.syfo.util.connectionFactory
import no.nav.syfo.util.loadBaseConfig
import no.nav.syfo.util.toConsumerConfig
import no.nav.syfo.util.toStreamsConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.Joined
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.StringWriter
import java.time.Duration
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.Marshaller

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmarena")

data class JournaledReceivedSykmelding(val receivedSykmelding: ByteArray, val journalpostId: String)

fun main(args: Array<String>) = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val config: ApplicationConfig = objectMapper.readValue(File(System.getenv("CONFIG_FILE")))
    val credentials: VaultCredentials = objectMapper.readValue(vaultApplicationPropertiesPath.toFile())
    val applicationState = ApplicationState()

    val applicationServer = embeddedServer(Netty, config.applicationPort) {
        initRouting(applicationState)
    }.start(wait = false)

    val kafkaBaseConfig = loadBaseConfig(config, credentials)
    val consumerProperties = kafkaBaseConfig.toConsumerConfig("${config.applicationName}-consumer", valueDeserializer = KafkaAvroDeserializer::class)
    val streamProperties = kafkaBaseConfig.toStreamsConfig(config.applicationName, valueSerde = GenericAvroSerde::class)
    val kafkaStream = createKafkaStream(streamProperties, config)
    kafkaStream.start()

    connectionFactory(config).createConnection(credentials.mqUsername, credentials.mqPassword).use { connection ->
        connection.start()

        try {
            val listeners = (1..config.applicationThreads).map {
                launch {

                    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                    val arenaQueue = session.createQueue(config.arenaQueue)
                    val arenaProducer = session.createProducer(arenaQueue)

                    val kafkaConsumer = KafkaConsumer<String, String>(consumerProperties)
                    kafkaConsumer.subscribe(listOf(config.kafkasm2013ArenaInput))

                    blockingApplicationLogic(applicationState, kafkaConsumer, arenaProducer, session)
                }
        }.toList()

        runBlocking {
            Runtime.getRuntime().addShutdownHook(Thread {
                kafkaStream.close()
                applicationServer.stop(10, 10, TimeUnit.SECONDS)
            })

            applicationState.initialized = true
            listeners.forEach { it.join() }
        }
        } finally {
        applicationState.running = false
        }
    }
}

fun createKafkaStream(streamProperties: Properties, config: ApplicationConfig): KafkaStreams {
    val streamsBuilder = StreamsBuilder()

    val sm2013InputStream = streamsBuilder.stream<String, String>(listOf(
            config.kafkaSm2013manuelDigitalManuellTopic,
            config.kafkaSm2013AutomaticPapirmottakTopic,
            config.kafkaSm2013manuellPapirmottakTopic,
            config.kafkaSm2013AutomaticDigitalHandlingTopic))

    val journalCreatedTaskStream = streamsBuilder.stream<String, RegisterJournal>(config.kafkasm2013oppgaveJournalOpprettetTopic)
    KafkaConfig.LogRetentionTimeMillisProp()

    val joinWindow = JoinWindows.of(TimeUnit.HOURS.toMillis(11))
    val joined = Joined.with(Serdes.String(), Serdes.String(), SpecificAvroSerde<RegisterJournal>())

    sm2013InputStream.join(journalCreatedTaskStream, { sm2013, journalCreated ->
        objectMapper.writeValueAsString(JournaledReceivedSykmelding(sm2013.toByteArray(Charsets.UTF_8), journalCreated.journalpostId))
    }, joinWindow, joined).to(config.kafkasm2013ArenaInput)

    return KafkaStreams(streamsBuilder.build(), streamProperties)
}

suspend fun blockingApplicationLogic(applicationState: ApplicationState, kafkaconsumer: KafkaConsumer<String, String>, arenaProducer: MessageProducer, session: Session) {
        while (applicationState.running) {
            var logValues = arrayOf(
                    StructuredArguments.keyValue("smId", "missing"),
                    StructuredArguments.keyValue("organizationNumber", "missing"),
                    StructuredArguments.keyValue("msgId", "missing")
            )

            val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
                "{}"
            }

            kafkaconsumer.poll(Duration.ofMillis(0)).forEach {
                val journaledReceivedSykmelding: JournaledReceivedSykmelding = objectMapper.readValue(it.value())
                val receivedSykmelding: ReceivedSykmelding = objectMapper.readValue(journaledReceivedSykmelding.receivedSykmelding)
                logValues = arrayOf(
                        StructuredArguments.keyValue("smId", receivedSykmelding.navLogId),
                        StructuredArguments.keyValue("organizationNumber", receivedSykmelding.legekontorOrgNr),
                        StructuredArguments.keyValue("msgId", receivedSykmelding.msgId)
                )

                log.info("Received a SM2013, going to Arena rules, $logKeys", *logValues)

                val validationRuleResults = ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, RuleMetadata(
                        receivedDate = receivedSykmelding.mottattDato,
                        signatureDate = receivedSykmelding.signaturDato,
                        rulesetVersion = receivedSykmelding.rulesetVersion
                ))

                val results = listOf(validationRuleResults).flatten()

                log.info("Rules hit {}, $logKeys", results.map { it.name }, *logValues)

                // TODO map rules to arena hendelse
                when (results.firstOrNull()) {
                    null -> log.info("Message is NOT sendt to arena  $logKeys", *logValues)
                    else -> sendArenaSykmelding(arenaProducer, session, createArenaSykmelding(receivedSykmelding, results, journaledReceivedSykmelding.journalpostId), logKeys, logValues)
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
    log.info("Message is sendt to arena $logKeys", *logValues)
})

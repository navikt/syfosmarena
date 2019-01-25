package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.arenaSykemelding.ArenaSykmelding
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.util.connectionFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.jms.MessageProducer
import javax.jms.Session

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmarena")

fun main(args: Array<String>) = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val config: ApplicationConfig = objectMapper.readValue(File(System.getenv("CONFIG_FILE")))
    val credentials: VaultCredentials = objectMapper.readValue(vaultApplicationPropertiesPath.toFile())
    val applicationState = ApplicationState()

    val applicationServer = embeddedServer(Netty, config.applicationPort) {
        initRouting(applicationState)
    }.start(wait = false)

    connectionFactory(config).createConnection(credentials.mqUsername, credentials.mqPassword).use { connection ->
        connection.start()

        try {
            val listeners = (1..config.applicationThreads).map {
                launch {

                    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                    val arenaQueue = session.createQueue(config.arenaQueue)
                    val arenaProducer = session.createProducer(arenaQueue)

                    val consumerProperties = readConsumerConfig(config, credentials, valueDeserializer = StringDeserializer::class)
                    val kafkaconsumer = KafkaConsumer<String, String>(consumerProperties)
                    kafkaconsumer.subscribe(listOf(config.kafkaSm2013AutomaticPapirmottakTopic, config.kafkaSm2013AutomaticDigitalHandlingTopic))
                    blockingApplicationLogic(applicationState, kafkaconsumer, arenaProducer, session)
                }
        }.toList()

        runBlocking {
            Runtime.getRuntime().addShutdownHook(Thread {
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
                val receivedSykmelding: ReceivedSykmelding = objectMapper.readValue(it.value())
                logValues = arrayOf(
                        StructuredArguments.keyValue("msgId", receivedSykmelding.msgId),
                        StructuredArguments.keyValue("smId", receivedSykmelding.navLogId),
                        StructuredArguments.keyValue("orgNr", receivedSykmelding.legekontorOrgNr)
                )

                log.info("Received a SM2013, going to Arena rules, $logKeys", *logValues)

                val validationRuleResults: List<Rule<Any>> = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                        ValidationRuleChain.values().toList()
                ).flatten().executeFlow(receivedSykmelding.sykmelding, RuleMetadata(
                        receivedDate = receivedSykmelding.mottattDato,
                        signatureDate = receivedSykmelding.signaturDato
                ))

                val results = listOf(validationRuleResults).flatten()

                // TODO map rules to arena hendelse

                val arenaSykmelding_1 = ArenaSykmelding().apply {
                }
            }
        }
        delay(100)
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

package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toStreamsConfig
import no.nav.syfo.model.JournalKafkaMessage
import no.nav.syfo.model.JournaledReceivedSykmelding
import no.nav.syfo.model.ReceivedSykmelding
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.JoinWindows
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmarena-stream")

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)

    startKafkaAivenStream(env, applicationState)
    applicationServer.start()
}

fun startKafkaAivenStream(env: Environment, applicationState: ApplicationState) {
    val streamsBuilder = StreamsBuilder()
    val streamProperties = KafkaUtils.getAivenKafkaConfig().toStreamsConfig(env.applicationName, Serdes.String()::class, Serdes.String()::class)
    streamProperties[StreamsConfig.APPLICATION_ID_CONFIG] = env.applicationId
    val inputStream = streamsBuilder.stream(
        listOf(
            env.okSykmeldingTopic,
            env.manuellSykmeldingTopic
        ),
        Consumed.with(Serdes.String(), Serdes.String())
    ).filter { _, value ->
        value?.let { objectMapper.readValue<ReceivedSykmelding>(value).skalBehandles() } ?: true
    }

    val journalOpprettetStream = streamsBuilder.stream(
        listOf(
            env.journalOpprettetTopic
        ),
        Consumed.with(Serdes.String(), Serdes.String())
    )

    val joinWindow = JoinWindows.of(Duration.ofDays(14))

    inputStream.join(
        journalOpprettetStream,
        { sm2013, journalKafkaMessage ->
            log.info("streamed to aiven")
            objectMapper.writeValueAsString(
                JournaledReceivedSykmelding(
                    receivedSykmelding = sm2013.toByteArray(Charsets.UTF_8),
                    journalpostId = objectMapper.readValue<JournalKafkaMessage>(journalKafkaMessage).journalpostId
                )
            )
        },
        joinWindow
    ).to(env.privatArenaInputTopic)

    val stream = KafkaStreams(streamsBuilder.build(), streamProperties)
    stream.setUncaughtExceptionHandler { err ->
        log.error("Aiven: Caught exception in stream: ${err.message}", err)
        stream.close(Duration.ofSeconds(30))
        applicationState.ready = false
        applicationState.alive = false
        throw err
    }

    stream.setStateListener { newState, oldState ->
        log.info("Aiven: From state={} to state={}", oldState, newState)
        if (newState == KafkaStreams.State.ERROR) {
            // if the stream has died there is no reason to keep spinning
            log.error("Aiven: Closing stream because it went into error state")
            stream.close(Duration.ofSeconds(30))
            log.error("Aiven: Restarter applikasjon")
            applicationState.ready = false
            applicationState.alive = false
        }
    }
    stream.start()
}

fun ReceivedSykmelding.skalBehandles(): Boolean {
    return merknader?.any { it.type == "UNDER_BEHANDLING" } != true && utenlandskSykmelding == null
}

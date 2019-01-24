package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.api.registerRuleApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun doReadynessCheck(): Boolean {
    // Do validation
    return true
}

data class ApplicationState(var running: Boolean = true)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmarena")

fun main(args: Array<String>) = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val config: ApplicationConfig = objectMapper.readValue(File(System.getenv("CONFIG_FILE")))
    // val credentials: VaultCredentials = objectMapper.readValue(vaultApplicationPropertiesPath.toFile())
    val applicationState = ApplicationState()

    val applicationServer = embeddedServer(Netty, config.applicationPort) {
        initRouting(applicationState)
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationServer.stop(10, 10, TimeUnit.SECONDS)
    })
}

fun Application.initRouting(applicationState: ApplicationState) {
    routing {
        registerNaisApi(readynessCheck = ::doReadynessCheck, livenessCheck = { applicationState.running })
        registerRuleApi()
    }
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
            }
        }
    }

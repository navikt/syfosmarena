package no.nav.syfo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.api.registerNaisApi
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.ActiveMQServers
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import javax.jms.ConnectionFactory
import javax.naming.InitialContext

object SelftestSpek : Spek({
    val applicationState = ApplicationState()

    val activeMQServer = ActiveMQServers.newActiveMQServer(ConfigurationImpl()
            .setPersistenceEnabled(false)
            .setJournalDirectory("target/data/journal")
            .setSecurityEnabled(false)
            .addAcceptorConfiguration("invm", "vm://0"))
    activeMQServer.start()

    val credentials = VaultCredentials("", "", "", "")

    val initialContext = InitialContext()
    val connectionFactory = initialContext.lookup("ConnectionFactory") as ConnectionFactory
    val queueConnection = connectionFactory.createConnection()
    queueConnection.start()
    val session = queueConnection.createSession()
    val exceptionHandler = CoroutineExceptionHandler { ctx, e ->
        log.error("Exception caught in coroutine {}", StructuredArguments.keyValue("context", ctx), e)
    }

    afterGroup {
        activeMQServer.stop()
    }
    val config = ApplicationConfig(0, 0, "", 0, "", "", "arenakø", "")

    val inputQueue = session.createQueue(config.arenaQueue)
    val producer = session.createProducer(inputQueue)

    afterGroup {
        applicationState.running = false
    }
    describe("Calling selftest with successful liveness and readyness tests") {
        with(TestApplicationEngine()) {
            start()

            // TODO
            application.initRouting(applicationState, producer)

            it("Returns ok on is_alive") {
                with(handleRequest(HttpMethod.Get, "/is_alive")) {
                    response.status() shouldEqual HttpStatusCode.OK
                    response.content shouldNotEqual null
                }
            }

            it("Returns ok on is_ready") {
                with(handleRequest(HttpMethod.Get, "/is_ready")) {
                    response.status() shouldEqual HttpStatusCode.OK
                    response.content shouldNotEqual null
                }
            }
        }
    }

    describe("Calling selftests with unsucessful liveness test") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerNaisApi(readynessCheck = { true }, livenessCheck = { false })
            }

            it("Returns internal server error when liveness check fails") {
                with(handleRequest(HttpMethod.Get, "/is_alive")) {
                    response.status() shouldEqual HttpStatusCode.InternalServerError
                    response.content shouldNotEqual null
                }
            }
        }
    }

    describe("Calling selftests with unsucessful readyness test") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerNaisApi(readynessCheck = { false }, livenessCheck = { true })
            }

            it("Returns internal server error when readyness check fails") {
                with(handleRequest(HttpMethod.Get, "/is_ready")) {
                    response.status() shouldEqual HttpStatusCode.InternalServerError
                    response.content shouldNotEqual null
                }
            }
        }
    }
})
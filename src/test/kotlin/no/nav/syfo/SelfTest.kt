package no.nav.syfo

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.ktor.utils.io.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.registerNaisApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class SelfTest {

    @InternalAPI
    @Test
    internal fun `Returns ok on is_alive`() {
        testApplication {
            application {
                val applicationState = ApplicationState()
                applicationState.ready = true
                applicationState.alive = true
                routing { registerNaisApi(applicationState) }
            }
            val response = client.get("/internal/is_alive")

            Assertions.assertEquals(HttpStatusCode.OK, response.status)
            Assertions.assertEquals("I'm alive! :)", response.bodyAsText())
        }
    }

    @InternalAPI
    @Test
    internal fun `Returns ok in is_ready`() {
        testApplication {
            application {
                val applicationState = ApplicationState()
                applicationState.ready = true
                applicationState.alive = true
                routing { registerNaisApi(applicationState) }
            }
            val response = client.get("/internal/is_ready")

            Assertions.assertEquals(HttpStatusCode.OK, response.status)
            Assertions.assertEquals("I'm ready! :)", response.bodyAsText())
        }
    }

    @InternalAPI
    @Test
    internal fun `Returns internal server error when liveness check fails`() {
        testApplication {
            application {
                val applicationState = ApplicationState()
                applicationState.ready = false
                applicationState.alive = false
                routing { registerNaisApi(applicationState) }
            }

            val response = client.get("/internal/is_alive")

            Assertions.assertEquals(HttpStatusCode.InternalServerError, response.status)
            Assertions.assertEquals("I'm dead x_x", response.bodyAsText())
        }
    }

    @InternalAPI
    @Test
    internal fun `Returns internal server error when readyness check fails`() {
        testApplication {
            application {
                val applicationState = ApplicationState()
                applicationState.ready = false
                applicationState.alive = false
                routing { registerNaisApi(applicationState) }
            }
            val response = client.get("/internal/is_ready")

            Assertions.assertEquals(HttpStatusCode.InternalServerError, response.status)
            Assertions.assertEquals("Please wait! I'm not ready :(", response.bodyAsText())
        }
    }
}

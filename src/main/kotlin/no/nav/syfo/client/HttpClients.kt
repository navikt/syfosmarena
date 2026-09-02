package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.engine.apache5.Apache5EngineConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.serialization.jackson3.jackson
import no.nav.syfo.Environment
import no.nav.syfo.log

class HttpClients(environment: Environment) {
    private val config: HttpClientConfig<Apache5EngineConfig>.() -> Unit = {
        install(ContentNegotiation) { jackson {} }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                when (exception) {
                    is SocketTimeoutException ->
                        throw ServiceUnavailableException(exception.message)
                }
            }
        }
        install(HttpRequestRetry) {
            constantDelay(100, 0, false)
            retryOnExceptionIf(3) { request, throwable ->
                log.warn("Caught exception ${throwable.message}, for url ${request.url}")
                true
            }
            retryIf(maxRetries) { request, response ->
                if (response.status.value.let { it in 500..599 }) {
                    log.warn(
                        "Retrying for statuscode ${response.status.value}, for url ${request.url}"
                    )
                    true
                } else {
                    false
                }
            }
        }
        expectSuccess = false
    }

    private val httpClient = HttpClient(Apache5, config)

    private val accessTokenClientV2 =
        AccessTokenClientV2(
            environment.aadAccessTokenV2Url,
            environment.clientIdV2,
            environment.clientSecretV2,
            httpClient,
        )

    val smtssClient =
        SmtssClient(
            environment.smtssApiUrl,
            accessTokenClientV2,
            environment.smtssApiScope,
            httpClient,
        )
}

class ServiceUnavailableException(message: String?) : Exception(message)

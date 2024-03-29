package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.log
import no.nav.syfo.util.LoggingMeta

class SmtssClient(
    private val endpointUrl: String,
    private val accessTokenClientV2: AccessTokenClientV2,
    private val resourceId: String,
    private val httpClient: HttpClient,
) {
    suspend fun findBestTssIdArena(
        samhandlerFnr: String,
        samhandlerOrgName: String,
        loggingMeta: LoggingMeta,
        sykmeldingId: String,
    ): String? {
        val accessToken = accessTokenClientV2.getAccessTokenV2(resourceId)
        val httpResponse =
            httpClient.get("$endpointUrl/api/v1/samhandler/arena") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer $accessToken")
                header("requestId", sykmeldingId)
                header("samhandlerFnr", samhandlerFnr)
                header("samhandlerOrgName", samhandlerOrgName)
            }
        return getResponse(httpResponse, loggingMeta)
    }

    private suspend fun getResponse(
        httpResponse: io.ktor.client.statement.HttpResponse,
        loggingMeta: LoggingMeta
    ): String? {
        return when (httpResponse.status) {
            HttpStatusCode.OK -> {
                httpResponse.body<TSSident>().tssid
            }
            HttpStatusCode.NotFound -> {
                log.info(
                    "smtss responded with {} for {}",
                    httpResponse.status,
                    StructuredArguments.fields(loggingMeta),
                )
                null
            }
            else -> {
                log.error("Error getting TSS-id ${httpResponse.status}")
                throw RuntimeException("Error getting TSS-id")
            }
        }
    }
}

data class TSSident(
    val tssid: String,
)

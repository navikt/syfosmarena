package no.nav.syfo.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.objectMapper
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smarenaRegler")

fun Routing.registerRuleApi() {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")

        val receivedSykmeldingText = call.receiveText()

        if (log.isDebugEnabled) {
            log.debug(receivedSykmeldingText)
        }
        val receivedSykmelding: ReceivedSykmelding = objectMapper.readValue(receivedSykmeldingText)

        val logValues = arrayOf(
                keyValue("smId", receivedSykmelding.navLogId),
                keyValue("organizationNumber", receivedSykmelding.legekontorOrgNr),
                keyValue("msgId", receivedSykmelding.msgId)
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        log.info("Received a SM2013, going to Arena rules, $logKeys", *logValues)

        val validationAndPeriodRuleResults: List<Rule<Any>> = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                ValidationRuleChain.values().toList()
        ).flatten().executeFlow(receivedSykmelding.sykmelding, RuleMetadata(
                receivedDate = receivedSykmelding.mottattDato,
                signatureDate = receivedSykmelding.signaturDato
        ))

        val results = listOf(validationAndPeriodRuleResults).flatten()

        // TODO do we need to send a respsone, fire and forget???
        call.respond(ValidationResult(
                status = results
                        .map { status -> status.status }
                        .firstOrNull { status -> status == Status.INVALID } ?: Status.OK,
                ruleHits = results.map { rule -> RuleInfo(rule.name) }
        ))
    }
}
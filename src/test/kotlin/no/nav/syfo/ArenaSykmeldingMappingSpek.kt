package no.nav.syfo

import no.nav.syfo.arena.createArenaSykmelding
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object ArenaSykmeldingMappingSpek : Spek({

    describe("Testing createArenaSykmelding") {

        it("Should check rule mapping of Arena sykmelding") {

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusMonths(3).plusDays(1)
                    )
            ))

            val metadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            val receivedSykmelding = ReceivedSykmelding(
                    sykmelding = healthInformation,
                    personNrPasient = "123124",
                    personNrLege = "123145",
                    navLogId = "0412",
                    msgId = "12314-123124-43252-2344",
                    legekontorOrgNr = "",
                    legekontorHerId = "",
                    legekontorReshId = "",
                    legekontorOrgName = "Legevakt",
                    mottattDato = LocalDateTime.now(),
                    signaturDato = LocalDateTime.now(),
                    rulesetVersion = "",
                    fellesformat = ""

            )

            val validationRuleChain = ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, metadata)
            val results = listOf(validationRuleChain).flatten()

            createArenaSykmelding(receivedSykmelding, results).eiaDokumentInfo.dokumentInfo.ediLoggId shouldEqual receivedSykmelding.navLogId
        }
    }
})
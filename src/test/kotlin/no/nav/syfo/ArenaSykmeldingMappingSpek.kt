package no.nav.syfo

import no.nav.syfo.arena.createArenaSykmelding
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
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
                    tlfPasient = "13214",
                    personNrLege = "123145",
                    navLogId = "0412",
                    msgId = "12314-123124-43252-2344",
                    legekontorOrgNr = "",
                    legekontorHerId = "",
                    legekontorReshId = "",
                    legekontorOrgName = "Legevakt",
                    mottattDato = LocalDateTime.now(),
                    rulesetVersion = "",
                    fellesformat = ""

            )

            val validationRuleChain = ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, metadata)
            val results = listOf(validationRuleChain).flatten()

            createArenaSykmelding(receivedSykmelding, results, "12355234").eiaDokumentInfo.dokumentInfo.ediLoggId shouldEqual receivedSykmelding.navLogId
        }

        it("Should check rule mapping of hendelseStatus") {
            val healthInformation = generateSykmelding(utdypendeOpplysninger = mapOf(
                    "6.1" to mapOf(
                            "6.1.1" to SporsmalSvar("Tekst", listOf())
                    )
            ))

            val metadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            val receivedSykmelding = ReceivedSykmelding(
                    sykmelding = healthInformation,
                    personNrPasient = "123124",
                    tlfPasient = "13214",
                    personNrLege = "123145",
                    navLogId = "0412",
                    msgId = "12314-123124-43252-2344",
                    legekontorOrgNr = "",
                    legekontorHerId = "",
                    legekontorReshId = "",
                    legekontorOrgName = "Legevakt",
                    mottattDato = LocalDateTime.now(),
                    rulesetVersion = "",
                    fellesformat = ""

            )

            val validationRuleChain = ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, metadata)
            val results = listOf(validationRuleChain).flatten()

            createArenaSykmelding(receivedSykmelding, results, "12355234").arenaHendelse.hendelse.first().hendelseStatus shouldEqual "UTFORT"
        }
    }
})

package no.nav.syfo

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.model.ReceivedSykmelding
import tools.jackson.module.kotlin.jacksonMapperBuilder

fun main() {
    val sm =
        generateSykmelding(
            perioder =
                listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusMonths(3).plusDays(1),
                    )
                )
        )

    val receivedSykmelding =
        ReceivedSykmelding(
            sykmelding = sm,
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
            fellesformat = "",
            tssid = "",
            merknader = null,
            partnerreferanse = "",
            legeHelsepersonellkategori = null,
            legeHprNr = null,
            vedlegg = null,
            utenlandskSykmelding = null,
        )

    println(jacksonMapperBuilder().build().writeValueAsString(receivedSykmelding))
}

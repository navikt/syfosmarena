package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.model.ReceivedSykmelding
import java.time.LocalDate
import java.time.LocalDateTime

fun main() {

    val sm = generateSykmelding(perioder = listOf(
            generatePeriode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(3).plusDays(1)
            )
    ))

    val receivedSykmelding = ReceivedSykmelding(
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
            fellesformat = ""

    )

    println(ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule()).apply {
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
            .writeValueAsString(receivedSykmelding))
}
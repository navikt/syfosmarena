package no.nav.syfo.arena

import no.nav.helse.arenaSykemelding.ArenaSykmelding
import no.nav.helse.arenaSykemelding.EiaDokumentInfoType
import no.nav.helse.arenaSykemelding.HendelseType
import no.nav.helse.arenaSykemelding.LegeType
import no.nav.helse.arenaSykemelding.MerknadType
import no.nav.helse.arenaSykemelding.PasientDataType
import no.nav.helse.arenaSykemelding.PersonType
import no.nav.syfo.Rule
import no.nav.syfo.model.ReceivedSykmelding
import java.time.LocalDate
import java.time.LocalDateTime

fun createArenaSykmelding(receivedSykmelding: ReceivedSykmelding, ruleResults: List<Rule<Any>>): ArenaSykmelding = ArenaSykmelding().apply {
    eiaDokumentInfo = EiaDokumentInfoType().apply {
        dokumentInfo = no.nav.helse.arenaSykemelding.DokumentInfoType().apply {
            dokumentType = "SM2"
            dokumentTypeVersjon = "1"
            dokumentreferanse = receivedSykmelding.msgId
            ediLoggId = receivedSykmelding.navLogId
            journalReferanse = "" // TODO find out what journalReferanse should be
            dokumentDato = LocalDateTime.now()
        }
        behandlingInfo = EiaDokumentInfoType.BehandlingInfo().apply {
            ruleResults.onEach {
                merknad.add(it.toMerknad())
            }
        }
        avsender = EiaDokumentInfoType.Avsender().apply {
            LegeType().apply {
                legeFnr = receivedSykmelding.personNrLege
            }
        }
        avsenderSystem = EiaDokumentInfoType.AvsenderSystem().apply {
            systemNavn = "syfosmarena"
            systemVersjon = "1.0.0"
        }
    }
    ArenaSykmelding.ArenaHendelse().apply {
        ruleResults.onEach {
            hendelse.add(it.toHendelse())
        }
    }
    pasientData = PasientDataType().apply {
        person = PersonType().apply {
            personFnr = receivedSykmelding.personNrPasient
        }
    }
    foersteFravaersdag = receivedSykmelding.sykmelding.kontaktMedPasient.kontaktDato
    identDato = LocalDate.now()
}

fun Rule<Any>.toMerknad() = MerknadType().apply {
    merknadNr = ruleId.toString()
    merknadType = "2"
    merknadBeskrivelse = name
}

fun Rule<Any>.toHendelse() = HendelseType().apply {
    hendelsesTypeKode = arenaHendelseType.type
    meldingFraLege = meldingFraLege
    hendelseStatus = arenaHendelseStatus.type
    hendelseTekst = hendelseTekst
}
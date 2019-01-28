package no.nav.syfo.arena

import no.nav.helse.arenaSykemelding.ArenaSykmelding
import no.nav.helse.arenaSykemelding.EiaDokumentInfoType
import no.nav.helse.arenaSykemelding.HendelseType
import no.nav.helse.arenaSykemelding.LegeType
import no.nav.helse.arenaSykemelding.MerknadType
import no.nav.helse.arenaSykemelding.PasientDataType
import no.nav.helse.arenaSykemelding.PersonType
import no.nav.syfo.model.ReceivedSykmelding
import java.time.LocalDate
import java.time.LocalDateTime

fun createArenaSykmelding(receivedSykmelding: ReceivedSykmelding): ArenaSykmelding = ArenaSykmelding().apply {
    EiaDokumentInfoType().apply {
        no.nav.helse.arenaSykemelding.DokumentInfoType().apply {
            dokumentType = "SM2"
            dokumentTypeVersjon = "1"
            dokumentreferanse = receivedSykmelding.msgId
            ediLoggId = receivedSykmelding.navLogId
            // TODO find out what journalReferanse should be
            journalReferanse = ""
            dokumentDato = LocalDateTime.now()
        }
        EiaDokumentInfoType.BehandlingInfo().apply {
            // TODO map rule result here
            merknad.add(MerknadType().apply {
                merknadNr = "1209"
                merknadNr = "1" // TODO denne skal være på alle merknadene
                merknadBeskrivelse = "Sykmeldingen er fremdatert: Startdato ligger mer enn 30 dager etter første konsultasjon."
            })
        }
        EiaDokumentInfoType.Avsender().apply {
            LegeType().apply {
                legeFnr = receivedSykmelding.personNrLege
            }
        }
        EiaDokumentInfoType.AvsenderSystem().apply {
            systemNavn = "syfosmarena"
            systemVersjon = "1.0.0"
        }
    }
    ArenaSykmelding.ArenaHendelse().apply {
        HendelseType().apply {
            hendelsesTypeKode = "MESM_I_SM"
            meldingFraLege = "" // TODO here we should sendt the healthInformation field for that rule
            hendelseStatus = "PLANLAGT"
            hendelseTekst = "Informasjon fra behandler til NAV. Åpne dokumentet for å se behandlers innspill til NAV."
        }
    }
    PasientDataType().apply {
        PersonType().apply {
            personFnr = receivedSykmelding.personNrPasient
        }
    }
    foersteFravaersdag = LocalDate.now()
    identDato = LocalDate.now()
}
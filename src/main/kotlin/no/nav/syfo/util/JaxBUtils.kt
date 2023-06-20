package no.nav.syfo.util

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import no.nav.helse.arena.sykemelding.ArenaSykmelding
import no.nav.helse.arena.sykemelding.EiaDokumentInfoType

val arenaSykmeldingJaxBContext: JAXBContext =
    JAXBContext.newInstance(ArenaSykmelding::class.java, EiaDokumentInfoType::class.java)

val arenaSykmeldingMarshaller: Marshaller = arenaSykmeldingJaxBContext.createMarshaller()

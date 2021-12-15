package no.nav.syfo.util

import no.nav.helse.arenaSykemelding.ArenaSykmelding
import no.nav.helse.arenaSykemelding.EiaDokumentInfoType
import no.nav.helse.infotrygd.foresp.InfotrygdForesp
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller

val arenaSykmeldingJaxBContext: JAXBContext = JAXBContext.newInstance(ArenaSykmelding::class.java, EiaDokumentInfoType::class.java)

val arenaSykmeldingMarshaller: Marshaller = arenaSykmeldingJaxBContext.createMarshaller()

val infotrygdSporringJaxBContext: JAXBContext = JAXBContext.newInstance(InfotrygdForesp::class.java)
val infotrygdSporringMarshaller: Marshaller = infotrygdSporringJaxBContext.createMarshaller()

val infotrygdSporringUnmarshaller: Unmarshaller = infotrygdSporringJaxBContext.createUnmarshaller()

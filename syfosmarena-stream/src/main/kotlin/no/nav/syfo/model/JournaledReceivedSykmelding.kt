package no.nav.syfo.model

data class JournaledReceivedSykmelding(
    val receivedSykmelding: ByteArray,
    val journalpostId: String
)

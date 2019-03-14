package no.nav.syfo

import java.nio.file.Path
import java.nio.file.Paths


data class ApplicationConfig(
    val applicationPort: Int,
    val applicationThreads: Int,
    val mqHost: String,
    val mqPort: Int,
    val mqQueueManager: String,
    val mqChannel: String,
    val arenaQueue: String,
    val kafkaSm2013AutomaticPapirmottakTopic: String,
    val kafkaSm2013manuellPapirmottakTopic: String,
    val kafkaSm2013AutomaticDigitalHandlingTopic: String,
    val kafkaSm2013manuelDigitalManuellTopic: String,
    val kafkasm2013oppgaveJournalOpprettetTopic: String,
    val kafkasm2013ArenaInput: String,
    val kafkaBootstrapServers: String,
    val applicationName: String,
    val vaultApplicationPropertiesPath: Path = Paths.get("/var/run/secrets/nais.io/vault/credentials.json")
)
data class VaultCredentials(
    val serviceuserUsername: String,
    val serviceuserPassword: String,
    val mqUsername: String,
    val mqPassword: String
)

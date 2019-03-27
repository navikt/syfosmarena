package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "4").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "syfosmarena"),
    val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val kafkaSm2013AutomaticPapirmottakTopic: String = getEnvVar("KAFKA_SMPAPIR_AUTOMATIC_TOPIC", "privat-syfo-smpapir-automatiskBehandling"),
    val kafkaSm2013AutomaticDigitalHandlingTopic: String = getEnvVar("KAFKA_SM2013_AUTOMATIC_TOPIC", "privat-syfo-sm2013-automatiskBehandling"),
    val kafkasm2013ManualHandlingTopic: String = getEnvVar("KAFKA_SM2013_MANUAL_TOPIC", "privat-syfo-sm2013-manuellBehandling"),
    val kafkasm2013ManualHandlingPapirTopic: String = getEnvVar("KAFKA_SMPAPIR_MANUAL_TOPIC", "privat-syfo-smpapir-manuellBehandling"),
    val kafkasm2013oppgaveJournalOpprettetTopic: String = getEnvVar("KAFKA_OPPGAVE_JOURNAL_OPPRETTET_TOPIC", "aapen-syfo-oppgave-journalOpprettet"),
    val kafkasm2013ArenaInput: String = getEnvVar("KAFKA_ARENA_INPUT_TOPIC", "privat-syfo-arena-input"),
    val mqHostname: String = getEnvVar("MQ_HOST_NAME"),
    val mqPort: Int = getEnvVar("MQ_PORT").toInt(),
    val mqGatewayName: String = getEnvVar("MQ_GATEWAY_NAME"),
    val mqChannelName: String = getEnvVar("MQ_CHANNEL_NAME"),
    val arenaQueue: String = getEnvVar("MQ_ARENA_QUEUE")
)

data class VaultCredentials(
    val serviceuserUsername: String,
    val serviceuserPassword: String,
    val mqUsername: String,
    val mqPassword: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

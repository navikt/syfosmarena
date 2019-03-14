package no.nav.syfo

import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.syfo.sak.avro.RegisterJournal
import no.nav.syfo.util.envOverrides
import no.nav.syfo.util.loadBaseConfig
import no.nav.syfo.util.toProducerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.time.LocalDate
import java.util.UUID

fun main() {
    val mottakCredentials = VaultCredentials("srvsyfosmmottak", "changeit", "", "")
    val sakCredentials = VaultCredentials("srvsyfosmsak", "changeit", "", "")
    val applicationConfig = objectMapper.readValue<ApplicationConfig>(File("config-local.json"))

    val mottakConfig = loadBaseConfig(applicationConfig, mottakCredentials)
            .envOverrides()
            .toProducerConfig("produce-message", StringSerializer::class)
            .apply {
                this["schema.registry.url"] = "http://localhost:8081"
            }
    val sakConfig = loadBaseConfig(applicationConfig, sakCredentials)
            .envOverrides()
            .toProducerConfig("produce-message", KafkaAvroSerializer::class)
            .apply {
                this["schema.registry.url"] = "http://localhost:8081"
            }

    val mottakProducer = KafkaProducer<String, String>(mottakConfig)
    val sakProducer = KafkaProducer<String, RegisterJournal>(sakConfig)

    val id = UUID.randomUUID().toString()

    // val sykmelding = generateSykmelding()
    val sykmelding = generateSykmelding(perioder = listOf(
            generatePeriode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(3).plusDays(1)
            )
    ))

    val receivedSykmelding = receivedSykmelding(id, sykmelding)
    val jsonReceivedSykmelding = objectMapper.writeValueAsString(receivedSykmelding)

    mottakProducer.send(ProducerRecord(applicationConfig.kafkaSm2013AutomaticDigitalHandlingTopic, id, jsonReceivedSykmelding))
    sakProducer.send(ProducerRecord(applicationConfig.kafkasm2013oppgaveJournalOpprettetTopic, id, RegisterJournal().apply {
        this.journalpostId = "hello"
        this.journalpostKilde = "wow"
        this.sakId = "123"
        this.messageId = "432"
    }))

    Thread.sleep(1000)
}

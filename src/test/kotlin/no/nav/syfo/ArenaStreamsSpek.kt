package no.nav.syfo

import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import java.time.Duration
import java.time.LocalDateTime
import java.util.Properties
import no.nav.common.KafkaEnvironment
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.kafka.toStreamsConfig
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.sak.avro.RegisterJournal
import org.amshove.kluent.shouldEqual
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ArenaStreamsSpek : Spek({
    val sm2013Auto = "it-sm2013-auto"
    val sm2013Manual = "it-sm2013-manual"
    val journalCreated = "it-sm2013-journalCreated"
    val arenaInputTopic = "it-sm2013-arenaInput"
    val kafkaEnvironment = KafkaEnvironment(
            withSchemaRegistry = true,
            topicNames = listOf(sm2013Auto, sm2013Manual, journalCreated, arenaInputTopic)
    )

    val env = Environment(
            applicationPort = 8080,
            mqPort = -1,
            mqHostname = "",
            mqGatewayName = "",
            mqChannelName = "",
            arenaQueue = "",
            kafkaSm2013AutomaticDigitalHandlingTopic = sm2013Auto,
            kafkasm2013ManualHandlingTopic = sm2013Manual,
            kafkasm2013oppgaveJournalOpprettetTopic = journalCreated,
            kafkasm2013ArenaInput = arenaInputTopic,
            kafkaBootstrapServers = kafkaEnvironment.brokersURL,
            applicationName = "spek-it",
            truststore = "truststore",
            truststorePassword = "pwd",
            cluster = "cluster"
    )
    val vaultCredentials = VaultCredentials(
            "unused",
            "unused",
            "unused",
            "unused"
    )

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
        put("schema.registry.url", kafkaEnvironment.schemaRegistry!!.url)
        put(StreamsConfig.STATE_DIR_CONFIG, kafkaStreamsStateDir.toAbsolutePath().toString())
    }

    val baseProperties = loadBaseConfig(env, vaultCredentials)
    val streamsProperties = baseProperties
            .toStreamsConfig(env.applicationName, valueSerde = SpecificAvroSerde::class)
            .overrideForTest()

    val streamsApplication = createKafkaStream(streamsProperties, env)

    beforeGroup {
        cleanupDir(kafkaStreamsStateDir, env.applicationName)
        kafkaEnvironment.start()

        streamsApplication.start()
    }

    afterGroup {
        kafkaEnvironment.tearDown()
        deleteDir(kafkaStreamsStateDir)
    }

    describe("Kafka streams") {
        val joarkProducer = KafkaProducer<String, RegisterJournal>(baseProperties
                .toProducerConfig("spek-it-producer", KafkaAvroSerializer::class)
                .overrideForTest())

        val smProducer = KafkaProducer<String, String>(baseProperties
                .toProducerConfig("spek-it-producer", StringSerializer::class)
                .overrideForTest())

        val outputConsumer = KafkaConsumer<String, String>(baseProperties
                .toConsumerConfig("spek-it-consumer", StringDeserializer::class)
                .overrideForTest())

        outputConsumer.subscribe(listOf(env.kafkasm2013ArenaInput))

        val sykmelding = generateSykmelding()
        val receivedSykmelding = ReceivedSykmelding(
                sykmelding = sykmelding,
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
                merknader = null
        )

        it("Streams should join together the two topics") {
            smProducer.send(ProducerRecord(env.kafkaSm2013AutomaticDigitalHandlingTopic, sykmelding.id, objectMapper.writeValueAsString(receivedSykmelding)))
            joarkProducer.send(ProducerRecord(env.kafkasm2013oppgaveJournalOpprettetTopic, sykmelding.id, RegisterJournal().apply {
                this.journalpostId = "hello"
                this.journalpostKilde = "wow"
                this.sakId = "123"
                this.messageId = "432"
            }))

            val joined = outputConsumer.poll(Duration.ofMillis(10000)).toList()

            joined.size shouldEqual 1
            val journaledSykmelding = objectMapper.readValue<JournaledReceivedSykmelding>(joined.first().value())

            journaledSykmelding.journalpostId shouldEqual "hello"
            journaledSykmelding.receivedSykmelding shouldEqual objectMapper.writeValueAsBytes(receivedSykmelding)
        }
    }
})

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val artemisVersion = "2.17.0"
val coroutinesVersion = "1.6.0"
val fellesformatVersion = "2019.07.30-12-26-5c924ef4f04022bbb850aaf299eb8e4464c1ca6a"
val ibmMqVersion = "9.2.4.0"
val javaxActivationVersion = "1.1.1"
val jacksonVersion = "2.13.2"
val jaxbApiVersion = "2.4.0-b180830.0359"
val jaxbVersion = "2.3.0.1"
val kafkaVersion = "2.8.0"
val kluentVersion = "1.68"
val ktorVersion = "1.6.8"
val logbackVersion = "1.2.11"
val logstashEncoderVersion = "7.0.1"
val prometheusVersion = "0.15.0"
val smCommonVersion = "1.18fb664"
val spekVersion = "2.0.17"
val jaxwsApiVersion = "2.3.1"
val jaxbBasicAntVersion = "1.11.1"
val javaxAnnotationApiVersion = "1.3.2"
val jaxwsToolsVersion = "2.3.1"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val arenaSykemdlingVersion = "2019.09.09-08-50-693492ddc1d3f98e70c1638c94dcb95a66036d12"
val infotrygdForespVersion = "2019.07.29-02-53-86b22e73f7843e422ee500b486dac387a582f2d1"
val sykmeldingVersion = "2019.07.29-02-53-86b22e73f7843e422ee500b486dac387a582f2d1"
val jaxbTimeAdaptersVersion = "1.1.3"
val kithHodemeldingVersion = "2019.07.30-12-26-5c924ef4f04022bbb850aaf299eb8e4464c1ca6a"
val kontrollsystemblokk = "2019.07.29-02-53-86b22e73f7843e422ee500b486dac387a582f2d1"
val kotlinVersion = "1.6.0"


plugins {
    java
    kotlin("jvm") version "1.6.0"
    id("org.jmailen.kotlinter") version "3.6.0"
    id("com.diffplug.spotless") version "5.16.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

buildscript {
    dependencies {
        classpath("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        classpath("org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
        classpath("com.sun.activation:javax.activation:1.2.0")
    }
}

val githubUser: String by project
val githubPassword: String by project

allprojects {
    group = "no.nav.syfo"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven (url= "https://packages.confluent.io/maven/")
        maven {
            url = uri("https://maven.pkg.github.com/navikt/syfosm-common")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
    }
}
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation ("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

        implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
        implementation ("io.prometheus:simpleclient_hotspot:$prometheusVersion")
        implementation ("io.prometheus:simpleclient_common:$prometheusVersion")

        implementation ("io.ktor:ktor-server-netty:$ktorVersion")
        implementation ("io.ktor:ktor-client-apache:$ktorVersion")
        implementation ("io.ktor:ktor-client-auth-basic:$ktorVersion")
        implementation ("io.ktor:ktor-client-jackson:$ktorVersion")
        implementation ("io.ktor:ktor-jackson:$ktorVersion")

        implementation ("ch.qos.logback:logback-classic:$logbackVersion")
        implementation ("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

        implementation ("com.ibm.mq:com.ibm.mq.allclient:$ibmMqVersion")

        implementation ("org.apache.kafka:kafka_2.12:$kafkaVersion")
        implementation ("org.apache.kafka:kafka-streams:$kafkaVersion")

        implementation ("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
        implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        implementation ("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
        implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

        implementation ("no.nav.helse:syfosm-common-models:$smCommonVersion")
        implementation ("no.nav.helse:syfosm-common-kafka:$smCommonVersion")
        implementation ("no.nav.helse:syfosm-common-mq:$smCommonVersion")
        implementation ("no.nav.helse:syfosm-common-diagnosis-codes:$smCommonVersion")

        implementation ("no.nav.helse.xml:arenaSykmelding-1:$arenaSykemdlingVersion")
        implementation ("no.nav.helse.xml:infotrygd-foresp:$infotrygdForespVersion")
        implementation ("no.nav.helse.xml:sm2013:$sykmeldingVersion")
        implementation ("no.nav.helse.xml:xmlfellesformat:$fellesformatVersion")
        implementation ("no.nav.helse.xml:kith-hodemelding:$kithHodemeldingVersion")
        implementation ("no.nav.helse.xml:kontrollsystemblokk:$kontrollsystemblokk")

        implementation ("com.migesok:jaxb-java-time-adapters:$jaxbTimeAdaptersVersion")

        implementation ("javax.xml.bind:jaxb-api:$jaxbApiVersion")
        implementation ("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
        implementation ("javax.activation:activation:$javaxActivationVersion")

        testImplementation ("org.amshove.kluent:kluent:$kluentVersion")
        testImplementation ("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
        testImplementation ("io.ktor:ktor-server-test-host:$ktorVersion") {
            exclude(group = "org.eclipse.jetty") // conflicts with WireMock
        }
        testImplementation ("org.apache.activemq:artemis-server:$artemisVersion")
        testImplementation ("org.apache.activemq:artemis-jms-client:$artemisVersion")

        testImplementation ("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
            exclude(group = "org.jetbrains.kotlin")
        }
        testRuntimeOnly ("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
            exclude(group = "org.jetbrains.kotlin")
        }
    }

    tasks {
        withType<Jar> {
            manifest.attributes["Main-Class"] = "no.nav.syfo.BootstrapKt"
        }

        create("printVersion") {
            println(project.version)
        }

        withType<ShadowJar> {
            transform(ServiceFileTransformer::class.java) {
                setPath("META-INF/cxf")
                include("bus-extensions.txt")
            }
        }

        withType<Test> {
            useJUnitPlatform {
                includeEngines("spek2")
            }
            testLogging.showStandardStreams = true
        }

        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
        }

        "check" {
            dependsOn("formatKotlin")
        }
    }
}

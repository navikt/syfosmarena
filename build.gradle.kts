import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.7.1"
val syfoXmlCodeGenVersion = "1.0.4"
val ibmMqVersion = "9.3.2.1"
val javaxActivationVersion = "1.1.1"
val jacksonVersion = "2.15.2"
val jaxbApiVersion = "2.4.0-b180830.0359"
val jaxbVersion = "2.3.0.1"
val kafkaVersion = "3.4.1"
val ktorVersion = "2.3.1"
val logbackVersion = "1.4.7"
val logstashEncoderVersion = "7.3"
val prometheusVersion = "0.16.0"
val smCommonVersion = "1.0.1"
val jaxwsApiVersion = "2.3.1"
val jaxbBasicAntVersion = "1.11.1"
val javaxAnnotationApiVersion = "1.3.2"
val jaxwsToolsVersion = "2.3.1"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val jaxbTimeAdaptersVersion = "1.1.3"
val kotlinVersion = "1.8.22"
val junitJupiterVersion = "5.9.3"



plugins {
    java
    kotlin("jvm") version "1.8.21"
    id("org.jmailen.kotlinter") version "3.15.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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


    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/syfosm-common")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
    }


    dependencies {
        implementation ("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

        implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
        implementation ("io.prometheus:simpleclient_hotspot:$prometheusVersion")
        implementation ("io.prometheus:simpleclient_common:$prometheusVersion")

        implementation ("io.ktor:ktor-server-core:$ktorVersion")
        implementation ("io.ktor:ktor-server-netty:$ktorVersion")
        implementation ("io.ktor:ktor-server-content-negotiation:$ktorVersion")
        implementation ("io.ktor:ktor-server-status-pages:$ktorVersion")
        implementation ("io.ktor:ktor-server-call-id:$ktorVersion")
        implementation ("io.ktor:ktor-serialization-jackson:$ktorVersion")

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

        implementation ("no.nav.helse.xml:arena-sykmelding-1:$syfoXmlCodeGenVersion")
        implementation ("no.nav.helse.xml:sm2013:$syfoXmlCodeGenVersion")
        implementation ("no.nav.helse.xml:xmlfellesformat:$syfoXmlCodeGenVersion")
        implementation ("no.nav.helse.xml:kith-hodemelding:$syfoXmlCodeGenVersion")
        implementation ("no.nav.helse.xml:kontrollsystemblokk:$syfoXmlCodeGenVersion")

        implementation ("com.migesok:jaxb-java-time-adapters:$jaxbTimeAdaptersVersion")

        implementation ("javax.xml.bind:jaxb-api:$jaxbApiVersion")
        implementation ("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
        implementation ("javax.activation:activation:$javaxActivationVersion")

        testImplementation ("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation ("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testImplementation ("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
        testImplementation ("io.ktor:ktor-server-test-host:$ktorVersion") {
            exclude(group = "org.eclipse.jetty") // conflicts with WireMock
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
            }
            testLogging {
                events("skipped", "failed")
                showStackTraces = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }

        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
        }

        "check" {
            dependsOn("formatKotlin")
        }
    }
}

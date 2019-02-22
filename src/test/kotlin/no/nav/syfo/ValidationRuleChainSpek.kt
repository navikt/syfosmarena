package no.nav.syfo

import no.nav.syfo.model.MeldingTilNAV
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object ValidationRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule SICK_LAVE_END_DATE_MORE_THAN_3_MONTHS, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusMonths(3).plusDays(1)
                    )
            ))

            val metadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.SICK_LAVE_END_DATE_MORE_THAN_3_MONTHS(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule SICK_LAVE_END_DATE_MORE_THAN_3_MONTHS, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, ruleMetadata: RuleMetadata) =
                    RuleData(healthInformation, ruleMetadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusMonths(3)
                    )
            ))

            val ruleMetadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.SICK_LAVE_END_DATE_MORE_THAN_3_MONTHS(ruleData(healthInformation, ruleMetadata)) shouldEqual false
        }

        it("Should check rule SICK_LAVE_PERIODE_MORE_THEN_3_MONTHS, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(92)
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.SICK_LAVE_PERIODE_MORE_THEN_3_MONTHS(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule SICK_LAVE_PERIODE_MORE_THEN_3_MONTHS, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(91)
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.SICK_LAVE_PERIODE_MORE_THEN_3_MONTHS(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule TRAVEL_SUBSIDY_SPECIFIED, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            reisetilskudd = true
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.TRAVEL_SUBSIDY_SPECIFIED(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule TRAVEL_SUBSIDY_SPECIFIED, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            reisetilskudd = false
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.TRAVEL_SUBSIDY_SPECIFIED(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule MESSAGE_TO_EMPLOYER, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(meldingTilArbeidsgiver = "Han trenger nav ytelser")

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.MESSAGE_TO_EMPLOYER(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule MESSAGE_TO_EMPLOYER, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding()

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.MESSAGE_TO_EMPLOYER(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(57),
                            reisetilskudd = false
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(56),
                            reisetilskudd = false
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(50),
                            reisetilskudd = false
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "2")

            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(49),
                            reisetilskudd = false
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "2")

            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule MESSAGE_TO_NAV_ASSISTANCE_IMMEDIATLY, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(meldingTilNAV =
            MeldingTilNAV(bistandUmiddelbart = true, beskrivBistand = ""))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "2")

            ValidationRuleChain.MESSAGE_TO_NAV_ASSISTANCE_IMMEDIATLY(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule MESSAGE_TO_NAV_ASSISTANCE_IMMEDIATLY, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(meldingTilNAV =
            MeldingTilNAV(bistandUmiddelbart = false, beskrivBistand = ""))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "2")

            ValidationRuleChain.MESSAGE_TO_NAV_ASSISTANCE_IMMEDIATLY(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule DYNAMIC_QUESTIONS, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(utdypendeOpplysninger = mapOf(
                    "6.1" to mapOf(
                            "6.1.1" to SporsmalSvar("Tekst", listOf())
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.DYNAMIC_QUESTIONS(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule DYNAMIC_QUESTIONS, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(utdypendeOpplysninger = mapOf())

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.DYNAMIC_QUESTIONS(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule MEASURES_OTHER_OR_NAV, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(andreTiltak = "han trenget tiltak")

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.MEASURES_OTHER_OR_NAV(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule MEASURES_OTHER_OR_NAV, should NOT trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding()

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.MEASURES_OTHER_OR_NAV(ruleData(healthInformation, metadata)) shouldEqual false
        }

        it("Should check rule DYNAMIC_QUESTIONS_AAP, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(utdypendeOpplysninger = mapOf(
                    "6.6" to mapOf(
                            "6.6.1" to SporsmalSvar("Tekst", listOf())
                    )
            ))

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.DYNAMIC_QUESTIONS_AAP(ruleData(healthInformation, metadata)) shouldEqual true
        }

        it("Should check rule DYNAMIC_QUESTIONS_AAP, should trigger rule") {
            fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
                    RuleData(healthInformation, metadata)

            val healthInformation = generateSykmelding(utdypendeOpplysninger = mapOf())

            val metadata = RuleMetadata(signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    rulesetVersion = "1")

            ValidationRuleChain.DYNAMIC_QUESTIONS_AAP(ruleData(healthInformation, metadata)) shouldEqual false
        }
    }
})

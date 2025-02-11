package no.nav.syfo

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleData
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ValidationRuleChainTest {
    @Test
    internal fun `Should check rule TRAVEL_SUBSIDY_SPECIFIED, should trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                perioder =
                    listOf(
                        generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            reisetilskudd = true,
                        ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )
        assertEquals(
            true,
            ValidationRuleChain.TRAVEL_SUBSIDY_SPECIFIED(ruleData(healthInformation, metadata)),
        )
    }

    @Test
    internal fun `Should check rule TRAVEL_SUBSIDY_SPECIFIED, should NOT trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                perioder =
                    listOf(
                        generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            reisetilskudd = false,
                        ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )

        assertEquals(
            false,
            ValidationRuleChain.TRAVEL_SUBSIDY_SPECIFIED(ruleData(healthInformation, metadata)),
        )
    }

    @Test
    internal fun `Should check rule MESSAGE_TO_EMPLOYER, should trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(meldingTilArbeidsgiver = "Han trenger nav ytelser")

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )

        assertEquals(
            true,
            ValidationRuleChain.MESSAGE_TO_EMPLOYER(ruleData(healthInformation, metadata)),
        )
    }

    @Test
    internal fun `Should check rule MESSAGE_TO_EMPLOYER, should NOT trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation = generateSykmelding()

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )

        assertEquals(
            false,
            ValidationRuleChain.MESSAGE_TO_EMPLOYER(ruleData(healthInformation, metadata)),
        )
    }

    @Test
    internal fun `Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2, should trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                perioder =
                    listOf(
                        generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(57),
                            reisetilskudd = false,
                        ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )

        assertEquals(
            true,
            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2(
                ruleData(
                    healthInformation,
                    metadata,
                ),
            ),
        )
    }

    @Test
    internal fun `Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2, should NOT trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                perioder =
                    listOf(
                        generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(56),
                            reisetilskudd = false,
                        ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )

        assertEquals(
            false,
            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2(
                ruleData(
                    healthInformation,
                    metadata,
                ),
            ),
        )
    }

    @Test
    internal fun `Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2, should trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                perioder =
                    listOf(
                        generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(50),
                            reisetilskudd = false,
                        ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "2",
            )

        assertEquals(
            true,
            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2(
                ruleData(
                    healthInformation,
                    metadata,
                ),
            ),
        )
    }

    @Test
    internal fun `Should check rule PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2, should NOT trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                perioder =
                    listOf(
                        generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(49),
                            reisetilskudd = false,
                        ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "2",
            )

        assertEquals(
            false,
            ValidationRuleChain.PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2(
                ruleData(
                    healthInformation,
                    metadata,
                ),
            ),
        )
    }

    @Test
    internal fun `Should check rule DYNAMIC_QUESTIONS, should trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                utdypendeOpplysninger =
                    mapOf(
                        "6.1" to
                            mapOf(
                                "6.1.1" to SporsmalSvar("Pasient syk?", "Tekst", listOf()),
                            ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )

        assertEquals(
            true,
            ValidationRuleChain.DYNAMIC_QUESTIONS(ruleData(healthInformation, metadata)),
        )
    }

    @Test
    internal fun `Should check rule DYNAMIC_QUESTIONS, should NOT trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation = generateSykmelding(utdypendeOpplysninger = mapOf())

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )
        assertEquals(
            false,
            ValidationRuleChain.DYNAMIC_QUESTIONS(ruleData(healthInformation, metadata)),
        )
    }

    @Test
    internal fun `Should check rule DYNAMIC_QUESTIONS_AAP, should trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation =
            generateSykmelding(
                utdypendeOpplysninger =
                    mapOf(
                        "6.6" to
                            mapOf(
                                "6.6.1" to SporsmalSvar("Pasient syk?", "Tekst", listOf()),
                            ),
                    ),
            )

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )
        assertEquals(
            true,
            ValidationRuleChain.DYNAMIC_QUESTIONS_AAP(ruleData(healthInformation, metadata)),
        )
    }

    @Test
    internal fun `Should check rule DYNAMIC_QUESTIONS_AAP, should NOT trigger rule`() {
        fun ruleData(healthInformation: Sykmelding, metadata: RuleMetadata) =
            RuleData(healthInformation, metadata)

        val healthInformation = generateSykmelding(utdypendeOpplysninger = mapOf())

        val metadata =
            RuleMetadata(
                signatureDate = LocalDateTime.now(),
                receivedDate = LocalDateTime.now(),
                rulesetVersion = "1",
            )
        assertEquals(
            false,
            ValidationRuleChain.DYNAMIC_QUESTIONS_AAP(ruleData(healthInformation, metadata)),
        )
    }
}

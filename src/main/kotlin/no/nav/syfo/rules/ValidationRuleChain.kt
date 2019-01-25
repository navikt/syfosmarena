package no.nav.syfo.rules

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.ArenaHendelseStatus
import no.nav.syfo.ArenaHendelseType
import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class RuleMetadata(
    val signatureDate: LocalDateTime,
    val receivedDate: LocalDateTime
)

enum class ValidationRuleChain(override val ruleId: Int?, override val arenaHendelseType: ArenaHendelseType, override val arenaHendelseStatus: ArenaHendelseStatus, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    @Description("Hvis sykmeldingens sluttdato er mer enn 3 måneder frem i tid skal meldingen til oppfølging i Arena")
    SICK_LAVE_END_DATE_MORE_THAN_3_MONTHS(1603, ArenaHendelseType.VURDER_OPPFOLGING, ArenaHendelseStatus.PLANLAGT, { (healthInformation, ruleMetadata) ->
        if (!healthInformation.aktivitet.periode.isNullOrEmpty()) {
            healthInformation.aktivitet.periode.sortedTOMDate().last().atStartOfDay() < ruleMetadata.signatureDate.plusMonths(3)
        } else {
            false
        }
    }),

    @Description("Hvis sykmeldingsperioden er over 3 måneder skal meldingen til oppfølging i Arena")
    SICK_LAVE_PERIODE_MORE_THEN_3_MONTHS(1606, ArenaHendelseType.VURDER_OPPFOLGING, ArenaHendelseStatus.PLANLAGT, { (healthInformation, _) ->
        if (!healthInformation.aktivitet.periode.isNullOrEmpty()) {
            healthInformation.aktivitet.periode.any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 91 }
        } else {
            false
        }
    }),

    @Description("Forlengelse ut over maxdato.")
    MAX_SICK_LEAVE_PAYOUT(1607, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.PLANLAGT, { (healthInformation, _) ->
        // infotrygdForesp.sMhistorikk?.sykmelding?.first()?.periode?.stans == "MAX"
        false
    }),

    @Description("Kun reisetilskudd er angitt. Melding sendt til oppfølging i Arena, skal ikke registreres i Infotrygd.")
    TRAVEL_SUBSIDY_SPECIFIED(1608, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.PLANLAGT, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any { it.isReisetilskudd == true } // Can be null, so use equality
    }),

    @Description("Hvis sykmelder har gitt veiledning til arbeidsgiver/arbeidstaker (felt 9.1).")
    MESSAGE_TO_EMPLOYER(1609, ArenaHendelseType.VEILEDNING_TIL_ARBEIDSGIVER, ArenaHendelseStatus.UTFORT, { (healthInformation, _) ->
            !healthInformation.meldingTilArbeidsgiver.isNullOrBlank()
    }),

    @Description("Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.")
    PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2(1615, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT, { (healthInformation, _) ->
        val rulesettversion = healthInformation.regelSettVersjon ?: ""
        val timeGroup8Week = healthInformation.aktivitet.periode
                .any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 56 }

        timeGroup8Week && kotlin.collections.listOf("", "1").contains(rulesettversion)
    }),

    @Description("Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.")
    PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2(1615, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT, { (healthInformation, _) ->
        val rulesettversion2 = healthInformation.regelSettVersjon == "2"
        val timeGroup7Week = healthInformation.aktivitet.periode
                .any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 49 }

        timeGroup7Week && rulesettversion2
    }),

    @Description("Hvis sykmeldingen inneholder melding fra behandler skal meldingen til oppfølging i Arena.")
    MESSAGE_TO_NAV_ASSISTANCE_IMMEDIATLY(1616, ArenaHendelseType.MELDING_FRA_BEHANDLER, ArenaHendelseStatus.PLANLAGT, { (healthInformation, _) ->
        if (healthInformation.meldingTilNav != null) {
            healthInformation.meldingTilNav.isBistandNAVUmiddelbart
        } else {
            false
        }
    }),

    @Description("Hvis utdypende opplysninger om medisinske er oppgitt ved 7/8, 17, 39 uker settes merknad")
    DYNAMIC_QUESTIONS(1617, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT, { (healthInformation, _) ->
        if (healthInformation.utdypendeOpplysninger != null) {
            !healthInformation.utdypendeOpplysninger.spmGruppe.isNullOrEmpty()
        } else {
            false
        }
    }),

    @Description("Hvis sykmeldingen inneholer tiltakNAV eller andreTiltak, så skal merknad lages og hendelse sendes til Arena")
    MEASURES_OTHER_OR_NAV(1618, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.PLANLAGT, { (healthInformation, _) ->
        if (healthInformation.tiltak != null) {
            !healthInformation.tiltak.andreTiltak.isNullOrBlank() || !healthInformation.tiltak.tiltakNAV.isNullOrBlank()
        } else {
            false
        }
    }),

    @Description("Hvis utdypende opplysninger foreligger og pasienten søker om AAP")
    INVALID_FNR(1620, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT, { (healthInformation, _) ->
        if (healthInformation.utdypendeOpplysninger != null) {
            healthInformation.utdypendeOpplysninger.spmGruppe.any {
                it.spmGruppeId == "6.6"
            }
        } else {
            false
        }
    }),
}

fun List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>.sortedTOMDate(): List<LocalDate> =
        map { it.periodeTOMDato }.sorted()

fun ClosedRange<LocalDate>.daysBetween(): Long = ChronoUnit.DAYS.between(start, endInclusive)
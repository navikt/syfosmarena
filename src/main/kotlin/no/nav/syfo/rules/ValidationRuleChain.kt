package no.nav.syfo.rules

import no.nav.syfo.arena.ArenaHendelseStatus
import no.nav.syfo.arena.ArenaHendelseType
import no.nav.syfo.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class RuleMetadata(
    val signatureDate: LocalDateTime,
    val receivedDate: LocalDateTime,
    val rulesetVersion: String?
)

enum class ValidationRuleChain(override val ruleId: Int?, override val arenaHendelseType: ArenaHendelseType, override val arenaHendelseStatus: ArenaHendelseStatus, override val meldingFraLege: String, override val hendelseTekst: String, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    @Description("Hvis sykmeldingens sluttdato er mer enn 3 måneder frem i tid skal meldingen til oppfølging i Arena")
    SICK_LAVE_END_DATE_MORE_THAN_3_MONTHS(1603, ArenaHendelseType.VURDER_OPPFOLGING, ArenaHendelseStatus.PLANLAGT,
            "", "Sykmeldings sluttdato er mer enn 3 måneder frem i tid.", { (sykmelding, ruleMetadata) ->
        sykmelding.perioder.sortedTOMDate().last().atStartOfDay() > ruleMetadata.signatureDate.plusMonths(3)
    }),

    @Description("Hvis sykmeldingsperioden er over 3 måneder skal meldingen til oppfølging i Arena")
    SICK_LAVE_PERIODE_MORE_THEN_3_MONTHS(1606, ArenaHendelseType.VURDER_OPPFOLGING, ArenaHendelseStatus.PLANLAGT,
            "", "Sykmeldingsperioden er over 3 måneder.", { (sykmelding, _) ->
                sykmelding.perioder.any { (it.fom..it.tom).daysBetween() > 91 }
    }),

    @Description("Kun reisetilskudd er angitt. Melding sendt til oppfølging i Arena, skal ikke registreres i Infotrygd.")
    TRAVEL_SUBSIDY_SPECIFIED(1608, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.PLANLAGT,
            "", "Kun reisetilskudd er angitt", { (sykmelding, _) ->
        sykmelding.perioder.any { it.reisetilskudd == true }
    }),

    @Description("Hvis sykmelder har gitt veiledning til arbeidsgiver/arbeidstaker (felt 9.1).")
    MESSAGE_TO_EMPLOYER(1609, ArenaHendelseType.VEILEDNING_TIL_ARBEIDSGIVER, ArenaHendelseStatus.UTFORT,
            "", "Sykmelder har gitt veiledning til arbeidsgiver/arbeidstaker (felt 9.1)", { (sykmelding, _) ->
            !sykmelding.meldingTilArbeidsgiver.isNullOrBlank()
    }),

    @Description("Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.")
    PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2(1615, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT,
            "", "Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.", { (sykmelding, metadata) ->
        sykmelding.perioder
                .any { (it.fom..it.tom).daysBetween() > 56 } && kotlin.collections.listOf("", "1").contains(metadata.rulesetVersion ?: "")
    }),

    @Description("Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.")
    PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2(1615, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT,
            "", "Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.", { (sykmelding, metadata) ->
        sykmelding.perioder
                .any { (it.fom..it.tom).daysBetween() > 49 } && metadata.rulesetVersion == "2"
    }),

    @Description("Hvis sykmeldingen inneholder melding fra behandler skal meldingen til oppfølging i Arena.")
    MESSAGE_TO_NAV_ASSISTANCE_IMMEDIATLY(1616, ArenaHendelseType.MELDING_FRA_BEHANDLER, ArenaHendelseStatus.PLANLAGT,
            "", "Melding fra behandler bistandUmiddelbart", { (sykmelding, _) ->
        sykmelding.meldingTilNAV != null && sykmelding!!.meldingTilNAV!!.bistandUmiddelbart
    }),

    @Description("Hvis utdypende opplysninger om medisinske er oppgitt ved 7/8, 17, 39 uker settes merknad")
    DYNAMIC_QUESTIONS(1617, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT,
            "", "Utdypende opplysninger foreligger.", { (sykmelding, _) ->
        !sykmelding.utdypendeOpplysninger.isEmpty()
    }),

    @Description("Hvis sykmeldingen inneholer tiltakNAV eller andreTiltak, så skal merknad lages og hendelse sendes til Arena")
    MEASURES_OTHER_OR_NAV(1618, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.PLANLAGT,
            "", "Informasjon fra behandler til NAV. Åpne dokumentet for å se behandlers innspill til NAV.", { (sykmelding, _) ->
        sykmelding.tiltakNAV != null || !sykmelding.andreTiltak.isNullOrBlank()
    }),

    @Description("Hvis utdypende opplysninger foreligger og pasienten søker om AAP")
    DYNAMIC_QUESTIONS_AAP(1620, ArenaHendelseType.INFORMASJON_FRA_SYKMELDING, ArenaHendelseStatus.UTFORT,
            "", "Opplysninger om AAP foreligger", { (sykmelding, _) ->
        sykmelding.utdypendeOpplysninger.any { it.key == "6.6" }
    }),
}

fun List<Periode>.sortedTOMDate(): List<LocalDate> =
        map { it.tom }.sorted()

fun ClosedRange<LocalDate>.daysBetween(): Long = ChronoUnit.DAYS.between(start, endInclusive)

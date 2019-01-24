package no.nav.syfo.rules

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.model.Status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class RuleMetadata(
    val signatureDate: LocalDateTime,
    val receivedDate: LocalDateTime
)

enum class ValidationRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    @Description("Hvis sykmeldingens sluttdato er mer enn 3 måneder frem i tid skal meldingen til oppfølging i Arena")
    SICK_LAVE_END_DATE_MORE_THAN_3_MONTHS(1603, Status.INVALID, { (healthInformation, ruleMetadata) ->
        // 	lagArenaHendelse(ArenaHendelseTypeEnum.VURDER_OPPFOLGING.toString(), "", ArenaHendelseStatusEnum.PLANLAGT.toString()
        if (!healthInformation.aktivitet?.periode.isNullOrEmpty()) {
            healthInformation.aktivitet.periode.sortedTOMDate().last().atStartOfDay() < ruleMetadata.signatureDate.plusMonths(3)
        } else {
            false
        }
    }),

    @Description("Hvis sykmeldingsperioden er over 3 måneder skal meldingen til oppfølging i Arena")
    SICK_LAVE_PERIODE_MORE_THEN_3_MONTHS(1606, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.VURDER_OPPFOLGING.toString(), "", ArenaHendelseStatusEnum.PLANLAGT.toString()
            healthInformation.aktivitet.periode.any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 91 }
    }),

    @Description("Forlengelse ut over maxdato.")
    MAX_SICK_LEAVE_PAYOUT(1607, Status.INVALID, { (healthInformation, _) ->
        // 	lagArenaHendelse(ArenaHendelseTypeEnum.INFORMASJON_FRA_SYKMELDING.toString(), "", ArenaHendelseStatusEnum.PLANLAGT.toString()
        // infotrygdForesp.sMhistorikk?.sykmelding?.first()?.periode?.stans == "MAX"
        false
    }),

    @Description("Kun reisetilskudd er angitt. Melding sendt til oppfølging i Arena, skal ikke registreres i Infotrygd.")
    TRAVEL_SUBSIDY_SPECIFIED(1608, Status.INVALID, { (healthInformation, _) ->
        // 	lagArenaHendelse(ArenaHendelseTypeEnum.INFORMASJON_FRA_SYKMELDING.toString(), "", ArenaHendelseStatusEnum.PLANLAGT.toString()
        healthInformation.aktivitet.periode.any { it.isReisetilskudd == true } // Can be null, so use equality
    }),

    @Description("Hvis sykmelder har gitt veiledning til arbeidsgiver/arbeidstaker (felt 9.1).")
    MESSAGE_TO_EMPLOYER(1609, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.VEILEDNING_TIL_ARBEIDSGIVER.toString(), meldingSM13.meldingTilArbeidsgiver, ArenaHendelseStatusEnum.UTFORT.toString()
            !healthInformation.meldingTilArbeidsgiver.isNullOrBlank()
    }),

    @Description("Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.")
    PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_BEFORE_RULESETT_2(1615, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.INFORMASJON_FRA_SYKMELDING.toString(), "", ArenaHendelseStatusEnum.UTFORT.toString()
        val rulesettversion = healthInformation.regelSettVersjon ?: ""
        val timeGroup8Week = healthInformation.aktivitet.periode
                .any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 56 }

        timeGroup8Week && kotlin.collections.listOf("", "1").contains(rulesettversion)
    }),

    @Description("Sykmeldingsperioden har passert tidspunkt for vurdering av aktivitetsmuligheter. Åpne dokumentet for å se behandlers innspill til aktivitetsmuligheter.")
    PASSED_REVIEW_ACTIVITY_OPPERTUNITIES_AFTER_RULESETT_2(1615, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.INFORMASJON_FRA_SYKMELDING.toString(), "", ArenaHendelseStatusEnum.UTFORT.toString()
        val rulesettversion2 = healthInformation.regelSettVersjon == "2"
        val timeGroup7Week = healthInformation.aktivitet.periode
                .any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 49 }

        timeGroup7Week && rulesettversion2
    }),

    @Description("Hvis sykmeldingen inneholder melding fra behandler skal meldingen til oppfølging i Arena.")
    MESSAGE_TO_NAV_ASSISTANCE_IMMEDIATLY(1616, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.MELDING_FR_A_BEHANDLER.toString(), meldingSM13.meldingTilNav.beskrivBistandNAV, ArenaHendelseStatusEnum.PLANLAGT.toString()
        healthInformation.meldingTilNav.isBistandNAVUmiddelbart
    }),

    @Description("Hvis utdypende opplysninger om medisinske er oppgitt ved 7/8, 17, 39 uker settes merknad")
    DYNAMIC_QUESTIONS(1617, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.INFORMASJON_FRA_SYKMELDING.toString(), "", ArenaHendelseStatusEnum.UTFORT.toString()
        !healthInformation.utdypendeOpplysninger.spmGruppe.isNullOrEmpty()
    }),

    @Description("Hvis sykmeldingen inneholer tiltakNAV eller andreTiltak, så skal merknad lages og hendelse sendes til Arena")
    MEASURES_OTHER_OR_NAV(1618, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.INFORMASJON_FRA_SYKMELDING.toString(), meldingSM13.meldingTilArbeidsgiver, ArenaHendelseStatusEnum.PLANLAGT.toString()
        !healthInformation.tiltak.andreTiltak.isNullOrBlank() || !healthInformation.tiltak.tiltakNAV.isNullOrBlank()
    }),

    @Description("Hvis utdypende opplysninger foreligger og pasienten søker om AAP")
    INVALID_FNR(1620, Status.INVALID, { (healthInformation, _) ->
        // lagArenaHendelse(ArenaHendelseTypeEnum.INFORMASJON_FRA_SYKMELDING.toString(), meldingSM13.meldingTilArbeidsgiver, ArenaHendelseStatusEnum.UTFORT.toString()
        healthInformation.utdypendeOpplysninger.spmGruppe.any {
            it.spmGruppeId == "6.6"
        }
    }),
}

fun List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>.sortedTOMDate(): List<LocalDate> =
        map { it.periodeTOMDato }.sorted()

fun ClosedRange<LocalDate>.daysBetween(): Long = ChronoUnit.DAYS.between(start, endInclusive)
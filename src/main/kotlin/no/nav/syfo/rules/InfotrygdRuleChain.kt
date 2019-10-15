package no.nav.syfo.rules

import no.nav.helse.infotrygd.foresp.InfotrygdForesp
import no.nav.helse.infotrygd.foresp.TypeSMinfo
import no.nav.syfo.arena.ArenaHendelseStatus
import no.nav.syfo.arena.ArenaHendelseType
import no.nav.syfo.model.Periode

enum class InfotrygdRuleChain(
    override val ruleId: Int?,
    override val arenaHendelseType: ArenaHendelseType,
    override val arenaHendelseStatus: ArenaHendelseStatus,
    override val arenaHendelseTekst: String,
    override val predicate: (RuleData<InfotrygdForesp>) -> Boolean
) : Rule<RuleData<InfotrygdForesp>> {

    @Description("Hvis uføregrad er høyere enn det er i infotrygd skal meldingen til oppfølging i Arena")
    UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD(
            1530,
            ArenaHendelseType.VURDER_OPPFOLGING,
            ArenaHendelseStatus.PLANLAGT,
            "uføregrad er 100% og den er høyere enn det som er registert i infotrygd",
            { (sykmelding, infotrygdForesp) ->
                infotrygdForesp.sMhistorikk != null &&
                        infotrygdForesp.sMhistorikk.sykmelding.sortedSMInfos().lastOrNull() != null &&
                        !sykmelding.perioder.isNullOrEmpty() &&
                        !forstegangsSykmelding(infotrygdForesp, sykmelding.perioder.sortedSykmeldingPeriodeFOMDate().first()) &&
                        sykmelding.perioder.any { periode ->
                            infotrygdForesp.sMhistorikk.sykmelding.sortedSMInfos().last().periode.ufoeregrad < periode.findGrad().toBigInteger() &&
                                    periode.findGrad() == 100
                        }
            })
}

fun forstegangsSykmelding(infotrygdForesp: InfotrygdForesp, periode: Periode): Boolean {
    val typeSMinfo = infotrygdForesp.sMhistorikk?.sykmelding
            ?.sortedSMInfos()
            ?.lastOrNull()
            ?: return true

    return (infotrygdForesp.sMhistorikk.status.kodeMelding == "04" ||
            (typeSMinfo.periode.arbufoerTOM != null && (typeSMinfo.periode.arbufoerTOM..periode.fom).daysBetween() > 1))
}

fun List<TypeSMinfo>.sortedSMInfos(): List<TypeSMinfo> =
        sortedBy { it.periode.arbufoerTOM }

fun Periode.findGrad(): Int =
        if (gradert?.grad != null) {
            gradert!!.grad
        } else {
            100
        }

fun List<Periode>.sortedSykmeldingPeriodeFOMDate(): List<Periode> =
        sortedBy { it.fom }

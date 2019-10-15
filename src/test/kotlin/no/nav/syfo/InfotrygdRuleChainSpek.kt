package no.nav.syfo

import java.time.LocalDate
import no.nav.helse.infotrygd.foresp.InfotrygdForesp
import no.nav.helse.infotrygd.foresp.StatusType
import no.nav.helse.infotrygd.foresp.TypeSMinfo
import no.nav.syfo.model.Gradert
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.InfotrygdRuleChain
import no.nav.syfo.rules.RuleData
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object InfotrygdRuleChainSpek : Spek({

    fun deafaultInfotrygdForesp() = InfotrygdForesp().apply {
        hovedStatus = StatusType().apply {
            kodeMelding = "00"
        }
    }

    fun ruleData(
        sykmelding: Sykmelding,
        infotrygdForesp: InfotrygdForesp
    ): RuleData<InfotrygdForesp> = RuleData(sykmelding, infotrygdForesp)

    describe("Infotrygd rule tests") {

        it("Should check rule UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD, should trigger rule") {
            val generateSykmelding = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 6, 27),
                            tom = LocalDate.of(2019, 6, 28)
                    )
            ))

            val infotrygdForespResponse = deafaultInfotrygdForesp()
            infotrygdForespResponse.sMhistorikk = InfotrygdForesp.SMhistorikk().apply {
                sykmelding.add(TypeSMinfo().apply {
                    periode = TypeSMinfo.Periode().apply {
                        arbufoerFOM = LocalDate.of(2019, 6, 26)
                        ufoeregrad = 80.toBigInteger()
                    }
                })
                status = StatusType().apply {
                    kodeMelding = "01"
                }
            }

            InfotrygdRuleChain.UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD(ruleData(generateSykmelding, infotrygdForespResponse)) shouldEqual true
        }

        it("Should check rule UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD, should trigger rule") {
            val generateSykmelding = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 6, 27),
                            tom = LocalDate.of(2019, 6, 28)
                    )
            ))

            val infotrygdForespResponse = deafaultInfotrygdForesp()
            infotrygdForespResponse.sMhistorikk = InfotrygdForesp.SMhistorikk().apply {
                sykmelding.add(TypeSMinfo().apply {
                    periode = TypeSMinfo.Periode().apply {
                        arbufoerFOM = LocalDate.of(2019, 6, 26)
                        ufoeregrad = 80.toBigInteger()
                    }
                })
                status = StatusType().apply {
                    kodeMelding = "01"
                }
            }

            InfotrygdRuleChain.UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD(ruleData(generateSykmelding, infotrygdForespResponse)) shouldEqual true
        }

        it("Should check rule UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD, should NOT trigger rule") {
            val generateSykmelding = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 6, 27),
                            tom = LocalDate.of(2019, 6, 28),
                            gradert = Gradert(
                                    reisetilskudd = true,
                                    grad = 90
                            )
                    )
            ))

            val infotrygdForespResponse = deafaultInfotrygdForesp()
            infotrygdForespResponse.sMhistorikk = InfotrygdForesp.SMhistorikk().apply {
                sykmelding.add(TypeSMinfo().apply {
                    periode = TypeSMinfo.Periode().apply {
                        arbufoerFOM = LocalDate.of(2019, 6, 26)
                        ufoeregrad = 90.toBigInteger()
                    }
                })
                status = StatusType().apply {
                    kodeMelding = "01"
                }
            }

            InfotrygdRuleChain.UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD(ruleData(generateSykmelding, infotrygdForespResponse)) shouldEqual false
        }

        it("Should check rule UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD, should NOT trigger rule") {
            val generateSykmelding = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 6, 27),
                            tom = LocalDate.of(2019, 6, 28),
                            gradert = Gradert(
                                    reisetilskudd = true,
                                    grad = 90
                            )
                    )
            ))

            val infotrygdForespResponse = deafaultInfotrygdForesp()
            infotrygdForespResponse.sMhistorikk = InfotrygdForesp.SMhistorikk().apply {
                sykmelding.add(TypeSMinfo().apply {
                    periode = TypeSMinfo.Periode().apply {
                        arbufoerFOM = LocalDate.of(2019, 6, 24)
                        arbufoerTOM = LocalDate.of(2019, 6, 25)
                        ufoeregrad = 80.toBigInteger()
                    }
                })
                status = StatusType().apply {
                    kodeMelding = "01"
                }
            }

            InfotrygdRuleChain.UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD(ruleData(generateSykmelding, infotrygdForespResponse)) shouldEqual false
        }

        it("Should check rule UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD, should NOT trigger rule") {
            val generateSykmelding = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 6, 27),
                            tom = LocalDate.of(2019, 6, 28),
                            gradert = Gradert(
                                    reisetilskudd = true,
                                    grad = 70
                            )
                    )
            ))

            val infotrygdForespResponse = deafaultInfotrygdForesp()
            infotrygdForespResponse.sMhistorikk = InfotrygdForesp.SMhistorikk().apply {
                sykmelding.add(TypeSMinfo().apply {
                    periode = TypeSMinfo.Periode().apply {
                        arbufoerFOM = LocalDate.of(2019, 6, 24)
                        arbufoerTOM = LocalDate.of(2019, 6, 25)
                        ufoeregrad = 80.toBigInteger()
                    }
                })
                status = StatusType().apply {
                    kodeMelding = "01"
                }
            }

            InfotrygdRuleChain.UFOREGRADEN_ER_100_OG_HOYERE_ENN_I_INFOTRYGD(ruleData(generateSykmelding, infotrygdForespResponse)) shouldEqual false
        }
    }
})

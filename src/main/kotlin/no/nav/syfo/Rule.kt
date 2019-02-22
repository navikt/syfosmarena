package no.nav.syfo

import no.nav.syfo.arena.ArenaHendelseStatus
import no.nav.syfo.arena.ArenaHendelseType
import no.nav.syfo.model.Sykmelding

data class RuleData<T>(val sykmelding: Sykmelding, val metadata: T)

interface Rule<in T> {
    val name: String
    val ruleId: Int?
    val arenaHendelseStatus: ArenaHendelseStatus
    val arenaHendelseType: ArenaHendelseType
    val predicate: (T) -> Boolean
    operator fun invoke(input: T) = predicate(input)
}

inline fun <reified T, reified R : Rule<RuleData<T>>> List<R>.executeFlow(sykmelding: Sykmelding, value: T): List<Rule<Any>> =
        filter { it.predicate(RuleData(sykmelding, value)) }
                .map { it as Rule<Any> }
                .onEach { RULE_HIT_COUNTER.labels(it.name).inc() }

inline fun <reified T, reified R : Rule<RuleData<T>>> Array<R>.executeFlow(sykmelding: Sykmelding, value: T): List<Rule<Any>> = toList().executeFlow(sykmelding, value)

@Retention(AnnotationRetention.RUNTIME)
annotation class Description(val description: String)

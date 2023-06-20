package no.nav.syfo.metrics

import io.prometheus.client.Counter

const val NAMESPACE = "syfosmarena"

val RULE_HIT_COUNTER: Counter =
    Counter.Builder()
        .namespace(NAMESPACE)
        .name("rule_hit_counter")
        .labelNames("rule_name")
        .help("Registers a counter for each rule in the rule set")
        .register()

val ARENA_EVENT_COUNTER: Counter =
    Counter.Builder()
        .namespace(NAMESPACE)
        .name("arena_event_counter")
        .help("Registers a counter for each arena event")
        .register()

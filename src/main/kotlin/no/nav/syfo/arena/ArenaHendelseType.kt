package no.nav.syfo.arena

enum class ArenaHendelseType(val type: String) {
    VEILEDNING_TIL_ARBEIDSGIVER("VEIL_AG_AT"),
    INFORMASJON_FRA_SYKMELDING("MESM_I_SM"),
    VURDER_OPPFOLGING("MESM_V_OPF"),
}

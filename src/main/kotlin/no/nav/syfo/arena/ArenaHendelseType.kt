package no.nav.syfo.arena

enum class ArenaHendelseType(val type: String) {
    VEILEDNING_TIL_ARBEIDSGIVER("VEIL_AG_AT"),
    MELDING_FRA_BEHANDLER("MESM_M_BEH"),
    INFORMASJON_FRA_SYKMELDING("MESM_I_SM"),
    VURDER_OPPFOLGING("MESM_V_OPF"),
    NY_VURDERING("MESM_N_VRD")
}
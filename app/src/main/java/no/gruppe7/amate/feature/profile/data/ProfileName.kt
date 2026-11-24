package no.gruppe7.amate.feature.profile.data

fun validProfileNameOrNull(input: String): String? =
    input.trim().takeIf { it.length in 1..40 }

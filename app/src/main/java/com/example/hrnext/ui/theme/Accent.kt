package com.example.hrnext.ui.theme

import androidx.compose.ui.graphics.Color

/** A stable (container, onContainer, solid) accent triple used to color module icons and avatars. */
data class Accent(val container: Color, val onContainer: Color, val solid: Color)

private val ACCENTS = listOf(
    Accent(AccentIndigoContainer, AccentIndigoOn, AccentIndigo),
    Accent(AccentTealContainer, AccentTealOn, AccentTeal),
    Accent(AccentOrangeContainer, AccentOrangeOn, AccentOrange),
    Accent(AccentRoseContainer, AccentRoseOn, AccentRose),
    Accent(AccentVioletContainer, AccentVioletOn, AccentViolet),
    Accent(AccentGreenContainer, AccentGreenOn, AccentGreen),
    Accent(AccentAmberContainer, AccentAmberOn, AccentAmber),
    Accent(AccentSkyContainer, AccentSkyOn, AccentSky),
)

/** Deterministically picks an accent color for a given key (doctype, title, etc.) so it stays stable across recompositions. */
fun accentColorFor(key: String): Accent {
    val index = key.sumOf { it.code }.mod(ACCENTS.size)
    return ACCENTS[index]
}

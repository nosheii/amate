package com.example.amate.core.designsystem.component

import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Gjenbrukbar plassholder for vann-, skritt- og malkort. */
@Composable
fun MetricCard(title: String) {
    Card {
        Text(text = title)
    }
}

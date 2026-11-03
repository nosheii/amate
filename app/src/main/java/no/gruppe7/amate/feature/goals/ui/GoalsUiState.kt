package no.gruppe7.amate.feature.goals.ui

import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType

// Alt skjermen trenger å vise. Room er kilden til listen.
data class GoalsUiState(
    val goals: List<GoalEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val errorMessageResId: Int? = null,
    val editingType: GoalType? = null, // null = redigeringsfeltet
    val valueInput: String = "",
    val inputErrorResId: Int? = null,
) {
    fun goalFor(type: GoalType): GoalEntity? = goals.firstOrNull { it.type == type }
}

package no.gruppe7.amate.feature.home.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.core.navigation.AmateShell
import no.gruppe7.amate.feature.tasks.data.RoomTaskRepository
import no.gruppe7.amate.feature.tasks.ui.TasksRoute
import no.gruppe7.amate.feature.tasks.ui.rememberTasksViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeTasksIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AmateDatabase
    private lateinit var repository: RoomTaskRepository
    private val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AmateDatabase::class.java).build()
        repository = RoomTaskRepository(db.selfCareTaskDao())
        compose.setContent {
            val model = rememberTasksViewModel(repository)
            val state by model.uiState.collectAsStateWithLifecycle()
            AmateShell(waterContent = {}, settingsContent = {}, profileContent = {}, weatherContent = {},
                tasksState = state, onRetryTasks = model::retry, onPrepareNewTask = { model.openEditor() },
                tasksContent = { TasksRoute(repository, model = model) })
        }
    }
    @After fun cleanup() { db.close() }

    private fun card() {
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_open_tasks"))
    }
    private fun expectTotal(text: String) {
        card()
        compose.waitUntil(5000) {
            runCatching { compose.onNodeWithTag("home_tasks_total").assertTextEquals(text) }.isSuccess
        }
    }

    @Test fun todaySummaryFollowsCompletionUndoAndDeletion_andEmptyButtonOpensEditor() {
        runBlocking {
            repository.addTask(1, "Gå en tur", today)
            val second = repository.addTask(1, "Les en bok", today)
            val tasks = repository.observeTasksForDay(1, today, today + 1).first()
            repository.setCompleted(tasks.single { it.id == second }, true, today)
            // Andre dager og andre brukere skal ikke telles i hjemkortet.
            repository.addTask(1, "I morgen", LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
            repository.addTask(2, "En annen bruker", today)
        }
        expectTotal("1 av 2 fullført")
        compose.onNodeWithTag("home_open_tasks").assertTextEquals("Se gjøremål").performClick()
        val pending = runBlocking { repository.observeTasksForDay(1, today, today + 1).first().single { !it.isCompleted } }
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("task_${pending.id}"))
        compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("task_${pending.id}"))).performClick()
        compose.onNodeWithContentDescription("Tilbake til hjem").performClick()
        expectTotal("2 av 2 fullført")
        compose.onNodeWithTag("home_tasks_all_done").assertIsDisplayed()
        runBlocking { repository.setCompleted(pending, false, today) }
        expectTotal("1 av 2 fullført")
        runBlocking { repository.observeTasksForDay(1, today, today + 1).first().forEach { repository.delete(it) } }
        card()
        compose.waitUntil(5000) {
            runCatching { compose.onNodeWithTag("home_tasks_empty").assertIsDisplayed() }.isSuccess
        }
        compose.onNodeWithTag("home_tasks_progress").assertDoesNotExist()
        compose.onNodeWithTag("home_open_tasks").assertTextEquals("Legg til gjøremål").performClick()
        compose.onNodeWithText("Nytt gjøremål").assertIsDisplayed()
    }
}

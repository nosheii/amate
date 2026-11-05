package no.gruppe7.amate.feature.tasks.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


/** Instrumenterte tester av lagring og dagsavgrensning i task-DAO-en. */
@RunWith(AndroidJUnit4::class)
class SelfCareTaskDaoTest {
    private lateinit var database: AmateDatabase
    private lateinit var taskDao: SelfCareTaskDao

    @Before
    fun setup() {
        // En separat minnedatabase gjør testene uavhengige av appens lagrede data.
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        taskDao = database.selfCareTaskDao()
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun insert_savesTask() = runBlocking {
        taskDao.insert(SelfCareTaskEntity(userId = 1, title = "Gå en tur", scheduledForMillis = 1000))
        val tasks = taskDao.observeTasksForDay(1,0,5000).first()
        assertEquals(1, tasks.size)
        assertEquals("Gå en tur", tasks.first().title)
        assertFalse(tasks.first().isCompleted)
    }

    @Test
    fun dayQuery_excludesOtherUsersAndUsesExclusiveEnd() = runBlocking {
        // Starttidspunktet er med, sluttidspunktet er ikke med, og andre brukere skjules.
        listOf(
            SelfCareTaskEntity(userId = 1, title = "Dagen før", scheduledForMillis = 999),
            SelfCareTaskEntity(userId = 1, title = "Start", scheduledForMillis = 1000),
            SelfCareTaskEntity(userId = 1, title = "Dagen etter", scheduledForMillis = 2000),
            SelfCareTaskEntity(userId = 2, title = "Annen bruker", scheduledForMillis = 1500),
        ).forEach { taskDao.insert(it) }

        assertEquals(listOf("Start"), taskDao.observeTasksForDay(1, 1000, 2000).first().map{ it.title })
    }

    @Test
    fun openTasks_areListedBeforeCompleted() = runBlocking {
        // TODO: Sett inn både åpne og fullførte gjøremål, og bekreft rekkefølgen fra DAO-en.
    }
}

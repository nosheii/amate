package no.gruppe7.amate.feature.profile.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import no.gruppe7.amate.core.database.AmateDatabase
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class AvatarConfigDaoTest {
    private lateinit var database: AmateDatabase
    private lateinit var dao: AvatarConfigDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        dao = database.avatarConfigDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun observeAvatarConfig_ikke_eksisterende_NULL() = runTest {
        val config = dao.observeAvatarConfig(999).first()
        assertNull(config)
    }

    @Test
    fun upsert_lagrer_ny_avatar() = runTest {
        dao.upsert(AvatarConfigEntity(userId = 1, avatarKey = "katt"))

        val config = dao.observeAvatarConfig(1).first()
        assertEquals("katt", config?.avatarKey)
    }

    @Test
    fun upsert_oppdaterer_eksisterende_avatar() = runTest {
        dao.upsert(AvatarConfigEntity(userId = 1, avatarKey = "katt"))
        dao.upsert(AvatarConfigEntity(userId = 1, avatarKey = "bjørn"))

        val config = dao.observeAvatarConfig(1).first()
        assertEquals("bjørn", config?.avatarKey)
    }

}
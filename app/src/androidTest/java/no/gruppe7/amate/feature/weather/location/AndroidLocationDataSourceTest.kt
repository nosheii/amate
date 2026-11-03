package no.gruppe7.amate.feature.weather.location

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidLocationDataSourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun missingPermission_returnsBeforeTryingToReadLocation() = runBlocking {
        // Etterligner et nei uten å endre tillatelsene på brukerens emulator.
        val deniedContext = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
                if (permission == Manifest.permission.ACCESS_COARSE_LOCATION) PackageManager.PERMISSION_DENIED
                else super.checkPermission(permission, pid, uid)
        }
        assertEquals(WeatherLocationResult.PermissionRequired,
            AndroidLocationDataSource(deniedContext).getCurrentLocation())
    }

    @Suppress("DEPRECATION") // Denne varianten fungerer også på appens eldste Android-versjon.
    @Test fun manifestRequestsApproximateOnly_notPreciseOrBackgroundLocation() {
        val permissions = context.packageManager.getPackageInfo(context.packageName,
            PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        assertTrue(Manifest.permission.ACCESS_COARSE_LOCATION in permissions)
        assertFalse(Manifest.permission.ACCESS_FINE_LOCATION in permissions)
        assertFalse(Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions)
    }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and admob config from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Apex City Rush", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.admob_app_id).startsWith("ca-app-pub-"))
        assertTrue(context.getString(R.string.admob_banner_unit_id).startsWith("ca-app-pub-"))
        assertTrue(context.getString(R.string.admob_interstitial_unit_id).startsWith("ca-app-pub-"))
        assertTrue(context.getString(R.string.admob_rewarded_unit_id).startsWith("ca-app-pub-"))
    }
}

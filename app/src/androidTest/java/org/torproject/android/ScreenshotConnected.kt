package org.torproject.android

import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHost
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.torproject.android.service.OrbotConstants
import org.torproject.android.ui.connect.ConnectFragment
import org.torproject.android.ui.connect.ConnectUiState
import org.torproject.android.ui.connect.ConnectViewModel
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class ScreenshotConnected : BaseScreenshotTest() {
    @get:Rule
    var mActivityScenarioRule = ActivityScenarioRule(OrbotActivity::class.java)

    @Before
    fun setup() {
        mActivityScenarioRule.scenario.onActivity { activity ->
            val navHost = activity.supportFragmentManager.primaryNavigationFragment
            val connectFrag = navHost!!.childFragmentManager.fragments[0] as ConnectFragment
            connectFrag.doLayoutOn(connectFrag.requireContext())
        }
    }


    @Test
    fun screenshotConnected() {
        mActivityScenarioRule.scenario.onActivity { activity ->
            onView(withId(R.id.lvConnected)).isVisible()
            Thread.sleep(300)
            Screengrab.screenshot("A-orbot_connected")
        }
    }

}
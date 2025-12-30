package org.torproject.android

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab

class ScreenshotChooseHowToConnect : BaseScreenshotTest() {

    @get:Rule
    var mActivityScenarioRule = ActivityScenarioRule(OrbotActivity::class.java)

    @Test
    fun screenshotChooseHowToConnect() {
        val linearLayout = onData(anything())
            .inAdapterView(
                allOf(
                    withId(R.id.lvConnected),
                    childAtPosition(
                        withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                        5
                    )
                )
            )
            .atPosition(1)
        linearLayout.perform(scrollTo(), click())

        onView(withId(R.id.rbDirect)).isVisible()
        Screengrab.screenshot("B-choose-how")
    }
}

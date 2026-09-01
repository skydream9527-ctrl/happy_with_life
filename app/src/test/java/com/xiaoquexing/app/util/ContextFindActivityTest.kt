package com.xiaoquexing.app.util

import android.app.Activity
import android.content.ContextWrapper
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContextFindActivityTest {
    @Test
    fun unwrapsThemeWrapperToActivity() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped = ContextWrapper(ContextWrapper(activity))
        assertSame(activity, wrapped.findActivity())
    }
}

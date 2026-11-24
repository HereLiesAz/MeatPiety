package com.hereliesaz.meatpiety

import android.app.Activity
import com.hereliesaz.aznavrail.updates.AzUpdateChecker
import com.hereliesaz.aznavrail.updates.play.AzPlayUpdates

/** Checks the installed distribution without accessing user diet data. */
internal fun checkAppUpdate(activity: Activity) {
    AzUpdateChecker.check(activity, "HereLiesAz/MeatPiety", playSource = AzPlayUpdates)
}

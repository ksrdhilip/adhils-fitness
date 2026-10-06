package com.adhils.fitness.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.adhils.fitness.FitnessApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    FitnessApp()
}

fun MainViewController(initialRoute: String?): UIViewController = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    FitnessApp(initialRoute = initialRoute)
}

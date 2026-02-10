package com.gm.qualcommaimodelsdemo.utils

import androidx.navigation.NavHostController

object NavigationUtils {
    fun navigateWithoutParameter(navController: NavHostController, screenName: String) {
        navController.navigate(screenName)
    }
}
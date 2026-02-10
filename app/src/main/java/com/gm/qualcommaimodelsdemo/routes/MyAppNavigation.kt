package com.gm.qualcommaimodelsdemo.routes

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gm.qualcommaimodelsdemo.screens.DepthScreen
import com.gm.qualcommaimodelsdemo.screens.MainScreen
import com.gm.qualcommaimodelsdemo.screens.ObjectDetectionScreen
import com.gm.qualcommaimodelsdemo.screens.SegmentationScreen
import com.gm.qualcommaimodelsdemo.screens.SuperScaleScreen
import com.gm.qualcommaimodelsdemo.screens.VideoClassificationScreen

@Composable
fun MyAppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.HOME_SCREEN, builder = {
        composable(Routes.HOME_SCREEN) { MainScreen(navController) }
        composable(Routes.DEPTH_SCREEN) { DepthScreen(navController) }
        composable(Routes.OBJECT_DETCTION_SCREEN) { ObjectDetectionScreen(navController) }
        composable(Routes.SEGMENTATION_SCREEN) { SegmentationScreen(navController) }
        composable(Routes.SUPER_SCALE_SCREEN) { SuperScaleScreen(navController) }
        composable(Routes.VIDEO_CLASSIFCATION_SCREEN) { VideoClassificationScreen(navController) }
    })
}
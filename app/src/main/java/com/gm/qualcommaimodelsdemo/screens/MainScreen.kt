package com.gm.qualcommaimodelsdemo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gm.qualcommaimodelsdemo.routes.Routes
import com.gm.qualcommaimodelsdemo.ui.theme.QualcommAiModelsDemoTheme
import com.gm.qualcommaimodelsdemo.viewmodel.MainScreenViewModel

@Preview
@Composable
fun MainScreen(
    navController: NavHostController = NavHostController(LocalContext.current),
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    QualcommAiModelsDemoTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { AppBarMainScreen() }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                        start = 16.dp,
                        end = 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { navController.navigate(Routes.OBJECT_DETCTION_SCREEN) }, Modifier
                        .fillMaxWidth(1f)
                        .height(50.dp)
                ) {
                    Text("Object Detection and Classification")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Routes.DEPTH_SCREEN) }, Modifier
                        .fillMaxWidth(1f)
                        .height(50.dp)
                ) {
                    Text("Depth Estimation")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Routes.SEGMENTATION_SCREEN) }, Modifier
                        .fillMaxWidth(1f)
                        .height(50.dp)
                ) {
                    Text("Image Segmentation")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Routes.SUPER_SCALE_SCREEN) }, Modifier
                        .fillMaxWidth(1f)
                        .height(50.dp)
                ) {
                    Text("Super Resolution")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Routes.VIDEO_CLASSIFCATION_SCREEN) },
                    Modifier
                        .fillMaxWidth(1f)
                        .height(50.dp)
                ) {
                    Text("Video Classification")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarMainScreen() {
    TopAppBar(
        title = {
            Text(text = "Qualcomm Ai Models Demo")
        },
        navigationIcon = {
            IconButton(onClick = { /* Handle navigation icon click */ }) {
                Icon(Icons.Filled.Menu, contentDescription = "Navigation menu")
            }
        },
        actions = {
            // Action icons are added here
            IconButton(onClick = { /* Handle more options click */ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
        },
        // Customize colors (optional)
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        )
    )
}
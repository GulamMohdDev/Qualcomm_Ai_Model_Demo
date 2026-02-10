package com.gm.qualcommaimodelsdemo.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.gm.qualcommaimodelsdemo.ui.theme.QualcommAiModelsDemoTheme
import com.gm.qualcommaimodelsdemo.viewmodel.VideoClassificationScreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Preview
@Composable
fun VideoClassificationScreen(
    navController: NavHostController = NavHostController(LocalContext.current),
    viewModel: VideoClassificationScreenViewModel = hiltViewModel()
) {
    val estimatedOutputTime by viewModel.estimatedOutputTime.collectAsState()
    val result by viewModel.result.collectAsState()
    val videoUri by viewModel.videoUri.collectAsState()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.setVideoUri(uri)
        }
    )

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    val contentUri = FileProvider.getUriForFile(
        context,
        "com.gm.qualcommaimodelsdemo.fileprovider", // Authorities must match your manifest
        File(context.externalCacheDir, "my_video.mp4")
    )

    val recordVideoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            viewModel.setVideoUri(contentUri)
        }
    }

    QualcommAiModelsDemoTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { AppBarVideoClassificationScreen(navController) }) { innerPadding ->
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
                Spacer(Modifier.height(40.dp))
                
                if (videoUri != null) {
                    VideoPlayer(videoUri!!)
                } else {
                    Text("No Video Selected")
                }

                Spacer(Modifier.height(40.dp))
                Row(Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            recordVideoLauncher.launch(contentUri)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    }, Modifier.weight(1f)) {
                        Text("Record")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = {
                        galleryLauncher.launch("video/*")
                    }, Modifier.weight(1f)) {
                        Text("Gallery")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (videoUri != null) {
                        Toast.makeText(context, "Processing Video...", Toast.LENGTH_SHORT).show()
                        viewModel.classifyVideo()
                    } else {
                        Toast.makeText(context, "Please select a video first", Toast.LENGTH_SHORT).show()
                    }
                }, Modifier.fillMaxWidth(1f)) {
                    Text("Classify Video with Ai Model")
                }
                Spacer(Modifier.height(20.dp))
                Text("Output", color = Color.Black, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.fillMaxWidth())
                Text(estimatedOutputTime ?: "N/A", color = Color.Black, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text( result?: "", color = Color.Black, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarVideoClassificationScreen(navController: NavHostController) {
    TopAppBar(
        title = { Text(text = "Video Classification") },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        )
    )
}

@Composable
fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
            }
        },
        update = {
            it.player = exoPlayer
        }
    )
}

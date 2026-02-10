package com.gm.qualcommaimodelsdemo.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gm.qualcommaimodelsdemo.R
import com.gm.qualcommaimodelsdemo.ui.theme.QualcommAiModelsDemoTheme
import com.gm.qualcommaimodelsdemo.viewmodel.SuperScaleScreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Preview
@Composable
fun SuperScaleScreen(
    navController: NavHostController = NavHostController(LocalContext.current),
    viewModel: SuperScaleScreenViewModel = hiltViewModel()
) {
    val imageBitmap by viewModel.imageBitmap.collectAsState()
    val outImageBitmap by viewModel.outputImageBitmap.collectAsState()
    val estimatedOutputTime by viewModel.estimatedOutputTime.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.onImagePicked(context, uri)
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { thumbnailBitmap ->
            if (thumbnailBitmap != null) {
                viewModel.onImageCaptured(thumbnailBitmap)
            }
        }
    )

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    QualcommAiModelsDemoTheme {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {AppBarSuperScaleScreen(navController)}) { innerPadding ->
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
                Image(
                    bitmap = imageBitmap?.asImageBitmap()
                        ?: ImageBitmap.imageResource(id = R.drawable.placeholder),
                    contentDescription = "Depth Image",
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFCECECE))
                        .clip(RoundedCornerShape(16.dp))
                        .clipToBounds(),
                )
                Spacer(Modifier.height(40.dp))
                Row(Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            cameraLauncher.launch(null)
                        } else {
                            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                        }
                    }, Modifier.weight(1f)) {
                        Text("Camera")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = {
                        launcher.launch("image/*")
                    }, Modifier.weight(1f)) {
                        Text("Gallery")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    imageBitmap?.let {
                        viewModel.upScaleImage(it)
                        return@Button
                    }
                    Toast.makeText(context, "No Image Selected", Toast.LENGTH_SHORT).show()
                }, Modifier.fillMaxWidth(1f)) {
                    Text("Upscale Image with Ai Model")
                }
                Spacer(Modifier.height(20.dp))
                Text("Output", color = Color.Black, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.fillMaxWidth())
                Text(estimatedOutputTime?:"", color = Color.Black, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Image(
                    bitmap = outImageBitmap?.asImageBitmap()
                        ?: ImageBitmap.imageResource(id = R.drawable.placeholder),
                    contentDescription = "Output Image",
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFCECECE)),

                    )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun AppBarSuperScaleScreen(navController: NavHostController){
    TopAppBar(
        title = {
            Text(text = "Super Scale")
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Action icons are added here
            IconButton(onClick = { /* Handle search icon click */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
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
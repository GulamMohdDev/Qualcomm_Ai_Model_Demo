# Qualcomm AI Models Demo

This is an Android application that demonstrates the use of Qualcomm AI models.
* **
Note: Video Classification model not added in this repository. use below link to download this model

https://aihub.qualcomm.com/mobile/models/video_mae?domain=Computer+Vision&useCase=Video+Classification


## Project Overview

The project is built with modern Android development tools and libraries, including:

*   **Kotlin:** The primary programming language.
*   **Jetpack Compose:** For building the user interface.
*   **TensorFlow Lite:** For running machine learning models on-device.
*   **Hilt:** For dependency injection.
*   **Coil:** For image loading.
*   **Accompanist:** For permissions handling.
*   **Media3:** For video playback.

## Features

*   Demonstrates object detection using a TensorFlow Lite model.
*   Uses Jetpack Compose for the UI.
*   Integrates with the camera to perform real-time object detection.

## Getting Started

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Build and run the application on a physical device or emulator.

## Dependencies

The project uses the following dependencies:

*   **AndroidX:**
    *   `core-ktx`
    *   `lifecycle-runtime-ktx`
    *   `activity-compose`
    *   `compose-bom`
    *   `compose-ui`
    *   `compose-ui-graphics`
    *   `compose-ui-tooling-preview`
    *   `compose-material3`
    *   `navigation-compose`
    *   `hilt-navigation-compose`
    *   `lifecycle-viewmodel-compose`
    *   `media3-exoplayer`
    *   `media3-ui`
*   **TensorFlow Lite:**
    *   `tensorflow-lite-metadata`
    *   `tensorflow-lite-support`
    *   `tensorflow-lite-gpu`
*   **Object Detection:**
    *   `object-detection-common`
    *   `object-detection`
*   **Coil:**
    *   `coil-compose`
*   **Hilt:**
    *   `hilt-android`
    *   `hilt-android-compiler`
*   **Accompanist:**
    *   `accompanist-permissions`
*   **Testing:**
    *   `junit`
    *   `androidx-junit`
    *   `androidx-espresso-core`
    *   `androidx-compose-ui-test-junit4`

## Build Configuration

*   `minSdk`: 24
*   `targetSdk`: 36
*   `compileSdk`: 36
*   `JavaVersion`: 11

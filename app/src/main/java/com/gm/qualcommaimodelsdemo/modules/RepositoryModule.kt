package com.gm.qualcommaimodelsdemo.modules

import com.gm.qualcommaimodelsdemo.repository.repo_imp.DepthAiModelRepositoryImpl
import com.gm.qualcommaimodelsdemo.repository.repo_imp.ObjectDetectionAiModelRepositoryImpl
import com.gm.qualcommaimodelsdemo.repository.repo_imp.SemanticSegmentationAiModelRepositoryImpl
import com.gm.qualcommaimodelsdemo.repository.repo_imp.SuperResolutionAiModelRepositoryImpl
import com.gm.qualcommaimodelsdemo.repository.repo_imp.VideoClassificationModelRepositoryImpl
import com.gm.qualcommaimodelsdemo.repository.repo_interface.DepthAiModelRepository
import com.gm.qualcommaimodelsdemo.repository.repo_interface.ObjectDetectionAiModelRepository
import com.gm.qualcommaimodelsdemo.repository.repo_interface.SemanticSegmentationAiModelRepository
import com.gm.qualcommaimodelsdemo.repository.repo_interface.SuperResolutionAiModelRepository
import com.gm.qualcommaimodelsdemo.repository.repo_interface.VideoClassificationModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class) // Installs the binding in the application-wide component
abstract class RepositoryModule {

    @Binds
    abstract fun bindDepthAiModelRepository(
        depthAiModelRepositoryImpl: DepthAiModelRepositoryImpl,
    ): DepthAiModelRepository

    @Binds
    abstract fun bindObjectDetectionAiModelRepository(
        objectDetectionAiModelRepositoryImpl: ObjectDetectionAiModelRepositoryImpl
    ): ObjectDetectionAiModelRepository

    @Binds
    abstract fun bindSemanticSegmentationAiModelRepository(
        segmentationAiModelRepositoryImpl: SemanticSegmentationAiModelRepositoryImpl
    ): SemanticSegmentationAiModelRepository

    @Binds
    abstract fun bindSuperResolutionAiModelRepository(
        superResolutionAiModelRepositoryImpl: SuperResolutionAiModelRepositoryImpl
    ): SuperResolutionAiModelRepository

    @Binds
    abstract fun bindVideoClassificationModelRepository(
        videoClassificationModelRepositoryImpl: VideoClassificationModelRepositoryImpl
    ): VideoClassificationModelRepository
}
package com.gm.qualcommaimodelsdemo.viewmodel

import androidx.lifecycle.ViewModel
import com.gm.qualcommaimodelsdemo.logger.Logs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(): ViewModel(), Logs {
    override var TAG: String = "MainScreenViewModel"

}
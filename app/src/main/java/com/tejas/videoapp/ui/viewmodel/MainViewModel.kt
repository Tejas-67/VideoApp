package com.tejas.videoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.tejas.videoapp.datamodel.RoomModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow


@HiltViewModel
class MainViewModel: ViewModel() {
    var roomState: MutableStateFlow<List<RoomModel>?> = MutableStateFlow(null)

}

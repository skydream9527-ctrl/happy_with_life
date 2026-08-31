package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import com.xiaoquexing.app.data.remote.PlanStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlanUi(
    val previewMember: Boolean = false,
    val tierLabel: String = "免费版",
)

class PlanViewModel(
    private val store: PlanStore,
) : ViewModel() {
    private val _ui = MutableStateFlow(read())
    val uiState: StateFlow<PlanUi> = _ui.asStateFlow()

    fun setPreview(on: Boolean) {
        store.previewMember = on
        _ui.value = read()
    }

    private fun read() = PlanUi(
        previewMember = store.previewMember,
        tierLabel = if (store.previewMember) "会员预览" else "免费版",
    )
}

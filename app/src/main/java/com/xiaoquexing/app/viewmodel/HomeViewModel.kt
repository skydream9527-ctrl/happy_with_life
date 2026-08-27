package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.XiaoQueXingApp
import com.xiaoquexing.app.data.entity.PlantStage
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.util.GPCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val greeting: String = "你好",
    val dateStr: String = "",
    val activePlant: PlantState? = null,
    val currentGp: Int = 0,
    val nextStageGp: Int = 50,
    val progressInStage: Float = 0f,
    val stageName: String = "种子",
    val streakDays: Int = 1,
    val todayGp: Int = 0,
    val dailyLimit: Int = 100,
    val totalRecords: Int = 0,
    val latestRecord: Record? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as XiaoQueXingApp
    private val recordRepo = app.recordRepository
    private val plantRepo = app.plantRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val cal = Calendar.getInstance()
        val greeting = when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "早安"
            in 12..17 -> "午安"
            else -> "晚安"
        }
        val dateStr = java.text.SimpleDateFormat("M月d日 EEEE", java.util.Locale.CHINESE).format(cal.time)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                greeting = greeting,
                dateStr = dateStr
            )

            // Collect active plant
            plantRepo.getActivePlant().collect { plant ->
                val gp = plant?.totalGp ?: 0
                val stage = PlantStage.fromGp(gp)
                _uiState.value = _uiState.value.copy(
                    activePlant = plant,
                    currentGp = gp,
                    nextStageGp = if (stage == PlantStage.DIVINE) gp else stage.maxGp,
                    progressInStage = PlantStage.progressInStage(gp),
                    stageName = stage.displayName
                )
            }
        }

        viewModelScope.launch {
            recordRepo.getTodayGp().collect { todayGp ->
                _uiState.value = _uiState.value.copy(todayGp = todayGp)
            }
        }

        viewModelScope.launch {
            recordRepo.getTotalCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalRecords = count)
            }
        }

        viewModelScope.launch {
            recordRepo.getLatestRecord().collect { record ->
                _uiState.value = _uiState.value.copy(latestRecord = record)
            }
        }

        viewModelScope.launch {
            // streak 不像 GP 那样会变（一天一次），算一次就行。
            val streak = runCatching { recordRepo.calculateStreakDays() }.getOrDefault(0)
            _uiState.value = _uiState.value.copy(streakDays = streak)
        }
    }
}

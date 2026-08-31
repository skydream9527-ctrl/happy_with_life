package com.xiaoquexing.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.entity.PlantStage
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.Record
import com.xiaoquexing.app.data.repository.PlantRepository
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val greeting: String = "你好",
    val greetingEmoji: String = "🌱",
    val dateStr: String = "",
    val syncLabel: String = "记录先保存在本机",
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

class HomeViewModel(
    private val recordRepo: RecordRepository,
    private val plantRepo: PlantRepository,
    private val tokens: com.xiaoquexing.app.data.remote.TokenStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "早安"
            in 12..17 -> "午安"
            in 18..21 -> "傍晚"
            else -> "晚安"
        }
        val greetingEmoji = when (hour) {
            in 5..11 -> "☀️"
            in 12..17 -> "🌿"
            in 18..21 -> "🌅"
            else -> "🌙"
        }
        val dateStr = java.text.SimpleDateFormat("M月d日 EEEE", java.util.Locale.CHINESE).format(cal.time)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                greeting = greeting,
                greetingEmoji = greetingEmoji,
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

        viewModelScope.launch {
            tokens?.session?.collect { session ->
                _uiState.value = _uiState.value.copy(
                    syncLabel = if (session != null) "已登录 · 记录会同步到云端" else "未登录 · 记录先保存在本机",
                )
            }
        }
    }
}

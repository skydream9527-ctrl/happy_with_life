package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.XiaoQueXingApp
import com.xiaoquexing.app.data.entity.Achievement
import com.xiaoquexing.app.data.entity.PlantState
import com.xiaoquexing.app.data.entity.PlantType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val nickname: String = "小确幸用户",
    val totalRecords: Int = 0,
    val totalGp: Int = 0,
    val streakDays: Int = 1,
    val unlockedPlantCount: Int = 1,
    val unlockedAchievementCount: Int = 0
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as XiaoQueXingApp
    private val recordRepo = app.recordRepository
    private val plantRepo = app.plantRepository
    private val achievementRepo = app.achievementRepository

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _plants = MutableStateFlow<List<PlantState>>(emptyList())
    val plants: StateFlow<List<PlantState>> = _plants.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _activePlantType = MutableStateFlow(PlantType.TREE)
    val activePlantType: StateFlow<PlantType> = _activePlantType.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            recordRepo.getTotalCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalRecords = count)
            }
        }
        viewModelScope.launch {
            recordRepo.getTotalGp().collect { gp ->
                _uiState.value = _uiState.value.copy(totalGp = gp)
            }
        }
        viewModelScope.launch {
            plantRepo.getUnlockedCount().collect { count ->
                _uiState.value = _uiState.value.copy(unlockedPlantCount = count)
            }
        }
        viewModelScope.launch {
            achievementRepo.getUnlockedCount().collect { count ->
                _uiState.value = _uiState.value.copy(unlockedAchievementCount = count)
            }
        }
        viewModelScope.launch {
            plantRepo.getAllPlants().collect { plants ->
                _plants.value = plants
            }
        }
        viewModelScope.launch {
            achievementRepo.getAllAchievements().collect { achs ->
                _achievements.value = achs
            }
        }
        viewModelScope.launch {
            plantRepo.getActivePlant().collect { plant ->
                if (plant != null) _activePlantType.value = plant.plantType
            }
        }
    }

    fun selectPlant(type: PlantType) {
        viewModelScope.launch {
            plantRepo.setActivePlant(type)
            _activePlantType.value = type
        }
    }
}

package com.xiaoquexing.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoquexing.app.data.repository.RecordRepository
import com.xiaoquexing.app.util.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReviewUiState(
    val month: MonthReview? = null,
    val year: YearReview? = null,
    val reminderOn: Boolean = false,
    val message: String? = null,
)

class ReviewViewModel(
    private val app: Application,
    private val recordRepo: RecordRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ReviewUiState(reminderOn = ReminderScheduler.isEnabled(app)))
    val uiState: StateFlow<ReviewUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            recordRepo.getAllRecords().collect { records ->
                val now = Calendar.getInstance()
                _ui.value = _ui.value.copy(
                    month = ReviewAggregator.monthReview(records, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1),
                    year = ReviewAggregator.yearReview(records, now.get(Calendar.YEAR)),
                )
            }
        }
    }

    fun setReminder(on: Boolean) {
        ReminderScheduler.setEnabled(app, on)
        _ui.value = _ui.value.copy(
            reminderOn = on,
            message = if (on) "已开启每天 ${ReminderScheduler.HOUR}:00 提醒" else "已关闭提醒",
        )
    }
}

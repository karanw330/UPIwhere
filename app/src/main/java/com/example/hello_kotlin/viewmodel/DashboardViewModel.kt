package com.example.hello_kotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello_kotlin.data.db.AppDatabase
import com.example.hello_kotlin.data.db.dao.TagTotal
import com.example.hello_kotlin.data.db.dao.TransactionWithTag
import com.example.hello_kotlin.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        db.transactionDao(),
        db.tagDao(),
        db.autoTagRuleDao()
    )

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _activePeriod = MutableStateFlow("Month") // "Week", "Month", "Year"
    val activePeriod: StateFlow<String> = _activePeriod.asStateFlow()

    init {
        viewModelScope.launch {
            var hasAutoSwitched = false
            repository.getTotalCountFlow().collect { totalCount ->
                if (totalCount == 0) {
                    hasAutoSwitched = false
                } else if (!hasAutoSwitched) {
                    val currentMonth = _selectedMonth.value
                    val (start, end) = monthToTimestamps(currentMonth)
                    val currentMonthTransactions = repository.getDebitTransactionsForMonth(start, end).first()
                    
                    if (currentMonthTransactions.isEmpty()) {
                        val latestTs = repository.getLatestTransactionTimestamp()
                        if (latestTs != null) {
                            val zone = ZoneId.systemDefault()
                            val latestMonth = YearMonth.from(Instant.ofEpochMilli(latestTs).atZone(zone).toLocalDate())
                            _selectedMonth.value = latestMonth
                            hasAutoSwitched = true
                        }
                    } else {
                        hasAutoSwitched = true
                    }
                }
            }
        }
    }

    // Reactive timeRange based on selectedMonth and activePeriod
    val timeRange: StateFlow<Pair<Long, Long>> = combine(_selectedMonth, _activePeriod) { month, period ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()
        val isCurrentMonth = month == YearMonth.from(now)
        
        when (period) {
            "Week" -> {
                val endDate = if (isCurrentMonth) now else month.atEndOfMonth()
                val startDate = endDate.minusDays(6)
                val startTs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val endTs = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                Pair(startTs, endTs)
            }
            "Year" -> {
                val startOfYear = LocalDate.of(month.year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
                val startOfNextYear = LocalDate.of(month.year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
                Pair(startOfYear, startOfNextYear)
            }
            else -> { // "Month"
                val startOfMonth = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val startOfNextMonth = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                Pair(startOfMonth, startOfNextMonth)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0L, 0L))

    // Reactive: recalculates whenever timeRange changes
    val totalExpenditure: StateFlow<Double> = timeRange
        .flatMapLatest { (start, end) ->
            repository.getTotalExpenditure(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = timeRange
        .flatMapLatest { (start, end) ->
            repository.getTotalIncome(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val topExpenditure: StateFlow<TransactionWithTag?> = timeRange
        .flatMapLatest { (start, end) ->
            repository.getTopExpenditure(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tagBreakdown: StateFlow<List<TagTotal>> = timeRange
        .flatMapLatest { (start, end) ->
            repository.getMonthlyTotalByTag(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionWithTag>> = timeRange
        .flatMapLatest { (start, end) ->
            repository.getDebitTransactionsForMonth(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val untaggedCount: StateFlow<Int> = repository.getUntaggedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun selectPeriod(period: String) {
        _activePeriod.value = period
    }

    fun goToPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    val dailySpending: StateFlow<List<DailySpent>> = combine(_selectedMonth, _activePeriod) { month, period ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()
        val isCurrentMonth = month == YearMonth.from(now)

        if (period == "Week") {
            val endDate = if (isCurrentMonth) now else month.atEndOfMonth()
            val startDate = endDate.minusDays(6)
            
            val startTs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endTs = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            
            val txList = repository.getDebitTransactionsForMonth(startTs, endTs).first()
            val dayMap = txList.groupBy { tx ->
                Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
            }.mapValues { entry ->
                entry.value.sumOf { it.amount }
            }
            
            (0..6).map { i ->
                val date = startDate.plusDays(i.toLong())
                DailySpent(date.dayOfMonth, dayMap[date] ?: 0.0, date)
            }
        } else { // "Month" or fallback
            val startOfMonth = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val startOfNextMonth = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            
            val txList = repository.getDebitTransactionsForMonth(startOfMonth, startOfNextMonth).first()
            val dayMap = txList.groupBy { tx ->
                Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate().dayOfMonth
            }.mapValues { entry ->
                entry.value.sumOf { it.amount }
            }
            
            val daysInMonth = month.lengthOfMonth()
            (1..daysInMonth).map { day ->
                val date = month.atDay(day)
                DailySpent(day, dayMap[day] ?: 0.0, date)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlySpending: StateFlow<List<Double>> = _selectedMonth
        .flatMapLatest { month ->
            val zone = ZoneId.systemDefault()
            val startOfYear = LocalDate.of(month.year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val startOfNextYear = LocalDate.of(month.year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            
            repository.getDebitTransactionsForMonth(startOfYear, startOfNextYear)
                .map { txList ->
                    val monthMap = txList.groupBy { tx ->
                        Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate().monthValue
                    }.mapValues { entry ->
                        entry.value.sumOf { it.amount }
                    }
                    (1..12).map { m ->
                        monthMap[m] ?: 0.0
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(12) { 0.0 })

    val accountSpending: StateFlow<List<AccountSpent>> = recentTransactions
        .map { txList ->
            txList.groupBy { tx ->
                tx.accountName ?: tx.senderAddress
            }.map { (name, txs) ->
                AccountSpent(
                    name = name,
                    amount = txs.sumOf { it.amount },
                    count = txs.size
                )
            }.sortedByDescending { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun monthToTimestamps(month: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startOfMonth = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextMonth = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return Pair(startOfMonth, startOfNextMonth)
    }
}

data class DailySpent(val day: Int, val totalAmount: Double, val date: LocalDate? = null)
data class AccountSpent(val name: String, val amount: Double, val count: Int)

package com.example.hello_kotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello_kotlin.data.db.AppDatabase
import com.example.hello_kotlin.data.db.dao.TransactionWithTag
import com.example.hello_kotlin.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        db.transactionDao(),
        db.tagDao(),
        db.autoTagRuleDao()
    )

    val untaggedTransactions: StateFlow<List<TransactionWithTag>> = repository.getUntaggedTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tagTransaction(transactionId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.tagTransaction(transactionId, tagId)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }
}

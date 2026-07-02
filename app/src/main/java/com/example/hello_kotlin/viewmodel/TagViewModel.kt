package com.example.hello_kotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello_kotlin.data.db.AppDatabase
import com.example.hello_kotlin.data.db.entity.TagEntity
import com.example.hello_kotlin.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        db.transactionDao(),
        db.tagDao(),
        db.autoTagRuleDao()
    )

    val allTags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTag(name: String, colorHex: String, emoji: String? = null) {
        viewModelScope.launch {
            repository.createTag(name, colorHex, emoji)
        }
    }

    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch {
            repository.deleteTag(id)
        }
    }
}

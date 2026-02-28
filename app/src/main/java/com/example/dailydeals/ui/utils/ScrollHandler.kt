package com.example.dailydeals.ui.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.runtime.compositionLocalOf

class ScrollHandler {
    private val _scrollToTop = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val scrollToTop: SharedFlow<Int> = _scrollToTop

    fun requestScrollToTop(tabIndex: Int) {
        _scrollToTop.tryEmit(tabIndex)
    }
}

val LocalScrollHandler = compositionLocalOf { ScrollHandler() }

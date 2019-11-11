package com.softartdev.photofilters.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FilterViewModelFactory(
    private val imageUri: Uri,
    private val applicationContext: Context
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.cast(FilterViewModel(imageUri, applicationContext))
            ?: super.create(modelClass)
    }
}
package com.example.learndi

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GreetingViewModel @Inject constructor (private val repository: GreetingRepository) : ViewModel() {
    fun getGreetingService(name: String): String {
        //return "Hello $name"
        return repository.getGreetingService(name)
    }
    fun squareit(number: Int): Int {
        return repository.squareit(number)
    }
}
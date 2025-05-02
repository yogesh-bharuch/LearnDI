package com.example.learndi

import javax.inject.Inject

class GreetingRepository @Inject constructor (private val service: GreetingService) {
    fun getGreetingService(name: String): String {
        //return "Hello $name"
        return service.greet(name)
    }
    fun squareit(number: Int): Int{
        return service.squareit(number)
    }
}
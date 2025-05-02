package com.example.learndi

import javax.inject.Inject

class GreetingService @Inject constructor() {
    fun greet(name: String): String{
        return "Hello $name"
    }
    fun squareit(number: Int) : Int {
        return number*number
    }
}
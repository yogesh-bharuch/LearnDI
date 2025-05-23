package com.example.learndi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MyAppScreen(modifier: Modifier = Modifier, viewmodel: GreetingViewModel) {

    val name = viewmodel.getGreetingService("Yogesh J. Vyas")
    val squareof = viewmodel.squareit(25)
    //Box(modifier= modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column (modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            modifier = modifier
        )
        Spacer(modifier = modifier.padding(16.dp))
        Text(text = squareof.toString(), modifier = modifier)
    }
}

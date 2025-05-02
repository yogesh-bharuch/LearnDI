package com.example.learndi.firestore


import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext


@Composable
fun SyncTestButton() {
    val context = LocalContext.current
    Button(onClick = { triggerFirestoreSync(context) }) {
        Text("Trigger Sync Now")
    }
}
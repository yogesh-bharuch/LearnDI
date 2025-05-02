package com.example.learndi.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Detail : Screen("detail", "Detail", Icons.Default.Info)
    object TaskApp : Screen("taskApp", "TaskApp", Icons.AutoMirrored.Filled.List)
    object TaskEntryScreen : Screen("taskEntryScreen", "TaskEntryScreen", Icons.Default.Done)
}

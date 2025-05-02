package com.example.learndi.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.learndi.DetailScreen
import com.example.learndi.HomeScreen
import com.example.learndi.TaskApp
import com.example.learndi.TaskEntryScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onNavigateToDetail = {
                navController.navigate(Screen.Detail.route)
            })
        }
        composable(Screen.Detail.route) {
            DetailScreen()
        }
        composable(Screen.TaskEntryScreen.route) {
            TaskEntryScreen()
        }

        composable(Screen.TaskApp.route) {
            TaskApp(searchQuery = searchQuery)
        }
    }
}
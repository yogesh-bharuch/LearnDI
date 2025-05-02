package com.example.learndi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.learndi.sync.WorkManagerHelper
import com.example.learndi.ui.theme.LearnDITheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedPreferences = getSharedPreferences("learnDi_prefs", MODE_PRIVATE)
        val lastSyncTime = sharedPreferences.getLong("last_sync_time", 0L)
        // ✅ Get current user ID from Firebase Authentication
        val currentUserId = "Yogesh" //FirebaseAuth.getInstance().currentUser?.uid ?: "Unknown"
        WorkManagerHelper.chainSyncOnStartup(context = applicationContext, lastSyncTime = lastSyncTime, currentUserId = currentUserId)
        Log.d("FirestoreSync", "From: MainACtivity after.chainSyncOnStartup ")
        //WorkManagerHelper.chainSyncOnStartup(context = applicationContext)
        setContent {
            val context = this@MainActivity
            LearnDITheme {
                RequestStoragePermission(context)
                WorkManagerHelper.schedulePeriodicSync(context, timeIntervalInMinutes = 15, lastSyncTime = lastSyncTime, currentUserId = currentUserId)
                val navController = rememberNavController()
                MainScaffold(navController = navController)
            }
        }
    }
}
@Composable
fun RequestStoragePermission(context: Context) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied. Cannot access media.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }
}
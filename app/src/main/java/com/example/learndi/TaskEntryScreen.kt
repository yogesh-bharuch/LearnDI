package com.example.learndi

import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEntryScreen(
    viewModel: TaskViewModel = hiltViewModel(), // <-- Pass your ViewModel
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // States for fields
    var textTitle by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var isAlive by remember { mutableStateOf(true) }
    var childNumber by remember { mutableStateOf(1) }
    var selectedUri by remember { mutableStateOf<String?>(null) }

    var showError by remember { mutableStateOf(false) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri?.toString()
    }
    // Scroll state
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        OutlinedTextField(
            value = textTitle,
            onValueChange = { textTitle = it },
            label = { Text("Title") },
            isError = showError && (textTitle.length < 3)
        )

        // First Name
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            isError = showError && (firstName.length < 3)
        )

        // Middle Name
        OutlinedTextField(
            value = middleName,
            onValueChange = { middleName = it },
            label = { Text("Middle Name") },
            isError = showError && (middleName.length < 3)
        )

        // Last Name
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            isError = showError && (lastName.length < 3)
        )

        // Town
        OutlinedTextField(
            value = town,
            onValueChange = { town = it },
            label = { Text("Town") }
        )

        // Is Alive
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Is Alive?")
            Switch(
                checked = isAlive,
                onCheckedChange = { isAlive = it }
            )
        }

        // Child Number
        OutlinedTextField(
            value = childNumber.toString(),
            onValueChange = {
                childNumber = it.toIntOrNull() ?: 1
            },
            label = { Text("Child Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Select Image
        Button(onClick = {
            imagePickerLauncher.launch("image/*")
        }) {
            Text("Select Image")
        }

        // Show selected image preview
        selectedUri?.let {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(it)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        // Save IconButton

        // Save Button
        Button(
            onClick = {
                if (textTitle.isNotBlank() &&
                    firstName.length >= 3 &&
                    middleName.length >= 3 &&
                    lastName.length >= 3
                ) {
                    val uuid = UUID.randomUUID().toString()
                    val newId = "$textTitle$uuid"

                    // Create a new Task object
                    val task = Task(
                        id = newId,
                        syncStatus = SyncStatus.NOT_SYNCED,
                        title = textTitle,
                        firstName = firstName,
                        middleName = middleName,
                        lastName = lastName,
                        town = town,
                        isAlive = isAlive,
                        childNumber = childNumber,
                        imageUri = selectedUri,
                        createdBy = "user_id",  // Replace with actual user ID if needed
                        parentID = null,         // Set based on your logic
                        spouseID = null,         // Set based on your logic
                        updatedAt = System.currentTimeMillis()
                    )

                    // Convert Task to map and pass it to addTask
                    val taskData = task.toMap()

                    // Call the ViewModel's addTask with the map
                    viewModel.addTask(taskData, task)

                    // Clear fields after saving
                    textTitle = ""
                    firstName = ""
                    middleName = ""
                    lastName = ""
                    town = ""
                    isAlive = true
                    childNumber = 1
                    selectedUri = ""
                    showError = false
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Save") // Button shows "Save" text
        }
    }
}

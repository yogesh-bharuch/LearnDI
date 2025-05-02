package com.example.learndi

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.learndi.firestore.SyncTestButton
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(
    viewModel: TaskViewModel = hiltViewModel(),
    searchQuery: String = ""
) {
    val tasks by viewModel.tasks.collectAsState()
    var textTitle by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<String?>(null) }
    //var selectedUri = ""
    var overlayImageUri by remember { mutableStateOf<String?>(null) } // State for full-screen image

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.toString()?.let {
            selectedUri = it
        }
    }
    // **Filter tasks based on searchQuery**
    val filteredTasks = tasks.filter { task ->
        task.title.contains(searchQuery.trim(), ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Row {
            TextField(
                value = textTitle,
                onValueChange = { textTitle = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter task") }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                IconButton(onClick = {
                    if (textTitle.isNotBlank()) {
                        val uri1 = selectedUri?.let { Uri.parse(it) } ?: Uri.EMPTY
                        //val uri1: Uri = Uri.parse(selectedUri)
                        val uuid = UUID.randomUUID().toString()
                        //viewModel.addTask(textTitle, selectedUri ?: "", uri1, uuid)
                        //viewModel.addTask(textTitle, selectedUri, uri1, uuid)
                        textTitle = ""
                        selectedUri = ""
                    }
                }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add")
                }
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = "Change")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(filteredTasks) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { viewModel.toggleDone(task) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { viewModel.toggleDone(task) }
                            )
                            Text(task.title, modifier = Modifier.weight(1f))

                            IconButton(onClick = { viewModel.delete(task) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            task.imageUri?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Task Image",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray)
                                        //.padding(horizontal = 8.dp)
                                        .clickable { overlayImageUri = uri; Log.d("TaskApp", "Overlay Image URI: $overlayImageUri") }, // Show overlay on image click
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                text = task.firstName ?: "No Name",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }


    }

    // Full-screen image overlay
    overlayImageUri?.let { uri ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8F))
                .clickable { overlayImageUri = null } // Close overlay on click
                .zIndex(1f),
            contentAlignment = Alignment.Center // Center the image within the overlay
        ) {
            Box(
                modifier = Modifier
                    .size(400.dp) // Restrict size to 200x200
                    .clip(RoundedCornerShape(8.dp)) // Optional: Add rounded corners
                    .background(Color.White) // Optional: Add a background for contrast
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Task Image",
                    modifier = Modifier.fillMaxSize(), // Fill the 200x200 box
                    contentScale = ContentScale.Crop // Crop the image to fit inside the box
                )
            }
        }
    }
}
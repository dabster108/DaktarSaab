package com.example.daktarsaab.view

import android.Manifest
import android.app.Activity // Required for ActivityResultLauncher
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.daktarsaab.R
import com.example.daktarsaab.ReminderBroadcastReceiver
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import java.util.*
import android.speech.RecognizerIntent
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

class ReminderActivity : ComponentActivity() {
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("Notification permission granted!")
        } else {
            println("Notification permission denied.")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ResourcesCompat.getColor(resources, R.color.black, theme).let { color ->
            window.statusBarColor = color
        }

        val userName = intent.getStringExtra("USER_NAME")
        val profileImageUrl = intent.getStringExtra("PROFILE_IMAGE_URL")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            DaktarSaabTheme {
                ReminderActivityContent(userName = userName, profileImageUrl = profileImageUrl)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderActivityContent(userName: String?, profileImageUrl: String?) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            TopAppBar(
                title = { },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (profileImageUrl != null) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(8.dp)
                            )
                        }
                        Text(
                            text = (userName?.split(" ")?.first() ?: "User"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            ReminderScreen()
        }
    }
}

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var date: String,
    var time: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReminderList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val reminders: SnapshotStateList<Reminder> = mutableStateListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen() {
    val showNewReminderInput = remember { mutableStateOf(false) }
    val searchText = remember { mutableStateOf("") }
    val isGlobalEditMode = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val myLists = remember {
        mutableStateListOf(
            ReminderList(name = "Reminders") // Explicitly set name to "Reminders"
        )
    }
    val selectedList = remember { mutableStateOf(myLists.first()) }
    val isSelectedListExpanded = remember { mutableStateOf(true) }
    val showNewListDialog = remember { mutableStateOf(false) }
    val newListName = remember { mutableStateOf("") }

    // Derived state for displayed lists based on search text
    val displayedLists = remember(searchText.value, myLists.toList(), myLists.flatMap { it.reminders.toList().map { r -> r.id + r.text } }, myLists.map { it.name }) {
        if (searchText.value.isBlank()) {
            myLists
        } else {
            myLists.filter { list ->
                list.name.contains(searchText.value, ignoreCase = true) ||
                        list.reminders.any { reminder ->
                            reminder.text.contains(searchText.value, ignoreCase = true)
                        }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile Icon",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Dikshanta", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.LightGray.copy(alpha = 0.5f),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Gray
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // My Lists Header with Add List button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "My Lists", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = "Add List",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { showNewListDialog.value = true }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lists and Reminders
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(displayedLists, key = { it.id }) { reminderList ->
                ListItemCard(
                    title = reminderList.name,
                    bgColor = Color(0xFFFF9500),
                    icon = Icons.AutoMirrored.Filled.List,
                    showArrow = true,
                    isSelected = reminderList.id == selectedList.value.id,
                    onCardClick = {
                        if (selectedList.value.id == reminderList.id) {
                            isSelectedListExpanded.value = !isSelectedListExpanded.value
                        } else {
                            selectedList.value = reminderList
                            isSelectedListExpanded.value = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show reminders based on selection or search
                val showRemindersBasedOnSelection = selectedList.value.id == reminderList.id && isSelectedListExpanded.value
                val listContainsMatchingRemindersForSearch = reminderList.reminders.any { it.text.contains(searchText.value, ignoreCase = true) }
                val showRemindersBasedOnSearch = searchText.value.isNotBlank() && listContainsMatchingRemindersForSearch

                if (showRemindersBasedOnSelection || showRemindersBasedOnSearch) {
                    val currentRemindersToShow = if (searchText.value.isBlank()) {
                        reminderList.reminders
                    } else {
                        reminderList.reminders.filter { it.text.contains(searchText.value, ignoreCase = true) }
                    }

                    if (currentRemindersToShow.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            val message = if (searchText.value.isNotBlank()) {
                                "No matching reminders in this list."
                            } else {
                                "No reminders in this list."
                            }
                            Text(text = message, color = Color.Gray)
                        }
                    } else {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 0.dp)) {
                            currentRemindersToShow.forEach { reminder ->
                                ReminderItem(
                                    reminder = reminder,
                                    isEditingGlobal = isGlobalEditMode.value,
                                    onUpdate = { updatedReminder ->
                                        val sourceList = myLists.find { it.id == reminderList.id }
                                        sourceList?.let {
                                            val index = it.reminders.indexOfFirst { r -> r.id == updatedReminder.id }
                                            if (index != -1) {
                                                it.reminders[index] = updatedReminder
                                                scheduleReminder(context, updatedReminder)
                                            }
                                        }
                                    },
                                    onRemove = { reminderToRemove ->
                                        val sourceList = myLists.find { it.id == reminderList.id }
                                        sourceList?.reminders?.remove(reminderToRemove)
                                        cancelReminder(context, reminderToRemove)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Controls for the selected and expanded list
                    if (selectedList.value.id == reminderList.id && isSelectedListExpanded.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!isGlobalEditMode.value && !showNewReminderInput.value) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        showNewReminderInput.value = true
                                    }
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "New Reminder", tint = Color(0xFF007AFF))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Reminder", color = Color(0xFF007AFF))
                                }
                            } else {
                                Box {}
                            }
                            Text(
                                text = if (isGlobalEditMode.value) "Done" else "Edit",
                                color = Color(0xFF007AFF),
                                modifier = Modifier
                                    .clickable {
                                        isGlobalEditMode.value = !isGlobalEditMode.value
                                        if (isGlobalEditMode.value) {
                                            showNewReminderInput.value = false
                                        }
                                    }
                                    .padding(end = 16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // New Reminder Input Section
        if (showNewReminderInput.value) {
            NewReminderInput(
                onReminderAdded = { newReminder ->
                    selectedList.value.reminders.add(newReminder)
                    scheduleReminder(context, newReminder)
                    showNewReminderInput.value = false
                },
                onCancel = {
                    showNewReminderInput.value = false
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add New List Dialog
        if (showNewListDialog.value) {
            AlertDialog(
                onDismissRequest = { showNewListDialog.value = false },
                title = { Text("Create New List") },
                text = {
                    OutlinedTextField(
                        value = newListName.value,
                        onValueChange = { newListName.value = it },
                        label = { Text("List Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = myLists.any { it.name.equals(newListName.value.trim(), ignoreCase = true) },
                        supportingText = {
                            if (myLists.any { it.name.equals(newListName.value.trim(), ignoreCase = true) }) {
                                Text("List name already exists", color = Color.Red)
                            }
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val requestedName = newListName.value.trim()
                            if (requestedName.isNotBlank() && myLists.none { it.name.equals(requestedName, ignoreCase = true) }) {
                                val newList = ReminderList(name = requestedName)
                                myLists.add(newList)
                                newListName.value = ""
                                showNewListDialog.value = false
                                selectedList.value = newList
                                showNewReminderInput.value = false
                                isSelectedListExpanded.value = true
                            }
                        },
                        enabled = newListName.value.trim().isNotBlank() && myLists.none { it.name.equals(newListName.value.trim(), ignoreCase = true) }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showNewListDialog.value = false
                        newListName.value = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun scheduleReminder(context: Context, reminder: Reminder) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
        putExtra(ReminderBroadcastReceiver.Companion.EXTRA_REMINDER_TEXT, reminder.text)
        putExtra(ReminderBroadcastReceiver.Companion.EXTRA_REMINDER_ID, reminder.id)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timestamp, pendingIntent)
                println("Reminder scheduled for: ${reminder.text} at ${Date(reminder.timestamp)}")
            } catch (e: SecurityException) {
                println("SecurityException: ${e.message}")
            }
        } else {
            println("Cannot schedule exact alarms.")
        }
    } else {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timestamp, pendingIntent)
            println("Reminder scheduled for (legacy): ${reminder.text} at ${Date(reminder.timestamp)}")
        } catch (e: SecurityException) {
            println("SecurityException on legacy Android: ${e.message}")
        }
    }
}

private fun cancelReminder(context: Context, reminder: Reminder) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderBroadcastReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
    println("Reminder cancelled for: ${reminder.text}")
}

@Composable
fun ListItemCard(
    title: String,
    bgColor: Color,
    icon: ImageVector,
    showArrow: Boolean = false,
    isSelected: Boolean,
    onCardClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF007AFF) else Color(0xFFD6EAF8) // Blue for selected, light blue for unselected
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(bgColor, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                )
            }
            if (showArrow) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Expand list",
                    tint = if (isSelected) Color.White else Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReminderInput(onReminderAdded: (Reminder) -> Unit, onCancel: () -> Unit) {
    val reminderText = remember { mutableStateOf("") }
    val selectedDate = remember { mutableStateOf("") }
    val selectedTime = remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    // Speech Recognizer Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val spokenText: ArrayList<String>? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!spokenText.isNullOrEmpty()) {
                    reminderText.value = spokenText[0]
                }
            }
        }
    )

    // Permission Launcher for RECORD_AUDIO
    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, launch speech recognizer
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your reminder")
                }
                try {
                    speechRecognizerLauncher.launch(intent)
                } catch (e: Exception) {
                    // Handle exception if no speech recognizer is available
                    println("Speech recognizer not available: ${e.message}")
                    // You might want to show a Toast to the user here
                }
            } else {
                // Permission denied
                println("Audio permission denied")
                // Optionally, inform the user why the permission is needed
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = reminderText.value,
            onValueChange = { reminderText.value = it },
            label = { Text("New Reminder") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            trailingIcon = { // Add microphone icon here
                IconButton(onClick = {
                    // Check and request RECORD_AUDIO permission
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            // Permission is already granted, launch speech recognizer
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your reminder")
                            }
                            try {
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                println("Speech recognizer not available: ${e.message}")
                            }
                        }
                        else -> {
                            // Permission has not been granted, request it
                            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_mic_24),
                        contentDescription = "Speak Reminder"
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate.value = "${month + 1}/${dayOfMonth}/$year"
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }) {
                Text(if (selectedDate.value.isEmpty()) "Select Date" else selectedDate.value)
            }
            Button(onClick = {
                TimePickerDialog(context, { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    selectedTime.value = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
            }) {
                Text(if (selectedTime.value.isEmpty()) "Select Time" else selectedTime.value)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (reminderText.value.isNotBlank() && selectedDate.value.isNotBlank() && selectedTime.value.isNotBlank()) {
                        val newReminder = Reminder(
                            text = reminderText.value,
                            date = selectedDate.value,
                            time = selectedTime.value,
                            timestamp = calendar.timeInMillis
                        )
                        onReminderAdded(newReminder)
                        reminderText.value = ""
                        selectedDate.value = ""
                        selectedTime.value = ""
                    }
                }
            ) {
                Text("Add")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderItem(reminder: Reminder, isEditingGlobal: Boolean, onUpdate: (Reminder) -> Unit, onRemove: (Reminder) -> Unit) {
    val isEditingThisItem = remember { mutableStateOf(false) }
    val editText = remember { mutableStateOf(reminder.text) }
    val editDate = remember { mutableStateOf(reminder.date) }
    val editTime = remember { mutableStateOf(reminder.time) }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = reminder.timestamp } }

    DisposableEffect(isEditingGlobal) {
        if (!isEditingGlobal) {
            isEditingThisItem.value = false
        }
        onDispose { }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEditingGlobal && !isEditingThisItem.value) { isEditingThisItem.value = true },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (isEditingThisItem.value) {
                OutlinedTextField(
                    value = editText.value,
                    onValueChange = { editText.value = it },
                    label = { Text("Reminder Text") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            calendar.set(year, month, dayOfMonth)
                            editDate.value = "${month + 1}/${dayOfMonth}/$year"
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) {
                        Text(editDate.value)
                    }
                    Button(onClick = {
                        TimePickerDialog(context, { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            editTime.value = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                    }) {
                        Text(editTime.value)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditingThisItem.value = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val updatedReminder = reminder.copy(
                            text = editText.value,
                            date = editDate.value,
                            time = editTime.value,
                            timestamp = calendar.timeInMillis
                        )
                        onUpdate(updatedReminder)
                        isEditingThisItem.value = false
                    }) {
                        Text("Update")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = reminder.text, fontWeight = FontWeight.Medium)
                        Text(text = "${reminder.date} at ${reminder.time}", fontSize = 12.sp, color = Color.Gray)
                    }
                    if (isEditingGlobal) {
                        IconButton(onClick = { onRemove(reminder) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Reminder", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
package com.example.userprofile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.userprofile.ui.theme.UserProfileTheme

class UserProfile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UserProfileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UserProfileScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun UserProfileScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(id = R.drawable.outline_person_24),
            contentDescription = "Profile Icon",
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        EditableInfoBox(label = "Username")
        Spacer(modifier = Modifier.height(12.dp))
        EditableInfoBox(label = "Email")
        Spacer(modifier = Modifier.height(12.dp))
        EditableInfoBox(label = "Blood Group")
        Spacer(modifier = Modifier.height(12.dp))
        EditableInfoBox(label = "Phone Number")
        Spacer(modifier = Modifier.height(32.dp))
        var expanded by remember { mutableStateOf(false) }
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(if (expanded) 160.dp else 60.dp)
                .clickable { expanded = !expanded }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Reports Saved:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Text(text = "Report 1", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Report 2", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Report 3", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!expanded) {
                    Text(text = "Report 1", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ProfileInfoBox(label = "Appointment Booking", value = " ")
        Spacer(modifier = Modifier.height(12.dp))
        ProfileInfoBox(label = "Reminder", value = " ")
    }
}

@Composable
fun EditableInfoBox(label: String) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth(0.7f),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Box(modifier = Modifier.padding(2.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = { Text(text = "Enter $label", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun ProfileInfoBox(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth(0.85f)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(text = "$label: $value", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfilePreview() {
    UserProfileTheme {
        UserProfileScreen()
    }
}


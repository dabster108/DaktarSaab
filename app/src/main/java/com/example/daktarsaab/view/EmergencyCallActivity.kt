package com.example.daktarsaab.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daktarsaab.R // Make sure this R points to your project's resources
import com.example.daktarsaab.ui.theme.DaktarSaabTheme // Make sure this imports your theme

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class for general emergency numbers with optional image and phone number
data class EmergencyItem(val text: String, val imageRes: Int? = null, val phoneNumber: String? = null)

// Data class for hospital contact information
data class HospitalContact(val hospitalName: String, val phoneNumber: String)

// Data class for pager content details, used in the horizontal pager
data class PagerContentDetail(val title: String, val content: String)

/**
 * Main Activity for the Emergency Call Screen.
 * This activity sets up the Compose UI for displaying emergency contacts and health information.
 */
class EmergencyCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge display for a more immersive experience
        setContent {
            // Move the state and theme logic inside the Composable scope
            val isDarkTheme = isSystemInDarkTheme()
            var darkMode by rememberSaveable { mutableStateOf(isDarkTheme) }

            DaktarSaabTheme(darkTheme = darkMode) { // Applies the custom theme defined in DaktarSaabTheme
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // The main composable screen, passing padding from the Scaffold
                    EmergencyCallScreen(
                        modifier = Modifier.padding(innerPadding),
                        darkMode = darkMode,
                        onToggleDarkMode = { darkMode = !darkMode }
                    )
                }
            }
        }
    }
}

/**
 * The main Composable function for the Emergency Call Screen.
 * It displays emergency numbers, hospital contacts, and a horizontal content pager.
 *
 * @param modifier Modifier to be applied to the root Column.
 * @param darkMode Boolean indicating if dark mode is enabled
 * @param onToggleDarkMode Lambda to toggle dark mode state
 */
@OptIn(ExperimentalFoundationApi::class) // Opt-in for ExperimentalFoundationApi for HorizontalPager
@Composable
fun EmergencyCallScreen(
    modifier: Modifier = Modifier,
    darkMode: Boolean = isSystemInDarkTheme(),
    onToggleDarkMode: () -> Unit = {}
) {
    // Raw data for emergency numbers and hospital departments
    val rawEmergencyData = remember {
        listOf(
            "Emergency Call Numbers" to listOf(
                EmergencyItem("Ambulance: 102", R.drawable.ambulance, "102"),
                EmergencyItem("Police: 100", R.drawable.police, "100"),
                EmergencyItem("Fire: 101", R.drawable.fire, "101"),
                EmergencyItem("Disaster Management: 1149", R.drawable.disastermanagement, "1149")
            ),
            "Cardiology" to listOf(
                HospitalContact("Shahid Gangalal National Heart Centre", "01-4371322"),
                HospitalContact("Norvic International Hospital", "01-5970032"),
                HospitalContact("Medicity Hospital", "9801907777"),
                HospitalContact("Grande International Hospital", "01-5159266")
            ),
            "Neurology" to listOf(
                HospitalContact("Neuro Hospital Bansbari", "01-4378914"),
                HospitalContact("National Institute of Neurological and Allied Sciences", "01-4387807"),
                HospitalContact("Grande International Hospital", "01-5159266"),
                HospitalContact("Medicity Hospital", "9801907777")
            ),
            "Pediatrics" to listOf(
                HospitalContact("Kanti Children\'s Hospital", "01-4411550"),
                HospitalContact("Thapathali Maternity Hospital", "01-4261775"),
                HospitalContact("Star Hospital", "01-5212574"),
                HospitalContact("Nepal Medical College Teaching Hospital", "01-4493394")
            ),
            "Orthopedics" to listOf(
                HospitalContact("National Trauma Center", "01-4262696"),
                HospitalContact("Civil Service Hospital", "01-4217112"),
                HospitalContact("Grande International Hospital", "01-5159266"),
                HospitalContact("Nepal Orthopaedic Hospital", "01-4911043")
            )
        )
    }

    // Separates emergency numbers from hospital data
    val emergencyNumbersData = remember(rawEmergencyData) { rawEmergencyData.first { it.first == "Emergency Call Numbers" } }
    val hospitalDataList = remember(rawEmergencyData) { rawEmergencyData.filter { it.first != "Emergency Call Numbers" } }

    // Processes hospital data to include a title image resource for each department
    val processedHospitalData = remember(hospitalDataList) {
        hospitalDataList.map { (title, dataList) ->
            val titleImage = when (title) {
                "Cardiology" -> R.drawable.cardiology
                "Neurology" -> R.drawable.neurology
                "Pediatrics" -> R.drawable.pediatrics
                "Orthopedics" -> R.drawable.orthopedics
                else -> null
            }
            Triple(title, dataList, titleImage)
        }
    }

    // MutableTransitionStates for animated visibility of different UI sections
    val emergencyVisibleState = remember { MutableTransitionState(false) }
    val hospitalVisibleStates = remember {
        processedHospitalData.map { MutableTransitionState(false) }
    }
    val pagerVisibleState = remember { MutableTransitionState(false) }

    // State for managing the detail box visibility and content
    var showDetailBox by remember { mutableStateOf(false) }
    var detailBoxContent by remember { mutableStateOf(PagerContentDetail("", "")) }
    val detailBoxVisibilityState = remember { MutableTransitionState(false) }

    // State for managing the call confirmation dialog
    var showCallConfirmationDialog by remember { mutableStateOf(false) }
    var phoneNumberToCall by remember { mutableStateOf<String?>(null) } // Store the number to call

    // LaunchedEffect to control the staggered animations on screen load
    LaunchedEffect(Unit) {
        delay(200) // Small delay before first animation
        emergencyVisibleState.targetState = true // Show emergency section
        delay(300) // Delay before hospital sections
        if (hospitalVisibleStates.isNotEmpty()) {
            hospitalVisibleStates[0].targetState = true // Show first hospital section
            if (hospitalVisibleStates.size > 1) {
                delay(150)
                hospitalVisibleStates[1].targetState = true // Show second hospital section
            }
            if (hospitalVisibleStates.size > 2) {
                delay(150)
                hospitalVisibleStates[2].targetState = true // Show third hospital section
            }
            if (hospitalVisibleStates.size > 3) {
                delay(150)
                hospitalVisibleStates[3].targetState = true // Show fourth hospital section
            }
        }
        delay(150)
        pagerVisibleState.targetState = true // Show the horizontal pager
    }

    Column(modifier = modifier.fillMaxSize()) {
        // User profile section at the top right with dark mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dark/Light mode toggle
            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (darkMode) R.drawable.baseline_light_mode_24
                             else R.drawable.baseline_dark_mode_24
                    ),
                    contentDescription = if (darkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // User profile icon and name
            Icon(
                painter = painterResource(id = R.drawable.baseline_person_24),
                contentDescription = "User Profile",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Dikshanta",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Add extra space before the emergency content
        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn to scroll through all the content
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .weight(1f) // Makes LazyColumn fill remaining height
        ) {
            item {
                // Animated visibility for the static emergency section
                AnimatedVisibility(
                    visibleState = emergencyVisibleState,
                    enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(), // Slide in from top and fade in
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StaticEmergencySection(
                        title = emergencyNumbersData.first,
                        items = emergencyNumbersData.second as? List<EmergencyItem> ?: emptyList(), // Safe cast to EmergencyItem list
                        onCallRequested = { number ->
                            phoneNumberToCall = number
                            showCallConfirmationDialog = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(processedHospitalData.size) { index ->
                val (title, dataList, titleImageRes) = processedHospitalData[index]
                val visibleState = hospitalVisibleStates[index]
                // Define different entry animations based on index for visual variety
                val enterAnimation = when (index % 4) {
                    0, 2 -> slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn() // Slide in from right
                    1, 3 -> slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn() // Slide in from left
                    else -> fadeIn() // Default fade in
                }
                // Animated visibility for each expandable hospital section
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = enterAnimation
                ) {
                    ExpandableBox(
                        title = title,
                        dataList = dataList,
                        titleImageRes = titleImageRes,
                        onCallRequested = { number ->
                            phoneNumberToCall = number
                            showCallConfirmationDialog = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Animated visibility for the horizontal content pager
                AnimatedVisibility(
                    visibleState = pagerVisibleState,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() // Slide in from bottom and fade in
                ) {
                    HorizontalContentPager(
                        onCardClick = { title, content ->
                            // Update content and show detail box when a pager card is clicked
                            detailBoxContent = PagerContentDetail(title, content)
                            showDetailBox = true
                            detailBoxVisibilityState.targetState = true // Start detail box animation
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Animated visibility for the detail box that appears on pager card click
        AnimatedVisibility(
            visibleState = detailBoxVisibilityState,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }), // Fade in and slide up
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }) // Fade out and slide down
        ) {
            if (showDetailBox) { // Only render if showDetailBox is true
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            detailBoxVisibilityState.targetState = false // Start dismiss animation
                            // Using a short delay to ensure animation plays before hiding
                            // This can be wrapped in a LaunchedEffect or coroutineScope.launch if strict timing is needed.
                            showDetailBox = false // Hide immediately for this example
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = detailBoxContent.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = detailBoxContent.content,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to dismiss",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Call Confirmation Dialog
        if (showCallConfirmationDialog) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = {
                    // This is called when the user taps outside the dialog or presses the back button
                    showCallConfirmationDialog = false // Dismiss the dialog
                    phoneNumberToCall = null // Clear the number
                },
                title = { Text("Confirm Call") },
                text = { Text("Are you sure you want to call ${phoneNumberToCall}?") },
                confirmButton = {
                    TextButton(onClick = {
                        phoneNumberToCall?.let { number ->
                            // This code runs ONLY if the user explicitly taps "Call" on the dialog
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$number")
                            }
                            context.startActivity(intent) // Opens the phone dialer
                        }
                        showCallConfirmationDialog = false // Dismiss the dialog after initiating call
                        phoneNumberToCall = null // Clear the number
                    }) {
                        Text("Call")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCallConfirmationDialog = false // Just dismiss the dialog
                        phoneNumberToCall = null // Clear the number
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Composable for displaying a static section of emergency numbers.
 * Each item is clickable to initiate a phone dial.
 *
 * @param title The title of the section (e.g., "Emergency Call Numbers").
 * @param items List of EmergencyItem data class to display.
 * @param onCallRequested Lambda to request a call, passing the phone number.
 */
@Composable
fun StaticEmergencySection(title: String, items: List<EmergencyItem>, onCallRequested: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            item.phoneNumber?.let { phoneNumberString ->
                                onCallRequested(phoneNumberString) // Request call via callback
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    item.imageRes?.let {
                        // Display icon if available
                        Image(
                            painter = painterResource(id = it),
                            contentDescription = item.text,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 8.dp),
                            colorFilter = null // No color filter applied
                        )
                    }
                    Text(
                        text = item.text,
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Composable for an expandable box, typically used for hospital departments.
 * It shows a title and an expand/collapse icon. When expanded, it lists contact details.
 * Items within the expanded section are clickable to dial.
 *
 * @param title The title of the expandable box (e.g., "Cardiology").
 * @param dataList List of items to display when expanded (can be EmergencyItem or HospitalContact).
 * @param titleImageRes Optional image resource ID to display next to the title.
 * @param onCallRequested Lambda to request a call, passing the phone number.
 */
@Composable
fun ExpandableBox(title: String, dataList: List<Any>, titleImageRes: Int? = null, onCallRequested: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) } // State to control expansion

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded } // Toggle expanded state on click
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    titleImageRes?.let {
                        // Display title image if available
                        Image(
                            painter = painterResource(id = it),
                            contentDescription = title,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 8.dp),
                            colorFilter = null
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp)
                    )
                }
                // Icon changes based on expanded state
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Animated visibility for the content when expanded
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    dataList.forEach { item ->
                        when (item) {
                            is EmergencyItem -> {
                                // Handles EmergencyItem (e.g., Police, Fire) within an expandable box
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            item.phoneNumber?.let { phoneNumberString ->
                                                onCallRequested(phoneNumberString) // Request call via callback
                                            }
                                        }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    item.imageRes?.let {
                                        Image(
                                            painter = painterResource(id = it),
                                            contentDescription = item.text,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .padding(end = 8.dp),
                                            colorFilter = null
                                        )
                                    }
                                    Text(
                                        text = item.text,
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is HospitalContact -> {
                                // Handles HospitalContact items, showing hospital name and phone number
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCallRequested(item.phoneNumber) // Request call via callback
                                        }
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.hospitalName,
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.phoneNumber,
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable for a horizontally scrolling content pager.
 * Displays various health tips or information cards that auto-scroll.
 *
 * @param onCardClick Lambda to be invoked when a card is clicked, providing its title and content.
 */
@OptIn(ExperimentalFoundationApi::class) // Opt-in for ExperimentalFoundationApi for HorizontalPager
@Composable
fun HorizontalContentPager(onCardClick: (String, String) -> Unit) {
    val pageCount = 3 // Total number of pages in the pager
    val pagerState = rememberPagerState { pageCount } // Initialize PagerState
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for launching effects

    // LaunchedEffect for auto-scrolling the pager
    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            delay(5000) // Scroll every 5 seconds
            val nextPage = (pagerState.currentPage + 1) % pageCount // Calculate next page
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage) // Animate scroll to the next page
            }
        }
    }

    // Content details for each page in the pager
    val pagerItems = remember {
        listOf(
            PagerContentDetail(
                "First Aid Quick Guide",
                "Learn basic first aid steps for common injuries like cuts, burns, and sprains. Knowing these can make a big difference in an emergency!"
            ),
            PagerContentDetail(
                "Emergency Tips",
                "Keep calm, assess the situation, and call for help if needed. Remember to provide clear and concise information to emergency services."
            ),
            PagerContentDetail(
                "Health Tidbit",
                "Did you know regular hydration can significantly improve your mood and energy levels? Drink plenty of water throughout the day!"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Fixed height for the pager section
            .padding(horizontal = 0.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp), // Shows parts of adjacent pages
            pageSpacing = 16.dp, // Space between pages
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Pager takes available height within the Column
        ) { page ->
            val item = pagerItems[page]
            // Define card background and icon colors based on page index
            val cardBackgroundColor = when (page) {
                0 -> Color(0xFFC8E6C9) // Light Green
                1 -> MaterialTheme.colorScheme.errorContainer // Error related color
                2 -> MaterialTheme.colorScheme.secondaryContainer // Secondary related color
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val iconColor = when (page) {
                0 -> Color(0xFF2E7D32) // Dark Green
                1 -> MaterialTheme.colorScheme.onErrorContainer
                2 -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp), // Fixed width for each card
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clickable {
                            onCardClick(item.title, item.content) // Invoke callback on card click
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Select icon based on page index
                    // Note: Ensure these drawable resources exist in your project
                    val iconRes = when (page) {
                        0 -> R.drawable.baseline_health_and_safety_24
                        1 -> R.drawable.baseline_emergency_24
                        2 -> R.drawable.baseline_local_pharmacy_24
                        else -> R.drawable.baseline_person_24 // Fallback icon
                    }

                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = item.title,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 8.dp),
                        colorFilter = ColorFilter.tint(iconColor) // Apply icon color
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = iconColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Pager Indicator (dots)
        DotsIndicator(
            totalDots = pageCount,
            selectedIndex = pagerState.currentPage,
            selectedColor = MaterialTheme.colorScheme.primary,
            unSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Composable for displaying a custom dots indicator for a pager.
 *
 * @param totalDots The total number of dots (pages).
 * @param selectedIndex The index of the currently selected dot (page).
 * @param selectedColor Color for the selected dot.
 * @param unSelectedColor Color for unselected dots.
 * @param modifier Modifier to be applied to the row of dots.
 * @param dotSize Size of each dot.
 * @param spacing Spacing between dots.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    selectedColor: Color,
    unSelectedColor: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    spacing: Dp = 4.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing), // Arrange dots with spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape) // Make dots circular
                    .background(if (index == selectedIndex) selectedColor else unSelectedColor) // Set color based on selection
            )
        }
    }
}

/**
 * Preview Composable for the EmergencyCallScreen.
 */
@Preview(showBackground = true)
@Composable
fun EmergencyPreview() {
    DaktarSaabTheme {
        EmergencyCallScreen()
    }
}
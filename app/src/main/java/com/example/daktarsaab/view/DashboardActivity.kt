package com.example.daktarsaab.view
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.R
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import kotlinx.coroutines.launch

// Import for Material Icons (make sure you have `androidx.compose.material:material-icons-extended` in your build.gradle)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble // For a filled chat bubble icon
// import androidx.compose.material.icons.outlined.ChatBubble // If you prefer an outlined version


// Data class for Medical Articles
data class MedicalArticle(
    val title: String,
    val subtitle: String,
    val link: String,
    val iconResId: Int
)

// Main Activity for the Dashboard
class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge display for a modern look
        setContent {
            DaktarSaabTheme(content = { // Apply your app's theme
                DashboardScreen() // Display the main dashboard UI
            }, colorScheme = colorScheme)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun DashboardScreen() {
    // State for theme
    var isDarkTheme by remember { mutableStateOf(false) }

    // State variables to control the animated visibility of UI elements
    var showServiceGrid by remember { mutableStateOf(false) }
    var showMedicalArticles by remember { mutableStateOf(false) }
    var showRecentHistoryLabel by remember { mutableStateOf(false) }
    var showRecentHistory by remember { mutableStateOf(false) }
    var showNavBar by remember { mutableStateOf(false) } // State for Navbar visibility

    // State for selected navigation item
    var selectedNavItem by remember { mutableStateOf(0) }

    val context = LocalContext.current // Hoist context to composable scope

    // LaunchedEffect to trigger animations sequentially after a delay
    LaunchedEffect(true) {
        delay(300) // Delay before showing service grid
        showServiceGrid = true
        delay(300) // Delay before showing medical articles
        showMedicalArticles = true
        delay(300) // Delay before showing recent history label
        showRecentHistoryLabel = true
        delay(200) // Delay before showing recent history content
        showRecentHistory = true
        delay(400) // Delay before showing navbar
        showNavBar = true
    }

    DaktarSaabTheme(darkTheme = isDarkTheme, content = {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") }, // Title of the Top AppBar
                    actions = {
                        // Theme Toggle Button
                        IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isDarkTheme)
                                        R.drawable.baseline_light_mode_24
                                    else
                                        R.drawable.baseline_dark_mode_24
                                ),
                                contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode"
                            )
                        }
                        // Profile icon in the top app bar
                        Image(
                            painter = painterResource(id = R.drawable.baseline_person_24), // Replace with your profile icon
                            contentDescription = "Profile",
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp)
                                .clip(CircleShape) // Clip to a circle shape
                        )
                    }
                )
            },
            bottomBar = {
                // Animated visibility for the NavigationBar
                AnimatedVisibility(
                    visible = showNavBar,
                    enter = slideInVertically(
                        initialOffsetY = { it }, // Slide in from bottom
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = slideOutVertically(
                        targetOffsetY = { it }, // Slide out to bottom
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeOut(animationSpec = tween(durationMillis = 500))
                ) {
                    // Customizing Navbar colors and properties
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.primaryContainer, // Use primaryContainer for a distinct look
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        tonalElevation = 8.dp, // Add some elevation
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp) // Add horizontal and vertical padding
                            .clip(RoundedCornerShape(16.dp)) // Apply rounded corners to all four sides
                    ) {
                        // Home
                        NavigationBarItem(
                            selected = selectedNavItem == 0,
                            onClick = {
                                selectedNavItem = 0
                                // Refresh DashboardActivity if already open
                                if (context is DashboardActivity) {
                                    context.recreate()
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_home_24),
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        // Utilities
                        NavigationBarItem(
                            selected = selectedNavItem == 1,
                            onClick = { selectedNavItem = 1 },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_handyman_24), // Example icon for utilities
                                    contentDescription = "Utilities"
                                )
                            },
                            label = { Text("Utilities") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        // Chatbot (using a more bot-like icon) - Moved before Profile
                        NavigationBarItem(
                            selected = selectedNavItem == 2, // Changed index
                            onClick = {
                                selectedNavItem = 2 // Changed index
                                // Open ChatbotActivity
                                context.startActivity(Intent(context, ChatbotActivity::class.java))
                            },
                            icon = {
                                // Changed icon to a more "bot-like" icon
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_android_24), // Example: Android bot icon
                                    contentDescription = "Chatbot"
                                )
                            },
                            label = { Text("Chatbot") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        // Profile
                        NavigationBarItem(
                            selected = selectedNavItem == 3, // Changed index
                            onClick = { selectedNavItem = 3 }, // Changed index
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_person_pin_24),
                                    contentDescription = "Profile"
                                )
                            },
                            label = { Text("Profile") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            // **Apply verticalScroll to the main Column**
            Column(
                modifier = Modifier
                    .padding(innerPadding) // Apply padding from Scaffold
                    .padding(horizontal = 16.dp) // Additional horizontal padding for content
                    .fillMaxSize() // Fill the available size
                    .verticalScroll(rememberScrollState()), // **Make the column scrollable**
                verticalArrangement = Arrangement.spacedBy(20.dp) // Spacing between vertical elements
            ) {
                // Display content based on selectedNavItem
                when (selectedNavItem) {
                    0 -> HomeContent(
                        showServiceGrid = showServiceGrid,
                        showMedicalArticles = showMedicalArticles,
                        showRecentHistoryLabel = showRecentHistoryLabel,
                        showRecentHistory = showRecentHistory
                    )
                    1 -> UtilitiesContent() // New Utilities content
                    2 -> ChatbotContent() // Placeholder for Chatbot content (actual activity is launched)
                    3 -> ProfileContent() // Placeholder for Profile content
                }

                Spacer(modifier = Modifier.height(16.dp)) // Add some space at the bottom for the navbar
            }
        }
    }, colorScheme = colorScheme)
}

@Composable
fun HomeContent(
    showServiceGrid: Boolean,
    showMedicalArticles: Boolean,
    showRecentHistoryLabel: Boolean,
    showRecentHistory: Boolean
) {
    // Animated visibility for Service Grid
    AnimatedVisibility(
        visible = showServiceGrid,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 }, // Slide in from top
            animationSpec = tween(durationMillis = 500)
        ) + fadeIn(animationSpec = tween(durationMillis = 500)) // Fade in
    ) {
        ServiceGrid() // Composable for service cards
    }

    // Animated visibility for Medical Articles
    AnimatedVisibility(
        visible = showMedicalArticles,
        enter = slideInHorizontally(
            initialOffsetX = { -it }, // Slide in from left
            animationSpec = tween(durationMillis = 500)
        ) + fadeIn(animationSpec = tween(durationMillis = 500)) // Fade in
    ) {
        // List of medical articles to display
        val articles = remember {
            listOf(
                MedicalArticle(
                    title = "Medical Articles",
                    subtitle = "Updated research & trends",
                    link = "https://www.healthhub.com",
                    iconResId = R.drawable.baseline_article_24
                ),
                MedicalArticle(
                    title = "Mental Wellness",
                    subtitle = "Tips for a healthy mind",
                    link = "https://www.mindcare.org",
                    iconResId = R.drawable.baseline_self_improvement_24
                ),
                MedicalArticle(
                    title = "Nutrition Guides",
                    subtitle = "Eat well, live long",
                    link = "https://www.facebook.com", // Example link
                    iconResId = R.drawable.baseline_fastfood_24
                )
            )
        }
        MedicalArticlesCard(articles = articles) // Composable for medical articles carousel
    }

    // Animated visibility for "Recent History" label
    AnimatedVisibility(
        visible = showRecentHistoryLabel,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)) // Simply fade in
    ) {
        Text("Recent History", style = MaterialTheme.typography.titleMedium)
    }

    // Animated visibility for Recent History content
    AnimatedVisibility(
        visible = showRecentHistory,
        enter = slideInVertically(
            initialOffsetY = { it }, // Slide in from bottom
            animationSpec = tween(durationMillis = 500)
        ) + fadeIn(animationSpec = tween(durationMillis = 500)) // Fade in
    ) {
        RecentHistory() // Composable for recent history list
    }
}

@Composable
fun UtilitiesContent() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Explore Utilities",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        // Reminder Card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable {
                    // TODO: Replace ReminderActivity::class.java with your actual Reminder activity
                    // context.startActivity(Intent(context, ReminderActivity::class.java))
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Set medicine reminders, appointments", style = MaterialTheme.typography.bodyMedium)
                }
                Icon(
                    painter = painterResource(id = R.drawable.baseline_alarm_24), // Reminder icon
                    contentDescription = "Reminders",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Emergency Call Card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable {
                    // TODO: Replace EmergencyCallActivity::class.java with your actual Emergency call activity
                    // context.startActivity(Intent(context, EmergencyCallActivity::class.java))
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Emergency Contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Quick access to emergency services", style = MaterialTheme.typography.bodyMedium)
                }
                Icon(
                    painter = painterResource(id = R.drawable.baseline_local_hospital_24), // Emergency icon
                    contentDescription = "Emergency Contact",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ProfileContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile Section Coming Soon!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            painter = painterResource(id = R.drawable.baseline_construction_24), // Under construction icon
            contentDescription = "Under Construction",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ChatbotContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Chatbot is accessible via the navigation bar icon!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            painter = painterResource(id = R.drawable.baseline_android_24), // Android bot icon
            contentDescription = "Chatbot",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}


@Composable
fun ServiceGrid() {
    // List of services with their titles and Lottie animation asset names
    val services = listOf(
        Pair("X-ray Scan", "xray.json"),
        Pair("Symptom Analyzer", "search.json"),
        Pair("Doctor Booking", "appointment.json"),
        Pair("Maps", "maps.json")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Arrange service cards in rows of two
        for (i in services.indices step 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // First service card in the row
                ServiceCard(services[i].first, services[i].second, Modifier.weight(1f))
                // Check if there's a second service for the row
                if (i + 1 < services.size) {
                    ServiceCard(services[i + 1].first, services[i + 1].second, Modifier.weight(1f))
                } else {
                    // Add a Spacer if there's only one card in the last row
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ServiceCard(title: String, assetName: String, modifier: Modifier) {
    val context = LocalContext.current
    // For X-ray Scan, we want it to loop forever, others play once.
    val playLottieForever = title == "X-ray Scan"

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = if (playLottieForever) LottieConstants.IterateForever else 1,
        speed = 1f,
        restartOnPlay = false // Ensure it doesn't restart when it's supposed to be static
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(160.dp)
            .fillMaxWidth()
            .clickable {
                when (title) {
                    "X-ray Scan" -> {
                        context.startActivity(Intent(context, XrayAnalysisActivity::class.java))
                    }
                    "Symptom Analyzer" -> {
                        context.startActivity(Intent(context, SymptomAnalayzes::class.java))
                    }
                    "Maps" -> {
                        context.startActivity(Intent(context, MapsActivity::class.java))
                    }
                    "Doctor Booking" -> {
                        // context.startActivity(Intent(context, DoctorBookingActivity::class.java))
                    }
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LottieAnimation(
                composition,
                progress,
                modifier = Modifier.size(90.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MedicalArticlesCard(articles: List<MedicalArticle>) {
    val pagerState = rememberPagerState(initialPage = 0) // State for the horizontal pager
    val context = LocalContext.current // Get current context to launch intents

    // Auto-scroll the pager every 5 seconds
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000) // Delay for 5 seconds
            val nextPage = (pagerState.currentPage + 1) % articles.size
            pagerState.animateScrollToPage(nextPage) // Scroll to the next page
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Fixed height for the articles card
            .clip(RoundedCornerShape(16.dp)) // Rounded corners
            .background(MaterialTheme.colorScheme.primary), // Use primary color from theme
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            count = articles.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Take up available vertical space
        ) { page ->
            val article = articles[page]
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // "Explore!" text with an arrow, clickable to open the article link
                    Row(
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                                context.startActivity(intent) // Open URL in browser
                            } catch (e: Exception) {
                                e.printStackTrace() // Log any errors
                                // Optionally show a Toast message here for the user
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Explore!",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_forward_24), // Forward arrow icon
                            contentDescription = "Explore link",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = article.title,
                        color = MaterialTheme.colorScheme.inversePrimary, // Use a contrasting color from theme
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = article.subtitle,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Icon for the medical article
                Icon(
                    painter = painterResource(id = article.iconResId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        // Pager indicator dots
        HorizontalPagerIndicator(
            pagerState = pagerState,
            pageCount = articles.size,
            modifier = Modifier
                .padding(vertical = 8.dp),
            activeColor = MaterialTheme.colorScheme.onPrimary,
            inactiveColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            indicatorWidth = 8.dp,
            indicatorHeight = 8.dp,
            spacing = 8.dp
        )
    }
}

@Composable
fun RecentHistory() {
    // Dummy data for recent appointments - now with more entries
    val appointments = listOf(
        "Appointment 1: Kathmandu - Dr. Sharma (Cardiologist)",
        "Appointment 2: Lalitpur - Dr. Basnet (Dermatologist)",
        "Appointment 3: Bhaktapur - Dr. Thapa (Pediatrician)",
        "Appointment 4: Chitwan - Dr. Koirala (Orthopedic)",
        "Appointment 5: Pokhara - Dr. Gurung (Dentist)",
        "Appointment 6: Biratnagar - Dr. Poudel (Neurologist)",
        "Appointment 7: Butwal - Dr. Rai (Ophthalmologist)",
        "Appointment 8: Dharan - Dr. Limbu (ENT Specialist)",
        "Appointment 9: Janakpur - Dr. Yadav (General Physician)",
        "Appointment 10: Dhangadhi - Dr. Chaudhary (Psychiatrist)"
    )

    // Using LazyColumn to make the list scrollable
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.heightIn(max = 400.dp) // Example: limit height to 400.dp
    ) {
        items(appointments) { appointment ->
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(appointment, style = MaterialTheme.typography.titleSmall)
                        // You can parse the appointment string to get a more specific location if needed
                        Text("Details available upon click", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { /* TODO: Handle Report click */ } // Placeholder for report button action
                    ) {
                        Text("Report")
                    }
                }
            }
        }
    }
}
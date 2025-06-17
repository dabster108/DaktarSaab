package com.example.daktarsaab.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign


// Data class for Medical Articles
data class MedicalArticle(
    val title: String,
    val subtitle: String,
    val link: String,
    val iconResId: Int
)

// Data class for Utility items
data class UtilityItem(
    val title: String,
    val description: String,
    val iconResId: Int,
    val targetActivity: Class<*>? = null, // Nullable for placeholders
    val onClick: ((android.content.Context) -> Unit)? = null // Custom click action
)

// Main Activity for the Dashboard
class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for a modern look
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Read dark mode preference from SharedPreferences
        val prefs = getSharedPreferences("daktar_prefs", MODE_PRIVATE)

        setContent {
            // Hoist dark mode state to Compose and sync with SharedPreferences
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

            DaktarSaabTheme(darkTheme = isDarkTheme) {
                DashboardScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { dark ->
                        isDarkTheme = dark
                        prefs.edit().putBoolean("dark_mode", dark).apply()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun DashboardScreen(
    onThemeToggle: (Boolean) -> Unit, // Callback to toggle theme
    isDarkTheme: Boolean // Current theme state
) {
    // States to control animated visibility for Home content
    var showServiceGrid by remember { mutableStateOf(false) }
    var showMedicalArticles by remember { mutableStateOf(false) }
    var showRecentHistoryLabel by remember { mutableStateOf(false) }
    var showRecentHistory by remember { mutableStateOf(false) }

    // State to control animated visibility for Utilities content
    var showUtilitiesContent by remember { mutableStateOf(false) }

    // Use rememberSaveable to persist the state across configuration changes
    var showNavBar by rememberSaveable { mutableStateOf(false) }
    var initialLoad by rememberSaveable { mutableStateOf(true) } // For initial animation sequence

    // State for selected navigation item
    var selectedNavItem by remember { mutableStateOf(0) }

    val context = LocalContext.current // Hoist context to composable scope

    // LaunchedEffect to trigger animations sequentially after a delay
    LaunchedEffect(initialLoad, selectedNavItem) {
        if (initialLoad) {
            delay(300)
            showServiceGrid = true
            delay(300)
            showMedicalArticles = true
            delay(300)
            showRecentHistoryLabel = true
            delay(200)
            showRecentHistory = true
            delay(400)
            showNavBar = true
            initialLoad = false // Mark initial load as complete
        } else {
            // If not initial load, show everything for the current tab immediately
            showServiceGrid = (selectedNavItem == 0)
            showMedicalArticles = (selectedNavItem == 0)
            showRecentHistoryLabel = (selectedNavItem == 0)
            showRecentHistory = (selectedNavItem == 0)
            showUtilitiesContent = (selectedNavItem == 1) // Show utilities immediately if tab is selected
            showNavBar = true // Navbar always visible after initial load
        }
    }

    // Effect to control utilities content animation based on selected tab
    LaunchedEffect(selectedNavItem) {
        if (!initialLoad) { // Only animate if not initial app load
            // Hide previous content animations when switching tabs
            showServiceGrid = false
            showMedicalArticles = false
            showRecentHistoryLabel = false
            showRecentHistory = false
            showUtilitiesContent = false // Hide utilities first for re-animation

            if (selectedNavItem == 1) { // If Utilities tab is selected
                delay(100) // Small delay before animating in
                showUtilitiesContent = true
            } else if (selectedNavItem == 0) { // If Home tab is selected
                delay(100) // Small delay before animating in
                showServiceGrid = true
                delay(100)
                showMedicalArticles = true
                delay(100)
                showRecentHistoryLabel = true
                delay(100)
                showRecentHistory = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedNavItem == 1) "Utilities" else "Dashboard"
                    )
                },
                actions = {
                    // Theme Toggle Button
                    IconButton(onClick = { onThemeToggle(!isDarkTheme) }) {
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
                        painter = painterResource(id = R.drawable.baseline_person_24),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Home
                    NavigationBarItem(
                        selected = selectedNavItem == 0,
                        onClick = {
                            selectedNavItem = 0
                            // No recreate needed, compose handles state changes
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_home_24),
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                    // Utilities
                    NavigationBarItem(
                        selected = selectedNavItem == 1,
                        onClick = { selectedNavItem = 1 },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_handyman_24),
                                contentDescription = "Utilities"
                            )
                        },
                        label = { Text("Utilities") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                    // Chatbot
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            try {
                                context.startActivity(Intent(context, Class.forName("com.example.daktarsaab.view.ChatbotActivity")))
                            } catch (e: Exception) {
                                // Handle exception if class not found
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_android_24),
                                contentDescription = "Chatbot"
                            )
                        },
                        label = { Text("Chatbot") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                    // Profile
                    NavigationBarItem(
                        selected = selectedNavItem == 3,
                        onClick = { selectedNavItem = 3 },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_person_pin_24),
                                contentDescription = "Profile"
                            )
                        },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (selectedNavItem) {
                0 -> HomeContent(
                    showServiceGrid = showServiceGrid,
                    showMedicalArticles = showMedicalArticles,
                    showRecentHistoryLabel = showRecentHistoryLabel,
                    showRecentHistory = showRecentHistory
                )
                1 -> UtilitiesContent(showUtilitiesContent = showUtilitiesContent)
                3 -> ProfileContent()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Implement missing composables
@Composable
fun HomeContent(
    showServiceGrid: Boolean,
    showMedicalArticles: Boolean,
    showRecentHistoryLabel: Boolean,
    showRecentHistory: Boolean
) {
    AnimatedVisibility(
        visible = showServiceGrid,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(durationMillis = 500)
        ) + fadeIn(animationSpec = tween(durationMillis = 500))
    ) {
        ServiceGrid()
    }

    AnimatedVisibility(
        visible = showMedicalArticles,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(durationMillis = 500)
        ) + fadeIn(animationSpec = tween(durationMillis = 500))
    ) {
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
                    link = "https://www.facebook.com",
                    iconResId = R.drawable.baseline_fastfood_24
                )
            )
        }
        MedicalArticlesCard(articles = articles)
    }

    AnimatedVisibility(
        visible = showRecentHistoryLabel,
        enter = fadeIn(animationSpec = tween(durationMillis = 300))
    ) {
        Text("Recent History", style = MaterialTheme.typography.titleMedium)
    }

    AnimatedVisibility(
        visible = showRecentHistory,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 500)
        ) + fadeIn(animationSpec = tween(durationMillis = 500))
    ) {
        RecentHistory()
    }
}

@Composable
fun ServiceGrid() {
    val services = listOf(
        Pair("X-ray Scan", "xray.json"),
        Pair("Symptom Analyzer", "search.json"),
        Pair("Doctor Booking", "appointment.json"),
        Pair("Maps", "maps.json")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in services.indices step 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ServiceCard(services[i].first, services[i].second, Modifier.weight(1f))
                if (i + 1 < services.size) {
                    ServiceCard(services[i + 1].first, services[i + 1].second, Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ServiceCard(title: String, assetName: String, modifier: Modifier) {
    val context = LocalContext.current
    val playLottieForever = title == "X-ray Scan"

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = if (playLottieForever) LottieConstants.IterateForever else 1,
        speed = 1f,
        restartOnPlay = false
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(160.dp)
            .fillMaxWidth()
            .clickable {
                try {
                    when (title) {
                        "X-ray Scan" -> {
                            context.startActivity(Intent(context, Class.forName("com.example.daktarsaab.view.XrayAnalysisActivity")))
                        }
                        "Symptom Analyzer" -> {
                            context.startActivity(Intent(context, Class.forName("com.example.daktarsaab.view.SymptomAnalayzes")))
                        }
                        "Maps" -> {
                            context.startActivity(Intent(context, Class.forName("com.example.daktarsaab.view.MapsActivity")))
                        }
                        "Doctor Booking" -> {
                            context.startActivity(Intent(context, Class.forName("com.example.daktarsaab.view.DoctorBookActivity")))
                        }
                    }
                } catch (e: Exception) {
                    // Handle class not found exception
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
    val pagerState = rememberPagerState(initialPage = 0)
    val context = LocalContext.current

    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % articles.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            count = articles.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                    Row(
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
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
                            painter = painterResource(id = R.drawable.baseline_arrow_forward_24),
                            contentDescription = "Explore link",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = article.title,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = article.subtitle,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    painter = painterResource(id = article.iconResId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        HorizontalPagerIndicator(
            pagerState = pagerState,
            pageCount = articles.size,
            modifier = Modifier.padding(vertical = 8.dp),
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
    val appointments = listOf(
        "Appointment 1: Kathmandu - Dr. Sharma (Cardiologist)",
        "Appointment 2: Lalitpur - Dr. Basnet (Dermatologist)",
        "Appointment 3: Bhaktapur - Dr. Thapa (Pediatrician)",
        "Appointment 4: Chitwan - Dr. Koirala (Orthopedic)",
        "Appointment 5: Pokhara - Dr. Gurung (Dentist)"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.heightIn(max = 400.dp)
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
                        Text("Details available upon click", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { /* Handle Report click */ }
                    ) {
                        Text("Report")
                    }
                }
            }
        }
    }
}

@Composable
fun UtilitiesContent(showUtilitiesContent: Boolean) {
    val context = LocalContext.current

    val utilities = remember {
        listOf(
            UtilityItem(
                title = "Reminders",
                description = "Never miss a dose or appointment again. Your health, on schedule!",
                iconResId = R.drawable.baseline_alarm_24,
                onClick = { ctx ->
                    try {
                        ctx.startActivity(Intent(ctx, Class.forName("com.example.daktarsaab.view.ReminderActivity")))
                    } catch (e: Exception) {
                        // Handle exception
                    }
                }
            ),
            UtilityItem(
                title = "Emergency Contact",
                description = "Connect instantly to life-saving services and your trusted contacts.",
                iconResId = R.drawable.baseline_local_hospital_24,
                onClick = { ctx ->
                    try {
                        ctx.startActivity(Intent(ctx, Class.forName("com.example.daktarsaab.view.EmergencyCallActivity")))
                    } catch (e: Exception) {
                        // Handle exception
                    }
                }
            ),
            UtilityItem(
                title = "Symptom Analyzer",
                description = "Understand your symptoms better. Get insights before you consult.",
                iconResId = R.drawable.baseline_search_24,
                onClick = { ctx ->
                    try {
                        ctx.startActivity(Intent(ctx, Class.forName("com.example.daktarsaab.view.SymptomAnalayzes")))
                    } catch (e: Exception) {
                        // Handle exception
                    }
                }
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = showUtilitiesContent,
            enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 100)) +
                    slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = tween(durationMillis = 400, delayMillis = 100))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Health, Your Tools!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Dive into our practical tools designed to support your well-being, from timely reminders to immediate emergency support and smart symptom analysis",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        utilities.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = showUtilitiesContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 200 + index * 100)) +
                        slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(durationMillis = 400, delayMillis = 200 + index * 100)),
                modifier = Modifier.fillMaxWidth()
            ) {
                UtilityCard(item = item)
            }
        }
    }
}

@Composable
fun UtilityCard(item: UtilityItem) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { item.onClick?.invoke(context) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Icon(
                painter = painterResource(id = item.iconResId),
                contentDescription = item.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
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
            painter = painterResource(id = R.drawable.baseline_construction_24),
            contentDescription = "Under Construction",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}
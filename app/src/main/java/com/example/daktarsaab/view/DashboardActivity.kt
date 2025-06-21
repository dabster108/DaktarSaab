package com.example.daktarsaab.view

import android.annotation.SuppressLint
import android.content.Context // Added import for Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.R
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.example.daktarsaab.viewmodel.DashboardViewModel
import com.google.accompanist.pager.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import androidx.core.content.edit


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
    private lateinit var viewModel: DashboardViewModel
    private val TAG = "DashboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Check if we're coming from login and pass the USER_ID to the ViewModel
        val comingFromLogin = intent.getBooleanExtra("FROM_LOGIN", false)
        val userIdFromIntent = intent.getStringExtra("USER_ID")

        if (comingFromLogin && userIdFromIntent != null) {
            Log.d(TAG, "Activity launched from login with USER_ID: $userIdFromIntent. Forcing data fetch for this user.")
            viewModel.fetchUserData(forcedUserId = userIdFromIntent)
        } else {
            Log.d(TAG, "Activity not launched from login or USER_ID not provided, ViewModel will use default fetch logic.")
            // ViewModel's init block already calls fetchUserData() which will use currentUser by default
        }

        // Enable edge-to-edge display for a modern look
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Read dark mode preference from SharedPreferences
        val prefs = getSharedPreferences("daktar_prefs", MODE_PRIVATE)

        setContent {
            // Hoist dark mode state to Compose and sync with SharedPreferences
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

            DaktarSaabTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { dark ->
                            isDarkTheme = dark
                            prefs.edit() { putBoolean("dark_mode", dark) }
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun DashboardScreen(
    onThemeToggle: (Boolean) -> Unit, // Callback to toggle theme
    isDarkTheme: Boolean, // Current theme state
    viewModel: DashboardViewModel // Dashboard ViewModel
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
                        text = when (selectedNavItem) {
                            1 -> "Utilities"
                            3 -> "Profile"
                            else -> "Dashboard"
                        }
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

                    // Profile image in the top app bar
                    val userImageUrl by viewModel.userProfileImageUrl.observeAsState()

                    // Use AsyncImage to load the user's profile image from Cloudinary
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { selectedNavItem = 3 } // Navigate to profile tab when clicked
                    ) {
                        if (userImageUrl != null && userImageUrl!!.isNotEmpty()) {
                            // If we have a valid image URL, load it with AsyncImage
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // If no image URL is available, show the default person icon
                            Image(
                                painter = painterResource(id = R.drawable.baseline_person_24),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
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
                                val intent = Intent(context, Class.forName("com.example.daktarsaab.view.ChatbotActivity"))
                                val user = viewModel.userData.value
                                val profileUrl = viewModel.userProfileImageUrl.value
                                intent.putExtra("USER_NAME", user?.firstName ?: "User") // Changed to pass only firstName
                                intent.putExtra("PROFILE_IMAGE_URL", profileUrl)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle exception if class not found
                                Log.e("DashboardScreen", "Error starting ChatbotActivity", e)
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
                3 -> ProfileContent(viewModel = viewModel)
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // State to track whether to show all appointments or just the first two
            var showAllAppointments by remember { mutableStateOf(false) }

            // Row to contain both the title and the "View All" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent History", style = MaterialTheme.typography.titleMedium)

                // View All toggle button
                Row(
                    modifier = Modifier
                        .clickable { showAllAppointments = !showAllAppointments }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_swipe_down_24),
                        contentDescription = "Toggle Appointments View",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showRecentHistory,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeIn(animationSpec = tween(durationMillis = 500))
            ) {
                RecentHistory(showAllAppointments)
            }
        }
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

    val viewModel: DashboardViewModel = viewModel()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in services.indices step 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ServiceCard(services[i].first, services[i].second, viewModel, Modifier.weight(1f))
                if (i + 1 < services.size) {
                    ServiceCard(services[i + 1].first, services[i + 1].second, viewModel, Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    title: String,
    assetName: String,
    viewModel: DashboardViewModel,
    modifier: Modifier
) {
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
                            val intent = Intent(context, Class.forName("com.example.daktarsaab.view.MapsActivity"))
                            val user = viewModel.userData.value
                            val profileUrl = viewModel.userProfileImageUrl.value
                            intent.putExtra("USER_NAME", user?.firstName ?: "User")
                            intent.putExtra("PROFILE_IMAGE_URL", profileUrl)
                            // Pass the current theme state
                            val prefs = context.getSharedPreferences("daktar_prefs", Context.MODE_PRIVATE) // Changed to Context.MODE_PRIVATE
                            intent.putExtra("IS_DARK_THEME", prefs.getBoolean("dark_mode", false))
                            context.startActivity(intent)
                        }
                        "Doctor Booking" -> {
                            context.startActivity(Intent(context, Class.forName("com.example.daktarsaab.view.DoctorBookActivity")))
                        }
                    }
                } catch (e: Exception) {
                    // Handle class not found exception
                    Log.e("ServiceCard", "Error starting activity for $title", e)
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
fun RecentHistory(showAllAppointments: Boolean) {
    val appointments = listOf(
        "Appointment 1: Kathmandu - Dr. Sharma (Cardiologist)",
        "Appointment 2: Lalitpur - Dr. Basnet (Dermatologist)",
        "Appointment 3: Bhaktapur - Dr. Thapa (Pediatrician)",
        "Appointment 4: Chitwan - Dr. Koirala (Orthopedic)",
        "Appointment 5: Pokhara - Dr. Gurung (Dentist)"
    )

    // Calculate which appointments to show based on the state
    val appointmentsToShow = if (showAllAppointments) {
        appointments
    } else {
        appointments.take(2)
    }

    // Fixed height column instead of LazyColumn to prevent scrolling
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        appointmentsToShow.forEach { appointment ->
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

        // Show a message when there are more appointments to see but not showing all
        if (!showAllAppointments && appointments.size > 2) {

        }
    }
}

@Composable
fun UtilitiesContent(showUtilitiesContent: Boolean) {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel()
    val userData by viewModel.userData.observeAsState()
    val userProfileImageUrl by viewModel.userProfileImageUrl.observeAsState()

    val utilities = remember {
        listOf(
            UtilityItem(
                title = "Reminders",
                description = "Never miss a dose or appointment again. Your health, on schedule!",
                iconResId = R.drawable.baseline_alarm_24,
                onClick = { ctx ->
                    val intent = Intent(ctx, Class.forName("com.example.daktarsaab.view.ReminderActivity"))
                    intent.putExtra("USER_NAME", "${userData?.firstName} ${userData?.lastName}")
                    intent.putExtra("PROFILE_IMAGE_URL", userProfileImageUrl)
                    ctx.startActivity(intent)
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


@SuppressLint("ContextCastToActivity")
@Composable
fun ProfileContent(viewModel: DashboardViewModel) {
    val userProfileImageUrl by viewModel.userProfileImageUrl.observeAsState()
    val userData by viewModel.userData.observeAsState()
    var showEditProfile by remember { mutableStateOf(false) }
    val isUserDataLoaded = userData != null

    LaunchedEffect(userProfileImageUrl, userData) {
        Log.d("ProfileContent", "Profile image URL in ProfileContent: $userProfileImageUrl")
        Log.d("ProfileContent", "User data in ProfileContent: FirstName: ${userData?.firstName}, Email: ${userData?.email}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        userProfileImageUrl?.let { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                Log.d("ProfileContent", "Displaying image from URL: $imageUrl")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .error(R.drawable.baseline_person_24)
                        .placeholder(R.drawable.baseline_person_24)
                        .build(),
                    contentDescription = "User Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Log.d("ProfileContent", "Image URL is empty, showing default person icon")
                DefaultProfileIcon(size = 120.dp)
            }
        } ?: run {
            Log.d("ProfileContent", "No image URL, showing default person icon")
            DefaultProfileIcon(size = 120.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        userData?.let {
            Text(
                text = "${it.firstName} ${it.lastName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it.email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } ?: run {
            Text(
                text = "Loading user data...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        ProfileSectionItem(title = "My Reports", icon = R.drawable.baseline_assessment_24) {
            Log.d("ProfileContent", "My Reports clicked")
        }

        // Only show Edit Profile option when user data is loaded
        if (isUserDataLoaded) {
            ProfileSectionItem(title = "Edit Profile", icon = R.drawable.baseline_edit_24) {
                showEditProfile = true
            }
        }

        val context = LocalContext.current

        ProfileSectionItem(title = "Logout", icon = R.drawable.baseline_logout_24, isDestructive = true) {
            try {
                val intent = Intent(context, Class.forName("com.example.daktarsaab.view.LoginActivity"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
                (context as? ComponentActivity)?.finish()
            } catch (e: Exception) {
                Log.e("ProfileContent", "Error navigating to login screen", e)
            }
        }
    }

    if (showEditProfile && isUserDataLoaded) {
        EditProfileHandler(
            viewModel = viewModel,
            onComplete = { showEditProfile = false }
        )
    }
}

@Composable
fun DefaultProfileIcon(size: Dp) {
    Icon(
        painter = painterResource(id = R.drawable.baseline_person_24),
        contentDescription = "Default Profile Image",
        modifier = Modifier.size(size).clip(CircleShape),
        tint = MaterialTheme.colorScheme.primaryContainer
    )
}

@Composable
fun ProfileSectionItem(title: String, icon: Int, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = title,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24),
            contentDescription = "Go to $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun EditProfileHandler(viewModel: DashboardViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    val intent = Intent(context, EditProfileActivity::class.java)
    val user = viewModel.userData.value
    val profileUrl = viewModel.userProfileImageUrl.value
    intent.putExtra("USER_EMAIL", user?.email ?: "")
    intent.putExtra("PROFILE_IMAGE_URL", profileUrl ?: "")
    intent.putExtra("USER_ID", user?.userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: "")

    LaunchedEffect(Unit) {
        context.startActivity(intent)
        onComplete()
    }
}

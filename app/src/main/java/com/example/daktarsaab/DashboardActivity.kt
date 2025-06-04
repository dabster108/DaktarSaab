package com.example.daktarsaab
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            DaktarSaabTheme { // Apply your app's theme
                DashboardScreen() // Display the main dashboard UI
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun DashboardScreen() {
    // State variables to control the animated visibility of UI elements
    var showServiceGrid by remember { mutableStateOf(false) }
    var showMedicalArticles by remember { mutableStateOf(false) }
    var showRecentHistoryLabel by remember { mutableStateOf(false) }
    var showRecentHistory by remember { mutableStateOf(false) }

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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") }, // Title of the Top AppBar
                actions = {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) // Apply padding from Scaffold
                .padding(16.dp) // Additional padding for content
                .fillMaxSize(), // Fill the available size
            verticalArrangement = Arrangement.spacedBy(20.dp) // Spacing between vertical elements
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
    val context = LocalContext.current // Get context for starting activities
    Card(
        shape = RoundedCornerShape(16.dp), // Rounded corners for the card
        modifier = modifier
            .height(160.dp) // Fixed height for the card
            .fillMaxWidth()
            .clickable { // Make the card clickable
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
                    // Add other cases for navigation if needed
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Card shadow
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Lottie animation for the service icon
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
            val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever) // Loop animation
            LottieAnimation(
                composition,
                progress,
                modifier = Modifier.size(90.dp) // Size of the Lottie animation
            )
            Spacer(modifier = Modifier.height(8.dp)) // Space between animation and text
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis, // Ellipsize long text
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MedicalArticlesCard(articles: List<MedicalArticle>) {
    val pagerState = rememberPagerState(initialPage = 0) // State for the horizontal pager
    val coroutineScope = rememberCoroutineScope()
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
            .background(Color(0xFF3366CC)), // Background color for the card
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
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_forward_24), // Forward arrow icon
                            contentDescription = "Explore link",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = article.title,
                        color = Color.Yellow,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = article.subtitle,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Icon for the medical article
                Icon(
                    painter = painterResource(id = article.iconResId),
                    contentDescription = null,
                    tint = Color.White,
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
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.5f),
            indicatorWidth = 8.dp,
            indicatorHeight = 8.dp,
            spacing = 8.dp
        )
    }
}

@Composable
fun RecentHistory() {
    // Dummy data for recent appointments
    val appointments = listOf(
        "Appointment 1: Kathmandu",
        "Appointment 2: Lalitpur",
        "Appointment 3: Bhaktapur",
        "Appointment 4: Chitwan"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        appointments.forEach { appointment ->
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
                        Text("Location details", style = MaterialTheme.typography.bodySmall)
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


@Preview(showBackground = true)
@Composable
fun preDash(){
    DaktarSaabTheme { // Apply your app's theme
        DashboardScreen() // Display the main dashboard UI
    }
}


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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MedicalArticle(
    val title: String,
    val subtitle: String,
    val link: String,
    val iconResId: Int
)

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaktarSaabTheme {
                DashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun DashboardScreen() {
    var showServiceGrid by remember { mutableStateOf(false) }
    var showMedicalArticles by remember { mutableStateOf(false) }
    var showRecentHistoryLabel by remember { mutableStateOf(false) }
    var showRecentHistory by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        delay(300)
        showServiceGrid = true
        delay(300)
        showMedicalArticles = true
        delay(300)
        showRecentHistoryLabel = true
        delay(200)
        showRecentHistory = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(160.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
            val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
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
    val coroutineScope = rememberCoroutineScope()
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
            .background(Color(0xFF3366CC)),
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
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_forward_24),
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
                Icon(
                    painter = painterResource(id = article.iconResId),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

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
                        onClick = { /* TODO: Handle Report click */ }
                    ) {
                        Text("Report")
                    }
                }
            }
        }
    }
}
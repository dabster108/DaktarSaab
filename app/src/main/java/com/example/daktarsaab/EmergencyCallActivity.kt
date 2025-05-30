 package com.example.daktarsaab

import android.content.Intent
// import android.net.Uri // Replaced by androidx.core.net.toUri
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri // For String.toUri()
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
// Removed: import com.google.accompanist.pager.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class for general emergency numbers with optional image
data class EmergencyItem(val text: String, val imageRes: Int? = null, val phoneNumber: String? = null)

// Data class for hospital contact information
data class HospitalContact(val hospitalName: String, val phoneNumber: String)

// Data class for pager content details
data class PagerContentDetail(val title: String, val content: String)

class EmergencyCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaktarSaabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EmergencyCallScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class) // Changed from ExperimentalPagerApi
@Composable
fun EmergencyCallScreen(modifier: Modifier = Modifier) {
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

    val emergencyNumbersData = remember(rawEmergencyData) { rawEmergencyData.first { it.first == "Emergency Call Numbers" } }
    val hospitalDataList = remember(rawEmergencyData) { rawEmergencyData.filter { it.first != "Emergency Call Numbers" } }

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

    val emergencyVisibleState = remember { MutableTransitionState(false) }
    val hospitalVisibleStates = remember {
        processedHospitalData.map { MutableTransitionState(false) }
    }
    val pagerVisibleState = remember { MutableTransitionState(false) }

    var showDetailBox by remember { mutableStateOf(false) }
    var detailBoxContent by remember { mutableStateOf(PagerContentDetail("", "")) }
    val detailBoxVisibilityState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        delay(200)
        emergencyVisibleState.targetState = true
        delay(300)
        if (hospitalVisibleStates.isNotEmpty()) {
            hospitalVisibleStates[0].targetState = true
            if (hospitalVisibleStates.size > 1) {
                delay(150)
                hospitalVisibleStates[1].targetState = true
            }
            if (hospitalVisibleStates.size > 2) {
                delay(150)
                hospitalVisibleStates[2].targetState = true
            }
            if (hospitalVisibleStates.size > 3) {
                delay(150)
                hospitalVisibleStates[3].targetState = true
            }
        }
        delay(150)
        pagerVisibleState.targetState = true
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .weight(1f)
        ) {
            item {
                AnimatedVisibility(
                    visibleState = emergencyVisibleState,
                    enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StaticEmergencySection(
                        title = emergencyNumbersData.first,
                        items = emergencyNumbersData.second as? List<EmergencyItem> ?: emptyList() // Safe cast
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(processedHospitalData.size) { index ->
                val (title, dataList, titleImageRes) = processedHospitalData[index]
                val visibleState = hospitalVisibleStates[index]
                val enterAnimation = when (index % 4) {
                    0, 2 -> slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn()
                    1, 3 -> slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn()
                    else -> fadeIn()
                }
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = enterAnimation
                ) {
                    ExpandableBox(
                        title = title,
                        dataList = dataList,
                        titleImageRes = titleImageRes
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(
                    visibleState = pagerVisibleState,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                ) {
                    HorizontalContentPager(
                        onCardClick = { title, content ->
                            detailBoxContent = PagerContentDetail(title, content)
                            showDetailBox = true
                            detailBoxVisibilityState.targetState = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        AnimatedVisibility(
            visibleState = detailBoxVisibilityState,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            if (showDetailBox) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            detailBoxVisibilityState.targetState = false
                             // To ensure animation completes before state change if needed:
                             // coroutineScope.launch { delay(300); showDetailBox = false }
                             showDetailBox = false // Hide immediately for now
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
    }
}

@Composable
fun StaticEmergencySection(title: String, items: List<EmergencyItem>) {
    val context = LocalContext.current
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
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = "tel:$phoneNumberString".toUri() // Use .toUri()
                                }
                                context.startActivity(intent)
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
        }
    }
}

@Composable
fun ExpandableBox(title: String, dataList: List<Any>, titleImageRes: Int? = null) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    titleImageRes?.let {
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
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    dataList.forEach { item ->
                        when (item) {
                            is EmergencyItem -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            item.phoneNumber?.let { phoneNumberString ->
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = "tel:$phoneNumberString".toUri() // Use .toUri()
                                                }
                                                context.startActivity(intent)
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = "tel:${item.phoneNumber}".toUri() // Use .toUri()
                                            }
                                            context.startActivity(intent)
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

@OptIn(ExperimentalFoundationApi::class) // Changed from ExperimentalPagerApi
@Composable
fun HorizontalContentPager(onCardClick: (String, String) -> Unit) {
    val pageCount = 3
    val pagerState = rememberPagerState { pageCount } // New PagerState initialization
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) { // Trigger effect when currentPage changes
        while (true) { // Loop for continuous auto-scrolling
            delay(5000) // 5 seconds delay
            val nextPage = (pagerState.currentPage + 1) % pageCount
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

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
            .padding(horizontal = 0.dp) // Adjusted padding if needed
    ) {
        HorizontalPager(
            // count parameter is not used in the new API, pageCount is from PagerState
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp), // Shows parts of adjacent pages
            pageSpacing = 16.dp, // Use pageSpacing instead of itemSpacing
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Pager takes available height within the Column
        ) { page ->
            val item = pagerItems[page]
            val cardBackgroundColor = when (page) {
                0 -> Color(0xFFC8E6C9)
                1 -> MaterialTheme.colorScheme.errorContainer
                2 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val iconColor = when (page) {
                0 -> Color(0xFF2E7D32)
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
                            onCardClick(item.title, item.content)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
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
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor)
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

        // Custom Pager Indicator
        DotsIndicator(
            totalDots = pageCount,
            selectedIndex = pagerState.currentPage,
            selectedColor = MaterialTheme.colorScheme.primary,
            unSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

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
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (index == selectedIndex) selectedColor else unSelectedColor)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EmergencyPreview() {
    DaktarSaabTheme {
        EmergencyCallScreen()
    }
}


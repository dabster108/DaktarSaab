package com.example.daktarsaab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            MedicalMapScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalMapScreen() {
    val context = LocalContext.current
    val defaultLocation = remember { GeoPoint(51.5074, 0.1278) } // London coordinates
    val medicalCategories = listOf("Hospitals", "Pharmacy", "Clinic", "Emergency")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Search and Navigation Section
        var destination by remember { mutableStateOf("") }

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Enter destination") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    if (destination.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                }) {
                    Icon(Icons.Default.ArrowForward, "Navigate")
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // Map and Categories Layout
        Row(Modifier.weight(1f)) {
            // Categories Panel
            LazyColumn(
                modifier = Modifier.width(150.dp)
            ) {
                items(medicalCategories) { category ->
                    MedicalCategoryCard(
                        category = category,
                        onClick = {
                            val query = when (category) {
                                "Hospitals" -> "hospital"
                                "Pharmacy" -> "pharmacy"
                                "Clinic" -> "clinic"
                                "Emergency" -> "emergency room"
                                else -> ""
                            }
                            if (query.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                                }
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // OpenStreetMap View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(400.dp)
            ) {
                OSMapView(defaultLocation)
            }
        }
    }
}

@Composable
fun OSMapView(initialLocation: GeoPoint) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(initialLocation)

            // Add marker
            Marker(this).apply {
                position = initialLocation
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Default Location"
            }.also { overlays.add(it) }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalCategoryCard(category: String, onClick: () -> Unit) {
    val iconRes = when (category) {
        "Hospitals" -> android.R.drawable.ic_menu_info_details
        "Pharmacy" -> android.R.drawable.ic_menu_info_details
        "Clinic" -> android.R.drawable.ic_menu_info_details
        "Emergency" -> android.R.drawable.ic_menu_info_details
        else -> android.R.drawable.ic_menu_help
    }
    val iconPainter = painterResource(id = iconRes)

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Image(painter = iconPainter, contentDescription = category, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(category, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


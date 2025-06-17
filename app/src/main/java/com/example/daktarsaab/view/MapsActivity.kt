package com.example.daktarsaab.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// --- Geoapify Data Classes (Autocomplete) ---
data class GeoapifyAutocompleteResponse(val features: List<GeoapifyFeature>)
data class GeoapifyFeature(val properties: GeoapifyProperties)
data class GeoapifyProperties(val formatted: String)

// --- Data Classes for Places API ---
data class PlacesResponse(val features: List<PlaceFeature>)
data class PlaceFeature(
    val type: String,
    val properties: PlaceProperties,
    val geometry: Geometry? = null
)

data class PlaceProperties(
    val name: String? = null,
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val postcode: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val lat: Double?,
    val lon: Double?,
    val formatted: String?,
    val address_line1: String?,
    val address_line2: String?,
    val categories: List<String>?,
    val details: List<String>? = null,
    val datasource: PlaceDataSource? = null,
    val place_id: String
)

data class PlaceDataSource(
    val sourcename: String? = null,
    val attribution: String? = null,
    val license: String? = null,
    val url: String? = null
)

data class Geometry(
    val type: String,
    val coordinates: List<Double>
)

// --- Data Classes for Place Details API ---
data class PlaceDetailsResponse(val features: List<PlaceFeature>) // Re-uses PlaceFeature

// --- Data Classes for Routing API ---
data class RoutingResponse(
    val type: String,
    val features: List<RouteFeature>,
    val properties: RoutingOverallProperties? = null
)

data class RouteFeature(
    val type: String,
    val properties: RouteProperties,
    val geometry: RouteGeometry
)

data class RoutingOverallProperties(
    val mode: String?,
    val waypoints: List<WaypointInfo>?,
    val units: String?,
    val distance: Double?,
    val time: Double?
)

data class WaypointInfo(
    val original_index: Int?,
    val location: List<Double>?
)

data class RouteProperties(
    val mode: String?,
    val distance: Double?,
    val time: Double?,
    val legs: List<RouteLeg>?
)

data class RouteLeg(
    val distance: Double?,
    val time: Double?,
    val steps: List<RouteStep>?
)

data class RouteStep(
    val instruction: RouteInstruction?,
    val distance: Double?,
    val time: Double?,
    val from_index: Int?,
    val to_index: Int?
)

data class RouteInstruction(
    val text: String?
)

data class RouteGeometry(
    val type: String?,
    val coordinates: List<Any>? // Can be List<List<Double>> or List<List<List<Double>>>
)

// --- Data Classes for IP Geolocation API ---
data class IpGeolocationResponse(
    val ip: String?,
    val city: IpCity? = null,
    val state: IpState? = null,
    val country: IpCountry? = null,
    val continent: IpContinent? = null,
    val location: IpLocation? = null
)

data class IpCity(val name: String?)
data class IpState(val name: String?)
data class IpCountry(val name: String?, val iso_code: String?)
data class IpContinent(val name: String?, val code: String?)
data class IpLocation(val latitude: Double?, val longitude: Double?)

// --- Geoapify Data Classes (Route Planner API) ---
data class RoutePlannerRequest(
    val mode: String,
    val agents: List<RoutePlannerAgent>,
    val jobs: List<RoutePlannerJob>
)

data class RoutePlannerAgent(
    val start_location: List<Double>, // [longitude, latitude]
    val end_location: List<Double>,   // [longitude, latitude]
    val pickup_capacity: Int? = null,
    val name: String? = null
)

data class RoutePlannerJob(
    val id: String, // Jobs typically require an ID for referencing in the response
    val location: List<Double>, // [longitude, latitude]
    val duration: Int? = null,          // in seconds
    val pickup_amount: Int? = null
)

data class RoutePlannerResponse(
    val type: String, // Should be "FeatureCollection"
    val features: List<RoutePlannerAgentRouteFeature>
)

data class RoutePlannerAgentRouteFeature(
    val type: String, // Should be "Feature"
    val properties: RoutePlannerAgentRouteProperties,
    val geometry: RouteGeometry // Reusing existing RouteGeometry
)

data class RoutePlannerAgentRouteProperties(
    val agent_name: String?,
    val agent_id: String?,
    val distance: Double?,
    val time: Double?,
    val jobs_order: List<String>? // List of Job IDs
)

// --- Geoapify Service Interface ---
interface GeoapifyService {
    @GET("v1/geocode/autocomplete")
    suspend fun getAddressSuggestions(
        @Query("text") text: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 7
    ): GeoapifyAutocompleteResponse

    @GET("v1/geocode/search")
    suspend fun searchGeocode(
        @Query("text") text: String? = null,
        @Query("housenumber") housenumber: String? = null,
        @Query("street") street: String? = null,
        @Query("postcode") postcode: String? = null,
        @Query("city") city: String? = null,
        @Query("state") state: String? = null,
        @Query("country") country: String? = null,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 1
    ): PlacesResponse

    @GET("v2/places")
    suspend fun searchPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String? = null,
        @Query("bias") bias: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("apiKey") apiKey: String
    ): PlacesResponse

    @GET("v2/place-details")
    suspend fun getPlaceDetails(
        @Query("id") id: String,
        @Query("apiKey") apiKey: String
    ): PlaceDetailsResponse

    @GET("v1/routing")
    suspend fun getRoute(
        @Query("waypoints") waypoints: String, // e.g. "lat1,lon1|lat2,lon2"
        @Query("mode") mode: String,
        @Query("apiKey") apiKey: String,
        @Query("details") details: String? = null
    ): RoutingResponse

    @GET("v1/ipinfo")
    suspend fun getIpGeolocation(
        @Query("ip") ip: String? = null,
        @Query("apiKey") apiKey: String
    ): IpGeolocationResponse

    @POST("v1/routeplanner")
    suspend fun planComplexRoute(
        @Query("apiKey") apiKey: String,
        @Body body: RoutePlannerRequest
    ): RoutePlannerResponse

    companion object {
        fun create(): GeoapifyService {
            return Retrofit.Builder()
                .baseUrl("https://api.geoapify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeoapifyService::class.java)
        }
    }
}

class MapsActivity : ComponentActivity() {

    private var permissionGrantedSignal by mutableStateOf(0L)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MapsActivity", "Permission GRANTED by user.")
                permissionGrantedSignal = System.currentTimeMillis() // Update the signal
            } else {
                Log.d("MapsActivity", "Permission DENIED by user.")
                // Handle permission denial (e.g., show a message to the user)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            DaktarSaabTheme(content = {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MedicalMapScreen(
                        onLocationPermissionRequested = {
                            Log.d("MapsActivity", "Requesting location permission via launcher.")
                            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        permissionSignal = permissionGrantedSignal
                    )
                }
            }, colorScheme = colorScheme)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalMapScreen(onLocationPermissionRequested: () -> Unit, permissionSignal: Long) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedLocationName by remember { mutableStateOf("") }
    // State for start and destination search fields
    var startSearchQuery by remember { mutableStateOf("") }
    var destinationSearchQuery by remember { mutableStateOf("") }

    // State for suggestions
    var startSuggestions by remember { mutableStateOf(listOf<GeoapifyFeature>()) }
    var destinationSuggestions by remember { mutableStateOf(listOf<GeoapifyFeature>()) }
    var showStartSuggestions by remember { mutableStateOf(false) }
    var showDestinationSuggestions by remember { mutableStateOf(false) }

    // State for route display
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var startMarkerState by remember { mutableStateOf<Marker?>(null) }
    var endMarkerState by remember { mutableStateOf<Marker?>(null) }

    // State for fullscreen mode
    var isFullscreen by remember { mutableStateOf(false) }

    // State for selected place category
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var placeMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }

    // State for place details dialog
    var showPlaceDetailsDialog by remember { mutableStateOf(false) }
    var placeDetailsTitle by remember { mutableStateOf("") }
    var placeDetailsMessage by remember { mutableStateOf("") }

    val geoapifyApiKey = "17f1b9b807bd49b6991ad029e5571cf1" // Updated API key
    var permissionCheckedInitially by remember { mutableStateOf(false) }
    var initialZoomDone by remember { mutableStateOf(false) }

    val normalZoomLevel = 15.0
    val fullscreenZoomLevel = normalZoomLevel * 1.2 // 40% increase

    val geoapifyService = remember { GeoapifyService.create() }

    // Function to display route - Added to fix unresolved reference errors
    suspend fun displayRoute(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        geometry: RouteGeometry,
        rawCoords: List<Any>,
        startAddress: String,
        endAddress: String
    ) = withContext(Dispatchers.Main) {
        try {
            // Clear any existing route
            mapViewRef?.let { mapView ->
                routePolyline?.let { mapView.overlays.remove(it) }
                startMarkerState?.let { mapView.overlays.remove(it) }
                endMarkerState?.let { mapView.overlays.remove(it) }
            }

            // Process route coordinates based on geometry type
            val routePoints = mutableListOf<GeoPoint>()

            when (geometry.type) {
                "LineString" -> {
                    // Format: [[lon1,lat1], [lon2,lat2], ...]
                    (rawCoords as? List<*>)?.forEach { point ->
                        (point as? List<*>)?.let {
                            if (it.size >= 2 && it[0] is Number && it[1] is Number) {
                                val lon = (it[0] as Number).toDouble()
                                val lat = (it[1] as Number).toDouble()
                                routePoints.add(GeoPoint(lat, lon))
                            }
                        }
                    }
                }
                "MultiLineString" -> {
                    // Format: [[[lon1,lat1], [lon2,lat2], ...], [...]]
                    (rawCoords as? List<*>)?.forEach { segment ->
                        (segment as? List<*>)?.forEach { point ->
                            (point as? List<*>)?.let {
                                if (it.size >= 2 && it[0] is Number && it[1] is Number) {
                                    val lon = (it[0] as Number).toDouble()
                                    val lat = (it[1] as Number).toDouble()
                                    routePoints.add(GeoPoint(lat, lon))
                                }
                            }
                        }
                    }
                }
            }

            if (routePoints.size >= 2) {
                // Create the route polyline
                val polyline = Polyline().apply {
                    setPoints(routePoints)
                    outlinePaint.color = AndroidColor.parseColor("#2196F3") // Blue color
                    outlinePaint.strokeWidth = 10f
                }

                // Create start and end markers
                val startMarker = Marker(mapViewRef).apply {
                    position = startPoint
                    title = "Start: $startAddress"
                    icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }

                val endMarker = Marker(mapViewRef).apply {
                    position = endPoint
                    title = "Destination: $endAddress"
                    icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_directions)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }

                // Add everything to map
                mapViewRef?.overlays?.add(polyline)
                mapViewRef?.overlays?.add(startMarker)
                mapViewRef?.overlays?.add(endMarker)

                // Save references
                routePolyline = polyline
                startMarkerState = startMarker
                endMarkerState = endMarker

                // Zoom to show the entire route
                val boundingBox = BoundingBox.fromGeoPoints(routePoints)
                mapViewRef?.zoomToBoundingBox(boundingBox.increaseByScale(1.2f), true, 100)

                mapViewRef?.invalidate()
            } else {
                Log.e("MedicalMapScreen", "Route has too few points to display")
            }
        } catch (e: Exception) {
            Log.e("MedicalMapScreen", "Error displaying route: ${e.message}", e)
        }
    }

    LaunchedEffect(permissionSignal) {
        Log.d("MedicalMapScreen", "LaunchedEffect for permission triggered. Signal: $permissionSignal")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("MedicalMapScreen", "Permission IS granted. Fetching location.")
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        currentLocation = GeoPoint(it.latitude, it.longitude)
                        Log.d("MedicalMapScreen", "Current location: $currentLocation")
                    } ?: run {
                        Log.d("MedicalMapScreen", "Last known location is null")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MedicalMapScreen", "Error fetching location: ${e.message}")
                }
        } else {
            Log.d("MedicalMapScreen", "Permission NOT granted.")
            if (permissionSignal == 0L && !permissionCheckedInitially) {
                Log.d("MedicalMapScreen", "Initial load, permission not checked. Requesting.")
                onLocationPermissionRequested()
                permissionCheckedInitially = true
            } else if (permissionSignal > 0L) {
                Log.d("MedicalMapScreen", "Permission denied by user after prompt. Not re-requesting automatically.")
            }
        }
    }

    LaunchedEffect(mapViewRef, currentLocation) {
        if (mapViewRef != null && currentLocation != null && !initialZoomDone) {
            mapViewRef?.controller?.setZoom(normalZoomLevel) // Use normalZoomLevel for initial zoom
            mapViewRef?.controller?.setCenter(currentLocation)
            initialZoomDone = true
            Log.d("MedicalMapScreen", "Initial zoom to current location: $currentLocation at level $normalZoomLevel")
        }
    }

    // Adjust zoom when fullscreen state changes
    LaunchedEffect(isFullscreen, mapViewRef, initialZoomDone) {
        if (mapViewRef != null && initialZoomDone) { // Ensure map is ready and initial zoom has occurred
            val targetZoom = if (isFullscreen) fullscreenZoomLevel else normalZoomLevel
            mapViewRef?.controller?.setZoom(targetZoom)
            Log.d("MedicalMapScreen", "Toggled fullscreen. Set zoom to $targetZoom. Is fullscreen: $isFullscreen")
        }
    }

    LaunchedEffect(selectedCategory, currentLocation, mapViewRef) {
        if (selectedCategory == null || currentLocation == null || mapViewRef == null) return@LaunchedEffect

        // Capture the non-null value of currentLocation for use in the coroutine
        val stableCurrentLocation = currentLocation

        coroutineScope.launch {
            try {
                Log.d("MedicalMapScreen", "Loading places for category: $selectedCategory")
                withContext(Dispatchers.Main) {
                    placeMarkers.forEach { mapViewRef?.overlays?.remove(it) }
                    mapViewRef?.invalidate()
                }
                placeMarkers = emptyList()

                val categoryMapping = mapOf(
                    "Hospitals" to "healthcare.hospital",
                    "Pharmacies" to "healthcare.pharmacy"
                )
                val geoCategory = categoryMapping[selectedCategory!!] ?: return@launch

                // Use the stableCurrentLocation for constructing the filter, asserting non-null
                val filter = "circle:${stableCurrentLocation!!.longitude},${stableCurrentLocation.latitude},5000" // 5km radius

                val response = geoapifyService.searchPlaces(
                    categories = geoCategory,
                    filter = filter,
                    limit = 20,
                    apiKey = geoapifyApiKey
                )
                Log.d("MedicalMapScreen", "Found ${response.features.size} places for $selectedCategory")

                val newMarkers = response.features.mapNotNull { feature ->
                    val lat = feature.properties.lat
                    val lon = feature.properties.lon
                    val name = feature.properties.name ?: feature.properties.formatted ?: "Unknown"
                    if (lat != null && lon != null) {
                        Marker(mapViewRef).apply {
                            position = GeoPoint(lat, lon)
                            title = name
                            snippet = feature.properties.address_line1 ?: feature.properties.formatted ?: "No address available"

                            // Use different icons from R.drawable based on category
                            icon = when (selectedCategory) {
                                "Hospitals" -> ContextCompat.getDrawable(context, com.example.daktarsaab.R.drawable.baseline_local_hospital_24)?.apply {
                                    setTint(ContextCompat.getColor(context, com.example.daktarsaab.R.color.hospital_color))
                                }
                                "Pharmacies" -> ContextCompat.getDrawable(context, com.example.daktarsaab.R.drawable.baseline_local_pharmacy_24)?.apply {
                                    setTint(ContextCompat.getColor(context, com.example.daktarsaab.R.color.pharmacy_color))
                                }
                                else -> ContextCompat.getDrawable(context, com.example.daktarsaab.R.drawable.baseline_accessibility_24)
                            } ?: ContextCompat.getDrawable(context, com.example.daktarsaab.R.drawable.baseline_accessibility_24)

                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            id = feature.properties.place_id

                            // Update to set the location name when marker is clicked
                            setOnMarkerClickListener { marker, _ ->
                                selectedLocationName = marker.title
                                coroutineScope.launch {
                                    if (!isActive) return@launch // Avoid running in cancelled scope
                                    try {
                                        val placeId = marker.id
                                        if (placeId != null) {
                                            val detailsResponse = geoapifyService.getPlaceDetails(placeId, geoapifyApiKey)
                                            val details = detailsResponse.features.firstOrNull()?.properties
                                            if (details != null) {
                                                placeDetailsTitle = details.name ?: "Place Details"
                                                val addressInfo = details.formatted ?: details.address_line1 ?: "No address available"
                                                val categoryInfo = details.categories?.joinToString(", ") ?: "No category information"
                                                placeDetailsMessage = "Address: $addressInfo\nCategories: $categoryInfo"
                                                showPlaceDetailsDialog = true
                                            } else {
                                                placeDetailsTitle = "Place Details"
                                                placeDetailsMessage = "No details available for this place."
                                                showPlaceDetailsDialog = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MedicalMapScreen", "Error fetching place details: ${e.message}", e)
                                        placeDetailsTitle = "Error"
                                        placeDetailsMessage = "Failed to fetch details: ${e.message}"
                                        showPlaceDetailsDialog = true
                                    }
                                }
                                true
                            }
                        }
                    } else null
                }

                withContext(Dispatchers.Main) {
                    newMarkers.forEach { mapViewRef?.overlays?.add(it) }
                    if (newMarkers.isNotEmpty()) {
                        val boundingBox = BoundingBox.fromGeoPoints(newMarkers.map { it.position })
                        mapViewRef?.zoomToBoundingBox(boundingBox.increaseByScale(1.2f), true, 100)
                    }
                    mapViewRef?.invalidate()
                }
                placeMarkers = newMarkers
            } catch (e: Exception) {
                Log.e("MedicalMapScreen", "Error loading places: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Arrow Icon
                            Icon(
                                painter = painterResource(id = com.example.daktarsaab.R.drawable.baseline_arrow_left_24),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            // Profile with name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Icon(
                                    painter = painterResource(id = com.example.daktarsaab.R.drawable.baseline_person_24),
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        )
                                        .padding(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dikshanta",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { scaffoldPaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else scaffoldPaddingValues)
        ) {
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SearchField(
                        query = startSearchQuery,
                        onQueryChange = {
                            startSearchQuery = it
                            coroutineScope.launch {
                                if (it.length >= 3) {
                                    try {
                                        val suggestions = geoapifyService.getAddressSuggestions(
                                            text = it,
                                            apiKey = geoapifyApiKey
                                        )
                                        startSuggestions = suggestions.features
                                        showStartSuggestions = true
                                    } catch (e: Exception) {
                                        Log.e("MedicalMapScreen", "Error fetching suggestions: ${e.message}", e)
                                    }
                                } else {
                                    startSuggestions = emptyList()
                                    showStartSuggestions = false
                                }
                            }
                        },
                        label = "Start Address",
                        suggestions = startSuggestions,
                        onSuggestionClick = { feature ->
                            startSearchQuery = feature.properties.formatted
                            startSuggestions = emptyList()
                            showStartSuggestions = false
                        },
                        showSuggestions = showStartSuggestions
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SearchField(
                        query = destinationSearchQuery,
                        onQueryChange = {
                            destinationSearchQuery = it
                            coroutineScope.launch {
                                if (it.length >= 3) {
                                    try {
                                        val suggestions = geoapifyService.getAddressSuggestions(
                                            text = it,
                                            apiKey = geoapifyApiKey
                                        )
                                        destinationSuggestions = suggestions.features
                                        showDestinationSuggestions = true
                                    } catch (e: Exception) {
                                        Log.e("MedicalMapScreen", "Error fetching suggestions: ${e.message}", e)
                                    }
                                } else {
                                    destinationSuggestions = emptyList()
                                    showDestinationSuggestions = false
                                }
                            }
                        },
                        label = "Destination Address",
                        suggestions = destinationSuggestions,
                        onSuggestionClick = { feature ->
                            destinationSearchQuery = feature.properties.formatted
                            destinationSuggestions = emptyList()
                            showDestinationSuggestions = false
                        },
                        showSuggestions = showDestinationSuggestions
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Spacer before Search Route Button

                    // Conditional Search Route Button
                    if (startSearchQuery.isNotBlank() && destinationSearchQuery.isNotBlank()) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        // First, geocode both addresses to get coordinates
                                        val startResponse = geoapifyService.searchGeocode(
                                            text = startSearchQuery,
                                            apiKey = geoapifyApiKey
                                        )
                                        val endResponse = geoapifyService.searchGeocode(
                                            text = destinationSearchQuery,
                                            apiKey = geoapifyApiKey
                                        )

                                        val startFeature = startResponse.features.firstOrNull()
                                        val endFeature = endResponse.features.firstOrNull()

                                        if (startFeature != null && endFeature != null) {
                                            val startLat = startFeature.properties.lat
                                            val startLon = startFeature.properties.lon
                                            val endLat = endFeature.properties.lat
                                            val endLon = endFeature.properties.lon

                                            if (startLat != null && startLon != null && endLat != null && endLon != null) {
                                                val startPoint = GeoPoint(startLat, startLon)
                                                val endPoint = GeoPoint(endLat, endLon)

                                                // Now get the route
                                                val waypoints = "$startLat,$startLon|$endLat,$endLon"
                                                val routeResponse = geoapifyService.getRoute(
                                                    waypoints = waypoints,
                                                    mode = "drive",
                                                    apiKey = geoapifyApiKey
                                                )

                                                val routeFeature = routeResponse.features.firstOrNull()
                                                if (routeFeature != null) {
                                                    val geometry = routeFeature.geometry
                                                    val coordinates = geometry.coordinates ?: emptyList()

                                                    displayRoute(
                                                        startPoint = startPoint,
                                                        endPoint = endPoint,
                                                        geometry = geometry,
                                                        rawCoords = coordinates,
                                                        startAddress = startSearchQuery,
                                                        endAddress = destinationSearchQuery
                                                    )
                                                } else {
                                                    Log.e("MedicalMapScreen", "No route found")
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MedicalMapScreen", "Error finding route: ${e.message}", e)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search Route")
                        }
                        Spacer(modifier = Modifier.height(16.dp)) // Spacer after Search Route Button, before categories
                    }

                    CategorySelection(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            selectedCategory = category
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
            ) {
                if (currentLocation != null) {
                    AndroidView(
                        factory = { context ->
                            MapView(context).apply {
                                mapViewRef = this
                                setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                                setMultiTouchControls(true)
                                controller.setZoom(normalZoomLevel)
                                controller.setCenter(currentLocation)
                            }
                        },
                        update = { mapView ->
                            mapViewRef = mapView
                            mapView.overlays.clear()

                            // Add current location marker
                            currentLocation?.let {
                                val currentLocationMarker = Marker(mapView).apply {
                                    position = it
                                    title = "Your Location"
                                    // Use the standard My Location icon and tint it
                                    icon = ContextCompat.getDrawable(context, com.example.daktarsaab.R.drawable.baseline_accessibility_24)?.apply {
                                        setTint(ContextCompat.getColor(context, android.R.color.black))
                                    }
                                    // Anchor bottom center so the icon tip points to the location
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    setOnMarkerClickListener { marker, _ ->
                                        selectedLocationName = "Your Current Location"
                                        true
                                    }
                                }
                                mapView.overlays.add(currentLocationMarker)
                                // Center and zoom map on current location
                                mapView.controller.setCenter(it)
                                mapView.controller.setZoom(normalZoomLevel)
                            }

                            // Add place markers
                            placeMarkers.forEach { mapView.overlays.add(it) }

                            // Add route polyline if it exists
                            routePolyline?.let { mapView.overlays.add(it) }
                            // Add start and end markers for the route if they exist
                            startMarkerState?.let { mapView.overlays.add(it) }
                            endMarkerState?.let { mapView.overlays.add(it) }

                            mapView.invalidate()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isFullscreen) 8.dp else 16.dp)
                        .align(Alignment.TopStart)
                ) {
                    if (selectedLocationName.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier.padding(top = if (isFullscreen) 48.dp else 8.dp)
                        ) {
                            Text(
                                text = selectedLocationName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { isFullscreen = !isFullscreen },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isFullscreen)
                                com.example.daktarsaab.R.drawable.baseline_close_fullscreen_24
                            else
                                com.example.daktarsaab.R.drawable.baseline_open_in_full_24
                        ),
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen"
                    )
                }
            }
        } // End of main Column

        // AlertDialog for Place Details
        if (showPlaceDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showPlaceDetailsDialog = false },
                title = { Text(placeDetailsTitle) },
                text = { Text(placeDetailsMessage) },
                confirmButton = {
                    Button(onClick = { showPlaceDetailsDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    } // End of Scaffold content lambda
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    suggestions: List<GeoapifyFeature>,
    onSuggestionClick: (GeoapifyFeature) -> Unit,
    showSuggestions: Boolean
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        if (showSuggestions && suggestions.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = { onSuggestionClick(suggestion) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(suggestion: GeoapifyFeature, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = suggestion.properties.formatted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun CategorySelection(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Hospitals", "Pharmacies")
    val rowSize = 2 // Number of items per row

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Find Nearby:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Display categories in rows with rowSize items per row
        categories.chunked(rowSize).forEachIndexed { index, rowCategories ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { category ->
                    CategoryButton(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Add placeholders for incomplete rows to maintain layout
                repeat(rowSize - rowCategories.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer
        ),
        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (category) {
                    "Hospitals" -> Icons.Default.LocalHospital
                    "Pharmacies" -> Icons.Default.LocalPharmacy
                    else -> Icons.Default.LocalHospital
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = category)
        }
    }
}

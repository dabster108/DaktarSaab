package com.example.daktarsaab

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import org.osmdroid.util.BoundingBox
import android.graphics.Color as AndroidColor

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
            DaktarSaabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MedicalMapScreen(
                        onLocationPermissionRequested = {
                            Log.d("MapsActivity", "Requesting location permission via launcher.")
                            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        permissionSignal = permissionGrantedSignal
                    )
                }
            }
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

    val geoapifyApiKey = BuildConfig.GEOAPIFY_API_KEY // Use API key from BuildConfig
    var permissionCheckedInitially by remember { mutableStateOf(false) }
    var initialZoomDone by remember { mutableStateOf(false) }

    val normalZoomLevel = 15.0
    val fullscreenZoomLevel = normalZoomLevel * 1.2 // 40% increase

    val geoapifyService = remember { GeoapifyService.create() }

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

                val categoryMapping = mapOf<String, String>(
                    "Hospitals" to "healthcare.hospital",
                    "Pharmacies" to "healthcare.pharmacy",
                    "Clinics" to "healthcare.clinic",
                    "Gas Stations" to "commercial.fuel.filling_station"
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
                            icon = when (selectedCategory) {
                                "Hospitals" -> ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass)?.apply {
                                    setTint(ContextCompat.getColor(context, R.color.hospital_color))
                                }
                                "Pharmacies" -> ContextCompat.getDrawable(context, android.R.drawable.ic_menu_add)?.apply {
                                    setTint(ContextCompat.getColor(context, R.color.pharmacy_color))
                                }
                                "Clinics" -> ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)?.apply {
                                    setTint(ContextCompat.getColor(context, R.color.clinic_color))
                                }
                                "Gas Stations" -> ContextCompat.getDrawable(context, android.R.drawable.ic_menu_directions)?.apply {
                                    setTint(ContextCompat.getColor(context, R.color.gas_station_color))
                                }
                                else -> ContextCompat.getDrawable(context, android.R.drawable.ic_menu_search)
                            } ?: ContextCompat.getDrawable(context, android.R.drawable.ic_menu_info_details) // Fallback icon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            id = feature.properties.place_id // For place details lookup
                            setOnMarkerClickListener { marker, _ ->
                                coroutineScope.launch {
                                    if (!isActive) return@launch // Avoid running in cancelled scope
                                    try {
                                        val placeId = marker.id
                                        if (placeId != null) {
                                            val detailsResponse = geoapifyService.getPlaceDetails(placeId, geoapifyApiKey)
                                            val details = detailsResponse.features.firstOrNull()?.properties
                                            if (details != null) {
                                                val detailsTextBuilder = StringBuilder().apply {
                                                    details.name?.let { append("Name: $it\n") }
                                                    details.categories?.let { append("Type: ${it.joinToString(", ")}\n") }
                                                    details.formatted?.let { append("Address: $it\n") }
                                                }
                                                placeDetailsTitle = "Place Details"
                                                placeDetailsMessage = detailsTextBuilder.toString().ifEmpty { "No details available" }
                                                showPlaceDetailsDialog = true
                                            } else {
                                                placeDetailsTitle = "Error"
                                                placeDetailsMessage = "No details found for this place."
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
                                true // Consume the click event
                            }
                        }
                    } else null
                }

                withContext(Dispatchers.Main) {
                    newMarkers.forEach { mapViewRef?.overlays?.add(it) }
                    if (newMarkers.isNotEmpty()) {
                        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(newMarkers.map { it.position })
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
                    title = { Text("Doctor Saab Maps") }
                )
            }
        }
    ) { scaffoldPaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else scaffoldPaddingValues) // Corrected padding usage
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
                                        // 1. Geocode start and destination addresses
                                        val startResult = geoapifyService.searchGeocode(text = startSearchQuery, apiKey = geoapifyApiKey, limit = 1)
                                        val endResult = geoapifyService.searchGeocode(text = destinationSearchQuery, apiKey = geoapifyApiKey, limit = 1)

                                        val startPoint = startResult.features.firstOrNull()?.geometry?.coordinates?.let { GeoPoint(it[1], it[0]) }
                                        val endPoint = endResult.features.firstOrNull()?.geometry?.coordinates?.let { GeoPoint(it[1], it[0]) }

                                        if (startPoint != null && endPoint != null) {
                                            val waypoints = "${startPoint.latitude},${startPoint.longitude}|${endPoint.latitude},${endPoint.longitude}"
                                            Log.d("MedicalMapScreen", "Fetching route for waypoints: $waypoints")
                                            val routeResult = geoapifyService.getRoute(waypoints = waypoints, mode = "drive", apiKey = geoapifyApiKey)

                                            val feature = routeResult.features.firstOrNull()
                                            val geometry = feature?.geometry
                                            val rawCoords = geometry?.coordinates // This is List<Any>?

                                            if (geometry != null && rawCoords != null) {
                                                val geometryType = geometry.type
                                                val routePoints = mutableListOf<GeoPoint>()

                                                Log.d("MedicalMapScreen", "Attempting to parse route geometry. Type: $geometryType")

                                                try {
                                                    when (geometryType) {
                                                        "MultiLineString" -> {
                                                            (rawCoords as? List<List<List<Double>>>)?.forEach { segment ->
                                                                segment.forEach { pair ->
                                                                    if (pair.size == 2) {
                                                                        routePoints.add(GeoPoint(pair[1], pair[0])) // Geoapify is [lon, lat]
                                                                    } else {
                                                                        Log.w("MedicalMapScreen", "Invalid coordinate pair in MultiLineString segment: $pair")
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        "LineString" -> {
                                                            (rawCoords as? List<List<Double>>)?.forEach { pair ->
                                                                if (pair.size == 2) {
                                                                    routePoints.add(GeoPoint(pair[1], pair[0])) // Geoapify is [lon, lat]
                                                                } else {
                                                                    Log.w("MedicalMapScreen", "Invalid coordinate pair in LineString: $pair")
                                                                }
                                                            }
                                                        }
                                                        else -> {
                                                            Log.e("MedicalMapScreen", "Unknown or null geometry type: $geometryType. Raw coords: $rawCoords")
                                                        }
                                                    }
                                                } catch (e: ClassCastException) {
                                                    Log.e("MedicalMapScreen", "Error parsing route coordinates due to ClassCastException: ${e.message}. Raw coords: $rawCoords", e)
                                                }

                                                Log.d("MedicalMapScreen", "Parsed ${routePoints.size} route points.")

                                                if (routePoints.size >= 2) {
                                                    val newRoutePolyline = Polyline().apply {
                                                        this.points.addAll(routePoints)
                                                        color = AndroidColor.BLUE
                                                        width = 8f
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        // Clear previous route and its markers first from map overlays
                                                        routePolyline?.let { mapViewRef?.overlays?.remove(it) }
                                                        startMarkerState?.let { mapViewRef?.overlays?.remove(it) }
                                                        endMarkerState?.let { mapViewRef?.overlays?.remove(it) }

                                                        // Add new route polyline to map
                                                        mapViewRef?.overlays?.add(newRoutePolyline)
                                                        // Update state
                                                        routePolyline = newRoutePolyline

                                                        // Create and add new start marker
                                                        val newStartMarker = Marker(mapViewRef).apply {
                                                            position = startPoint
                                                            title = "Start: $startSearchQuery"
                                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                            // Optionally, add a specific icon for route start/end
                                                        }
                                                        mapViewRef?.overlays?.add(newStartMarker)
                                                        startMarkerState = newStartMarker // Update state

                                                        // Create and add new end marker
                                                        val newEndMarker = Marker(mapViewRef).apply {
                                                            position = endPoint
                                                            title = "Destination: $destinationSearchQuery"
                                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                        }
                                                        mapViewRef?.overlays?.add(newEndMarker)
                                                        endMarkerState = newEndMarker // Update state

                                                        Log.d("MedicalMapScreen", "Route polyline and markers added to map.")
                                                        val boundingBox = BoundingBox.fromGeoPoints(routePoints)
                                                        mapViewRef?.zoomToBoundingBox(boundingBox.increaseByScale(1.2f), true, 100)
                                                        mapViewRef?.invalidate()
                                                    }
                                                } else {
                                                    Log.e("MedicalMapScreen", "Not enough route points (${routePoints.size}) to draw polyline. Clearing old route.")
                                                    withContext(Dispatchers.Main) {
                                                        routePolyline?.let { mapViewRef?.overlays?.remove(it) }
                                                        routePolyline = null // Clear from state
                                                        // Decide if start/end markers should also be cleared if no route line
                                                        // startMarkerState?.let { mapViewRef?.overlays?.remove(it); startMarkerState = null }
                                                        // endMarkerState?.let { mapViewRef?.overlays?.remove(it); endMarkerState = null }
                                                        mapViewRef?.invalidate()
                                                    }
                                                }
                                            } else {
                                                Log.e("MedicalMapScreen", "No route geometry or coordinates found in API response.")
                                                withContext(Dispatchers.Main) {
                                                    routePolyline?.let { mapViewRef?.overlays?.remove(it) }
                                                    routePolyline = null // Clear from state
                                                    mapViewRef?.invalidate()
                                                }
                                            }
                                        } else {
                                            Log.e("MedicalMapScreen", "Could not geocode start or end point for routing.")
                                            // Optionally, inform user via Toast or Dialog
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MedicalMapScreen", "Error during route search: ${e.message}", e)
                                        // Optionally, show a toast or dialog to the user
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth() // Adjusted modifier
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
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            mapViewRef = this
                            setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            if (currentLocation != null) {
                                controller.setCenter(currentLocation)
                            }
                        }
                    },
                    update = { mapView ->
                        mapViewRef = mapView
                        mapView.overlays.clear()

                        // Add current location marker
                        currentLocation?.let {
                            val currentLocationMarker = Marker(mapView).apply {
                                position = it
                                title = "Current Location"
                                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(currentLocationMarker)
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

                FloatingActionButton(
                    onClick = { isFullscreen = !isFullscreen },
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Respecting user's current Alignment.TopEnd
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (isFullscreen)
                                R.drawable.baseline_close_fullscreen_24
                            else
                                R.drawable.baseline_open_in_full_24
                        ),
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen" // Added content description
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
            modifier = Modifier
                .fillMaxWidth()
            // .padding(8.dp) // Padding handled by parent Column
        )

        if (showSuggestions && suggestions.isNotEmpty()) { // Ensure suggestions list is not empty
            ElevatedCard(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) { // Limit height of suggestions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
            .padding(vertical = 4.dp, horizontal = 8.dp), // Add padding within the Surface
        color = MaterialTheme.colorScheme.surfaceVariant, // Use a subtle background color
        shape = RoundedCornerShape(4.dp) // Add rounded corners
    ) {
        Text(
            text = suggestion.properties.formatted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(8.dp) // Add padding to the text itself
        )
    }
}

@Composable
fun CategorySelection(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Hospitals", "Pharmacies", "Clinics", "Gas Stations")

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Find Nearby:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.take(2).forEach { category ->
                ElevatedButton(
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (selectedCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selectedCategory == category) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(category, maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.drop(2).forEach { category ->
                ElevatedButton(
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (selectedCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selectedCategory == category) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(category, maxLines = 1)
                }
            }
        }
    }
}


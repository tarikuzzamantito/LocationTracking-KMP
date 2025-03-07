import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jordond.compass.Coordinates
import dev.jordond.compass.Place
import dev.jordond.compass.Priority
import dev.jordond.compass.autocomplete.Autocomplete
import dev.jordond.compass.autocomplete.mobile
import dev.jordond.compass.geocoder.MobileGeocoder
import dev.jordond.compass.geocoder.placeOrNull
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.TrackingStatus
import dev.jordond.compass.geolocation.isPermissionDeniedForever
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import locationtracking_kmp.composeapp.generated.resources.Res
import locationtracking_kmp.composeapp.generated.resources.compose_multiplatform

//https://www.youtube.com/watch?v=JexqGWoH72g&ab_channel=Stevdza-San
//https://github.com/stevdza-san/LocationTracking-KMP
@Composable
@Preview
fun App() {
    MaterialTheme {
        /*val geoLocation = remember { Geolocator.mobile() }
        LaunchedEffect(Unit) {
            when (val result = geoLocation.current()) {
                is GeolocatorResult.Success -> {
                    println("LOCATION: ${result.data.coordinates}")
                    println("LOCATION Name: ${MobileGeocoder().placeOrNull(result.data.coordinates)?.locality}")

                }

                is GeolocatorResult.Error -> when (result) {
                    is GeolocatorResult.NotSupported -> println("LOCATION ERROR ${result.message}")
                    is GeolocatorResult.NotFound -> println("LOCATION ERROR ${result.message}")
                    is GeolocatorResult.PermissionError -> println("LOCATION ERROR ${result.message}")
                    is GeolocatorResult.GeolocationFailed -> println("LOCATION ERROR ${result.message}")
                    else -> println("LOCATION ERROR ${result.message}")
                }
            }
        }*/

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LocationTracking()

        }


        /*var showContent by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }*/
    }
}

@Composable
fun LocationTracking() {
    val scope = rememberCoroutineScope()
    val geoLocator = remember { Geolocator.mobile() }
    val trackingStatus by geoLocator.trackingStatus.collectAsState(initial = null)

    var currentLocation: Coordinates? by remember {
        mutableStateOf(null)
    }
    var currentCity: String? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(Unit) {
        geoLocator.locationUpdates.collectLatest {
            currentLocation = it.coordinates
            currentCity = MobileGeocoder().placeOrNull(it.coordinates)?.locality
        }
    }

    LaunchedEffect(Unit) {
        geoLocator.trackingStatus.collectLatest { status ->
            when (status) {
                is TrackingStatus.Idle -> {}
                is TrackingStatus.Tracking -> {}
                is TrackingStatus.Update -> {}
                is TrackingStatus.Error -> {
                    val error: GeolocatorResult.Error = status.cause
                    println("TRACKING ERROR: $error")
                    // Show the permissions settings screen
                    val permissionDeniedForever = error.isPermissionDeniedForever()
                    println("TRACKING PERMISSION DENIED: $permissionDeniedForever")
                }
            }
        }
    }

    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = currentCity ?: "Waiting..."
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = "LAT: ${currentLocation?.latitude}\nLNG: ${currentLocation?.longitude}"
    )
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            enabled = trackingStatus == TrackingStatus.Idle,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    geoLocator.startTracking(
                        LocationRequest(
                            Priority.HighAccuracy
                        )
                    )
                }
            }
        ) {
            Text(text = "Start")
        }
        Spacer(modifier = Modifier.width(20.dp))
        Button(
            enabled = trackingStatus != TrackingStatus.Idle,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    geoLocator.stopTracking()
                    currentLocation = null
                    currentCity = null
                }
            }
        ) {
            Text(text = "Stop")
        }
    }
}

@Composable
fun PlaceAutocomplete() {
    val scope = rememberCoroutineScope()
    val autocomplete = remember { Autocomplete.mobile() }
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val places = remember { mutableStateListOf<Place?>() }
    var selectedPlace: Place? by remember { mutableStateOf(null) }

    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = if (selectedPlace != null) "${selectedPlace?.locality} (${selectedPlace?.country})\n" +
                "LAT: ${selectedPlace?.coordinates?.latitude}\n" +
                "LAT: ${selectedPlace?.coordinates?.longitude}\n"
        else "No place selected."
    )
    Spacer(modifier = Modifier.height(20.dp))
/*    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {}
    ) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            heyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    scope.launch {
                        autocomplete.search(searchQuery).getOrNull().let {
                            places.clear()
                            places.addAll(it?.toList() ?: emptyList())
                        }
                    }
                    expanded = !expanded
                }
            )

        )
    }*/
}


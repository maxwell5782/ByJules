package com.example.mocklocation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity() {

    private lateinit var mMap: MapView
    private var selectedLocation: GeoPoint? = null
    private var currentMarker: Marker? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private val pathPoints = mutableListOf<GeoPoint>()
    private val pathMarkers = mutableListOf<Marker>()
    private var isMoving = false
    private val handler = Handler(Looper.getMainLooper())
    private var movementRunnable: Runnable? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_main)

        mMap = findViewById(R.id.map)
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.setMultiTouchControls(true)

        val mapController = mMap.controller
        mapController.setZoom(10.0)
        val startPointInit = GeoPoint(25.0330, 121.5654)
        mapController.setCenter(startPointInit)

        // Set up click listener
        val receiveEvents = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                selectedLocation = p
                currentMarker?.let { mMap.overlays.remove(it) }
                currentMarker = Marker(mMap).apply {
                    position = p
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Selected Location"
                }
                mMap.overlays.add(currentMarker)
                mMap.invalidate()
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        mMap.overlays.add(MapEventsOverlay(receiveEvents))

        // Try to show current location if permission is already granted
        if (checkLocationPermission()) {
            enableMyLocation()
        }

        val btnSetLocation: Button = findViewById(R.id.btnSetLocation)
        btnSetLocation.setOnClickListener {
            if (checkLocationPermission()) {
                selectedLocation?.let {
                    setMockLocation(it, true)
                } ?: run {
                    Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestLocationPermission()
            }
        }

        findViewById<Button>(R.id.btnAddPoint).setOnClickListener {
            selectedLocation?.let {
                pathPoints.add(it)
                val marker = Marker(mMap).apply {
                    position = it
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Point ${pathPoints.size}"
                }
                mMap.overlays.add(marker)
                pathMarkers.add(marker)
                mMap.invalidate()
            } ?: Toast.makeText(this, "Select a point on map first", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnClearPath).setOnClickListener {
            stopPathMovement()
            pathPoints.clear()
            pathMarkers.forEach { mMap.overlays.remove(it) }
            pathMarkers.clear()
            mMap.invalidate()
            Toast.makeText(this, "Path cleared", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStartPath).setOnClickListener {
            if (isMoving) {
                stopPathMovement()
            } else {
                startPathMovement()
            }
        }
    }


    private fun startPathMovement() {
        if (pathPoints.size < 2) {
            Toast.makeText(this, "Set at least 2 points", Toast.LENGTH_SHORT).show()
            return
        }

        val speedKmH = findViewById<EditText>(R.id.etSpeed).text.toString().toDoubleOrNull() ?: 60.0
        if (speedKmH <= 0) {
            Toast.makeText(this, "Invalid speed", Toast.LENGTH_SHORT).show()
            return
        }
        val speedMS = speedKmH / 3.6

        isMoving = true
        findViewById<Button>(R.id.btnStartPath).text = getString(R.string.stop_path)

        var currentIdx = 0
        var segmentStartPoint = pathPoints[0]
        var segmentEndPoint = pathPoints[1]
        var segmentDistance = segmentStartPoint.distanceToAsDouble(segmentEndPoint)
        var segmentDuration = (segmentDistance / speedMS * 1000).toLong()
        var segmentStartTime = SystemClock.elapsedRealtime()

        movementRunnable = object : Runnable {
            override fun run() {
                if (!isMoving || pathPoints.size < 2) return

                val now = SystemClock.elapsedRealtime()
                var elapsedInSegment = now - segmentStartTime

                while (elapsedInSegment >= segmentDuration && isMoving) {
                    currentIdx++
                    if (currentIdx >= pathPoints.size - 1) {
                        if (pathPoints.isNotEmpty()) {
                            setMockLocation(pathPoints.last(), false)
                        }
                        stopPathMovement()
                        Toast.makeText(this@MainActivity, "Path completed", Toast.LENGTH_SHORT).show()
                        return
                    }
                    // Start next segment
                    segmentStartTime += segmentDuration
                    segmentStartPoint = pathPoints[currentIdx]
                    segmentEndPoint = pathPoints[currentIdx + 1]
                    segmentDistance = segmentStartPoint.distanceToAsDouble(segmentEndPoint)
                    segmentDuration = (segmentDistance / speedMS * 1000).toLong()
                    elapsedInSegment = now - segmentStartTime
                }

                if (isMoving) {
                    val fraction = if (segmentDuration > 0) elapsedInSegment.toDouble() / segmentDuration else 1.0
                    val currentPoint = LocationUtils.interpolate(segmentStartPoint, segmentEndPoint, fraction)
                    setMockLocation(currentPoint, false)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(movementRunnable!!)
    }

    private fun stopPathMovement() {
        isMoving = false
        movementRunnable?.let { handler.removeCallbacks(it) }
        movementRunnable = null
        findViewById<Button>(R.id.btnStartPath).text = getString(R.string.start_path)
    }


    private fun enableMyLocation() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mMap)
        myLocationOverlay?.enableMyLocation()
        mMap.overlays.add(myLocationOverlay)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. You can now set the location.", Toast.LENGTH_SHORT).show()
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permission denied. Cannot set mock location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMap.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPathMovement()
    }

    private fun setMockLocation(geoPoint: GeoPoint, showToast: Boolean) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providerName = LocationManager.GPS_PROVIDER

        try {
            // Check if provider exists, if not add it
            try {
                locationManager.addTestProvider(
                    providerName,
                    false, false, false, false, true, true, true,
                    0, 5
                )
            } catch (e: IllegalArgumentException) {
                // Provider already exists
            }

            locationManager.setTestProviderEnabled(providerName, true)

            val mockLocation = Location(providerName).apply {
                latitude = geoPoint.latitude
                longitude = geoPoint.longitude
                altitude = 0.0
                time = System.currentTimeMillis()
                accuracy = 1.0f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
            }

            locationManager.setTestProviderLocation(providerName, mockLocation)
            if (showToast) {
                Toast.makeText(this, "Location set to ${geoPoint.latitude}, ${geoPoint.longitude}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Security Exception: Make sure this app is set as the Mock Location App in Developer Options", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            if (showToast) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

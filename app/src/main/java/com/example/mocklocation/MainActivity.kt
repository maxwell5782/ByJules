package com.example.mocklocation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val lastLat = prefs.getFloat("last_lat", 25.0330f).toDouble()
        val lastLon = prefs.getFloat("last_lon", 121.5654f).toDouble()
        val startPointInit = GeoPoint(lastLat, lastLon)
        mapController.setCenter(startPointInit)

        // Add a marker at the last location if it was saved
        if (prefs.contains("last_lat")) {
            selectedLocation = startPointInit
            currentMarker = Marker(mMap).apply {
                position = startPointInit
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Last Location"
            }
            mMap.overlays.add(currentMarker)
        }

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
                    icon = createMarkerIcon(pathPoints.size - 1)
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

        val spinnerSlots: Spinner = findViewById(R.id.spinnerSlots)
        findViewById<Button>(R.id.btnSavePath).setOnClickListener {
            savePath(spinnerSlots.selectedItemPosition)
        }
        findViewById<Button>(R.id.btnLoadPath).setOnClickListener {
            loadPath(spinnerSlots.selectedItemPosition)
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

    private fun savePath(slot: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val gson = Gson()
        val json = gson.toJson(pathPoints)
        prefs.edit().putString("path_slot_$slot", json).apply()
        Toast.makeText(this, "Path saved to Slot ${slot + 1}", Toast.LENGTH_SHORT).show()
    }

    private fun loadPath(slot: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val json = prefs.getString("path_slot_$slot", null)
        if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<List<GeoPoint>>() {}.type
            val points: List<GeoPoint> = gson.fromJson(json, type)

            // Clear current path
            stopPathMovement()
            pathPoints.clear()
            pathMarkers.forEach { mMap.overlays.remove(it) }
            pathMarkers.clear()

            // Load points
            points.forEach {
                pathPoints.add(it)
                val marker = Marker(mMap).apply {
                    position = it
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createMarkerIcon(pathPoints.size - 1)
                    title = "Point ${pathPoints.size}"
                }
                mMap.overlays.add(marker)
                pathMarkers.add(marker)
            }
            mMap.invalidate()
            Toast.makeText(this, "Path loaded from Slot ${slot + 1}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No path saved in Slot ${slot + 1}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createMarkerIcon(index: Int): Drawable {
        val size = 60
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw red circle
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw white text
        paint.color = Color.WHITE
        paint.textSize = 36f
        paint.textAlign = Paint.Align.CENTER
        val text = (index + 1).toString()
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, size / 2f, size / 2f + bounds.height() / 2f, paint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun setMockLocation(geoPoint: GeoPoint, showToast: Boolean) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providerName = LocationManager.GPS_PROVIDER

        // Save last location
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .putFloat("last_lat", geoPoint.latitude.toFloat())
            .putFloat("last_lon", geoPoint.longitude.toFloat())
            .apply()

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

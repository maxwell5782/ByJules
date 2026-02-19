package com.example.mocklocation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Marker

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private var currentMarker: Marker? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnSetLocation: Button = findViewById(R.id.btnSetLocation)
        btnSetLocation.setOnClickListener {
            if (checkLocationPermission()) {
                selectedLocation?.let {
                    setMockLocation(it)
                } ?: run {
                    Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestLocationPermission()
            }
        }
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
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
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
                if (::mMap.isInitialized) {
                    try {
                        mMap.isMyLocationEnabled = true
                    } catch (e: SecurityException) {
                        // Permission was just granted, but catch just in case
                    }
                }
            } else {
                Toast.makeText(this, "Permission denied. Cannot set mock location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check if API Key is set
        if (getString(R.string.google_maps_key) == "YOUR_API_KEY_HERE") {
            Toast.makeText(this, "Please set your Google Maps API key in google_maps_api.xml", Toast.LENGTH_LONG).show()
        }

        // Try to show current location if permission is already granted
        if (checkLocationPermission()) {
            try {
                mMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                // Should not happen if check passed
            }
        }

        // Move camera to a default location (Taipei) to avoid starting at (0,0)
        val defaultLocation = LatLng(25.0330, 121.5654)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        mMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            currentMarker?.remove()
            currentMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        }
    }

    private fun setMockLocation(latLng: LatLng) {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
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
                latitude = latLng.latitude
                longitude = latLng.longitude
                altitude = 0.0
                time = System.currentTimeMillis()
                accuracy = 1.0f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
            }

            locationManager.setTestProviderLocation(providerName, mockLocation)
            Toast.makeText(this, "Location set to ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Security Exception: Make sure this app is set as the Mock Location App in Developer Options", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

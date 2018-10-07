package ua.lviv.iot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.utils.*
import android.content.res.Resources
import android.util.Log
import com.google.android.gms.maps.model.MapStyleOptions


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val mTAG = "MainActivity"

    private lateinit var mMapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMapView = findViewById(R.id.mapView)
        mMapView.onCreate(savedInstanceState)
        mMapView.onResume()
        try {
            MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mMapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap == null) {
            Log.e(mTAG, "Map is not initialized")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }

        // Customise the map style
        try {
            val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_json))
            if (!success) Log.e(mTAG, "Style parsing failed.")

        } catch (e: Resources.NotFoundException) {
            Log.e(mTAG, "Can't find style. Error: ", e)
        }

        // Moving camera to Lviv position
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(LVIV_LAT, LVIV_LNG), 17f))
    }
}

package ua.lviv.iot.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.FirebaseApp
import ua.lviv.iot.R
import ua.lviv.iot.ui.questsMenu.QuestsMenuActivity
import ua.lviv.iot.ui.user.UserActivity
import ua.lviv.iot.utils.LVIV_LAT
import ua.lviv.iot.utils.LVIV_LNG


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val mTAG = "MainActivity"

    private lateinit var mMapView: MapView
    private lateinit var mQuestsBtn: Button
    private lateinit var mBalanceBtn: Button
    private lateinit var mProfileBtn: Button
    private lateinit var mRatingBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        mMapView = findViewById(R.id.mapview_main)
        mQuestsBtn = findViewById(R.id.btn_main_quests)
        mBalanceBtn = findViewById(R.id.btn_main_balance)
        mProfileBtn = findViewById(R.id.btn_main_profile)
        mRatingBtn = findViewById(R.id.btn_main_rating)

        mMapView.onCreate(savedInstanceState)
        mMapView.onResume()
        try {
            MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mMapView.getMapAsync(this)

        mQuestsBtn.setOnClickListener {
            startActivity(Intent(this, QuestsMenuActivity::class.java))
        }

        val nextIntent = Intent(this, UserActivity::class.java)

        mBalanceBtn.setOnClickListener { startActivity(nextIntent.putExtra("fragment", "balance")) }
        mProfileBtn.setOnClickListener { startActivity(nextIntent.putExtra("fragment", "profile")) }
        mRatingBtn.setOnClickListener { startActivity(nextIntent.putExtra("fragment", "rating")) }
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

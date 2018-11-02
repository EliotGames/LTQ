package ua.lviv.iot.ui.quest

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.location.Location
import android.location.LocationListener
import com.androidmapsextensions.Marker
import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.map.LocationStructure
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import android.location.LocationManager
import android.util.Log
import ua.lviv.iot.model.map.UserLocationManager


class QuestViewModel(): ViewModel(){

    private val DEFAULT_LATITUDE = 49.841787
    private val DEFAULT_LONGITUDE = 24.031686
    private val defaultLatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    private val markersList = ArrayList<Marker>()
    private var origin: LatLng? = null
    private var counter: Int = 0
    private val polylinesList = ArrayList<LatLng>()
    private var data = ArrayList<LatLng>()
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private var dest: LatLng? = null
    private var requestList = ArrayList<RequestClass>()
    private var requestIndex = 0
    private val distanceList = ArrayList<String>()
    private var locationListFromDatabase:  List<LocationStructure>? = null
    var userCurrentLocation = MutableLiveData<LatLng>().default(defaultLatLng)


    fun checkLocationUpdates(locationSystemService: Any) {
        UserLocationManager(locationSystemService).checkLocationUpdates(object : UserLocationManager.UserLocationListener {
            override fun onSuccess(location: Location) {
                userCurrentLocation.value = getMyLocation(location)
            }

            override fun onError() {
                Log.e("UserLocation", "Some problems with user location!")
            }

        })
    }

    private fun getMyLocation(location: Location): LatLng {
        val latitude = location.latitude
        val longitude = location.longitude
        return LatLng(latitude, longitude)
    }

    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
}


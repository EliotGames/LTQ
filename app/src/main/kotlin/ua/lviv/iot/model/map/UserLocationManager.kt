package ua.lviv.iot.model.map

import android.annotation.SuppressLint
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlin.math.pow
import kotlin.math.sqrt


class UserLocationManager(locationSystemService: Any) {

    private var DEFAULT_VALUE_LATLNG = LatLng(0.0, 0.0)
    private var previousLatLng = DEFAULT_VALUE_LATLNG
    private var coordinateJumpCounter = 0

    private var locationManager: LocationManager? = locationSystemService as LocationManager

    private var isNetworkProviderEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    private fun chooseLocationManagerType() : String {
        return if(isNetworkProviderEnabled) {
            LocationManager.NETWORK_PROVIDER
        }
        else {
            LocationManager.GPS_PROVIDER
        }
    }

    @SuppressLint("MissingPermission")
    fun checkLocationUpdates(listener: UserLocationManager.UserLocationListener) {
        locationManager?.requestLocationUpdates(chooseLocationManagerType(), 10000, 0.toFloat(), object : LocationListener {
            override fun onLocationChanged(p0: Location?) {
                if(p0 == null) {
                    listener.onError()
                }
                else {
                    listener.onSuccess(getMyLocation(p0))
                }
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
                Log.e("location request", "onStatusChenged")
            }

            override fun onProviderEnabled(p0: String?) {
                Log.e("location request", "onProviderEnabled")
            }

            override fun onProviderDisabled(p0: String?) {
                Log.e("location request", "onProviderDisabled")
            }

        })

    }

    //check location jumping ???
    /*private fun markerJumpCheck(currentLocation: Location) : LatLng {
        var currentLatLng = getMyLocation(currentLocation)
        when {
            previousLatLng == DEFAULT_VALUE_LATLNG -> {
                previousLatLng = currentLatLng
                return currentLatLng
            }
            isCheckInAvailable(currentLatLng, previousLatLng) <= 5 -> {
                previousLatLng = currentLatLng
                return currentLatLng
            }
            coordinateJumpCounter > 3 -> {
                coordinateJumpCounter = 0
                previousLatLng = currentLatLng
                return currentLatLng
            }
            else -> {
                coordinateJumpCounter++
                return previousLatLng
            }
        }
    }*/

    //check

    interface UserLocationListener {
        fun onSuccess(latLng: LatLng)

        fun onError()
    }

    companion object {
        //get coordinates from Location and return LatLng
        fun getMyLocation(location: Location): LatLng {
            val latitude = location.latitude
            val longitude = location.longitude
            return LatLng(latitude, longitude)
        }
    }

    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
}
package ua.lviv.iot.model.map

import android.annotation.SuppressLint
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
        locationManager?.requestLocationUpdates(chooseLocationManagerType(), 0, 0.toFloat(), object : LocationListener {
            override fun onLocationChanged(p0: Location?) {
                if(p0 == null) {
                    listener.onError()
                }
                else {
                    listener.onSuccess(getMyLocation(p0))
                }
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            }

            override fun onProviderEnabled(p0: String?) {
            }

            override fun onProviderDisabled(p0: String?) {
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
            getDistance(currentLatLng, previousLatLng) <= 5 -> {
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

        //method to find distance between locations---------------------------------------------------------
        fun rad(x: Double) : Double {
            return x * Math.PI / 180
        }

        fun getDistance(p1 : LatLng, p2 : LatLng) : Double {
            val latitude = p1.latitude - p2.latitude
            val longitude = p2.longitude - p2.longitude
            return sqrt(latitude.pow(2) + longitude.pow(2))
        }
        //---------------------------------------------------------------------------------------------------
    }

    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
}
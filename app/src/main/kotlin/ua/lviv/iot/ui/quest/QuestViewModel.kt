package ua.lviv.iot.ui.quest

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.androidmapsextensions.Marker
import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.map.LocationStructure
import android.util.Log
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.map.LocationManager
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
    private lateinit var locationManager: LocationManager
    var userCurrentLocation = MutableLiveData<LatLng>().default(defaultLatLng)
    var locationForCheckInAvailable = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    var locationHasChecked = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)


    fun checkUserLocationUpdates(locationSystemService: Any) {
        UserLocationManager(locationSystemService).checkLocationUpdates(object : UserLocationManager.UserLocationListener {
            override fun onSuccess(latLng: LatLng) {
                userCurrentLocation.value = latLng
            }

            override fun onError() {
                Log.e("UserLocation", "Some problems with user location!")
            }

        })
    }

    fun locationCheckInListener(locationsList: ArrayList<LatLng>) {
        locationManager = LocationManager(locationsList)
        locationManager.locationCheckInListener(userCurrentLocation.value!!, object: ua.lviv.iot.model.map.LocationManager.LocationCheckInListener{
            override fun onChange(result: EventResultStatus) {
                locationForCheckInAvailable.value = result
            }

        })
    }

    fun activateCheckIn(questName: String) {
        locationManager.checkInLocation(questName, object : LocationManager.OnLocationChecked {
            override fun onError(result: EventResultStatus) {
                when(result) {
                    EventResultStatus.EVENT_SUCCESS -> Log.e("CheckIn", "getting value is not value we need!")
                    EventResultStatus.NO_EVENT -> Log.e("CheckIn", "firebase call cancelled!")
                    EventResultStatus.EVENT_FAILED -> Log.e("CheckIn", "firebase call returns null!")
                }
                locationHasChecked.value = EventResultStatus.EVENT_FAILED
            }
            override fun onSuccess() {
                locationHasChecked.value = EventResultStatus.EVENT_SUCCESS
                TODO()//Other activity after location checIn
            }

        })
    }


    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
}


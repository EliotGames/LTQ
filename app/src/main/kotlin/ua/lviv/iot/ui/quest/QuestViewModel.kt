package ua.lviv.iot.ui.quest

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.androidmapsextensions.Marker
import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.map.LocationStructure
import android.util.Log
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.firebase.Repository
import ua.lviv.iot.model.map.LocationManager
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.google.firebase.database.DatabaseError
import ua.lviv.iot.model.map.Quest
import ua.lviv.iot.model.map.UserLocationManager


class QuestViewModel : ViewModel(), DirectionCallback {
    private val polylinesList = ArrayList<ArrayList<LatLng>>()
    private val prePolylinesList = ArrayList<ArrayList<LatLng>>()
    val polylinesLiveData = MutableLiveData<ArrayList<ArrayList<LatLng>>>()
    private var counter: Int = 0
    private val DEFAULT_LATITUDE = 49.841787
    private val DEFAULT_LONGITUDE = 24.031686
    private val defaultLatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    private val markersList = ArrayList<Marker>()
    private var origin: LatLng? = null
    private var data = ArrayList<LatLng>()
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private var dest: LatLng? = null
    private var requestIndex = 0
    private val distanceList = ArrayList<ArrayList<String>>()
    private val predistanceList = ArrayList<ArrayList<String>>()
    val distanceLiveData = MutableLiveData<ArrayList<ArrayList<String>>>()
    private var locationListFromDatabase = mutableListOf<LocationStructure>()
    val locationLiveData = MutableLiveData<List<LocationStructure>>()
    private val repository = Repository.getInstance(FirebaseDataManager.getInstance())
    private val loginManager = FirebaseLoginManager()
    //var checks if app asks questUserStatus only once for each time maps are opened
    private var isUserStatusRequestSend = false
    private lateinit var locationManager: LocationManager
    var userCurrentLocation = MutableLiveData<LatLng>().default(defaultLatLng)
    var locationForCheckInAvailable = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    var locationHasChecked = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    var newQuestStarted = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    var isGuestStartQuest = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)


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

    fun getUserStatusForQuest(questName: String) {
        if (!isUserStatusRequestSend) {
            if(locationLiveData.value != null) {
                locationManager = LocationManager(getLatLngList(locationLiveData.value!!))
                if(loginManager.currentUser != null) {
                    repository.getLastLocationByQuest(loginManager.currentUser!!.uid, questName, object : FirebaseDataManager.LastLocationByQuestListener{
                        override fun onSuccess(location: Int) {
                            locationManager.currentLocationIndex = location
                        }
                        override fun onError(resultStatus: EventResultStatus) {
                            when(resultStatus) {
                                EventResultStatus.NO_EVENT -> Log.e("CheckIn", "firebase call cancelled!")
                                EventResultStatus.EVENT_FAILED -> {
                                    repository.setLastLocationByQuest(loginManager.currentUser!!.uid, questName, 0)
                                    repository.getLastLocationByQuest(loginManager.currentUser!!.uid, questName, object : FirebaseDataManager.LastLocationByQuestListener {
                                        override fun onSuccess(location: Int) {
                                            locationManager.currentLocationIndex = location
                                            newQuestStarted.value = EventResultStatus.EVENT_SUCCESS
                                        }

                                        override fun onError(resultStatus: EventResultStatus) {
                                            newQuestStarted.value = EventResultStatus.EVENT_FAILED
                                        }

                                    })
                                }
                            }
                        }
                    })
                }
                else {startQuestWithGuest(questName)}
                isUserStatusRequestSend = true
            }

        }
    }

    fun startQuestWithGuest(questName: String) {
        isGuestStartQuest.value = EventResultStatus.EVENT_SUCCESS
        questMapForGuest = HashMap<String, Int>()
        questMapForGuest[questName] = 0
    }

    fun locationCheckInListener() {
        if(locationLiveData.value != null) {
            locationManager.locationCheckInListener(userCurrentLocation.value!!, object: ua.lviv.iot.model.map.LocationManager.LocationCheckInListener{
                override fun onChange(result: EventResultStatus) {
                    locationForCheckInAvailable.value = result
                }

            })
        }
    }

    fun activateCheckIn(questName: String) {
        if (isGuestStartQuest.value == EventResultStatus.NO_EVENT) {
            locationManager.checkInLocation(questName, repository, object : LocationManager.OnLocationChecked {
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
                    //Other activity after location checIn
                }

            })
        }
        else {
            questMapForGuest[questName] = locationManager.currentLocationIndex++
            locationHasChecked.value = EventResultStatus.EVENT_SUCCESS
        }
    }


    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

    //------------------------------------------------------------------------------------------------------

    fun drawRoute(questName: String) {

        firebaseDataManager.questRetrieverByName(questName, object : FirebaseDataManager.DataRetrieverListenerForSingleQuestStructure {
            override fun onSuccess(questStructure: Quest, locationsIdList: List<Int>) {
                var currentQuestCategory = questStructure.parentCategoryID
                firebaseDataManager.locationsListRetriever(locationsIdList, object : FirebaseDataManager.DataRetrieveListenerForLocationsStructure {
                    override fun onSuccess(locationStructureList: List<LocationStructure>) {
                        predistanceList.clear()
                        distanceList.clear() //Cleaning, in case there are data from previous quest
                        locationListFromDatabase.addAll(locationStructureList)
                        locationLiveData.postValue(locationListFromDatabase)
                        prepareDataAndDrawingRoute(locationStructureList)
                    }

                    override fun onError(databaseError: DatabaseError) {
                        Log.e("FirebaseDataManager", "eeeedgfde")
                    }
                })
            }

            override fun onError(databaseError: DatabaseError) {

            }
        })
    }

    private fun prepareDataAndDrawingRoute(locationStructureList: List<LocationStructure>) {
        data = getLatLngList(locationStructureList)
        polylinesList.clear()
        prePolylinesList.clear()

        counter = if (data.size == 8) {
            1
        } else {
            data.size / 8 + 1
        }
        var latlngList: List<LatLng>
        var origin: LatLng?
        var dest: LatLng?
        if (data.size > 7) {
            var i = 0
            while (i < data.size - 1) {

                origin = LatLng(data[i].latitude, data[i].longitude)
                if (i + 7 > data.size - 1) {
                    dest = LatLng(data[data.size - 1].latitude, data[data.size - 1].longitude)
                    latlngList = data.subList(i + 1, data.size - 1)
                } else {
                    dest = LatLng(data[i + 7].latitude, data[i + 7].longitude)
                    latlngList = data.subList(i + 1, i + 7)
                }
                makeRequest(origin, latlngList, dest)
                i += 7
            }
        } else {
            origin = LatLng(data[0].latitude, data[0].longitude)
            dest = LatLng(data[data.size - 1].latitude, data[data.size - 1].longitude)
            latlngList = data.subList(1, data.size - 1)

            makeRequest(origin, latlngList, dest)
        }
    }

    private fun makeRequest(orig: LatLng, latlngList: List<LatLng>, destin: LatLng) {
        GoogleDirection.withServerKey("AIzaSyALGNj3GZI8DpCLzYeoqQz2Kr0HuqUdiGg")
                .from(orig)
                .and(latlngList)
                .to(destin)
                .transportMode(TransportMode.WALKING)
                .execute(this)
    }

    private fun getLatLngList(locationStructureList: List<LocationStructure>): ArrayList<LatLng> {
        val latlngList = ArrayList<LatLng>()
        for (locationStructure in locationStructureList) {
            val point = LatLng(locationStructure.lat, locationStructure.lon)
            latlngList.add(point)
        }
        return latlngList
    }

    override fun onDirectionSuccess(direction: Direction, rawBody: String) {
        if (direction.isOK) {
            counter--
            val directionPart = ArrayList<LatLng>()
            val distancePart = ArrayList<String>()

            for (j in 0 until direction.routeList[0].legList.size) {
                val leg = direction.routeList[0].legList[j]
                distancePart.add(direction.routeList[0].legList[j].distance.text)

                for (i in 0 until leg.stepList.size) {
                    directionPart.addAll(leg.stepList[i].polyline.pointList)
                }
            }

            prePolylinesList.add(directionPart)
            predistanceList.add(distancePart)

            if (counter == 0) {
                polylinesList.add(prePolylinesList[0])
                distanceList.add(predistanceList[0])
                prePolylinesList.removeAt(0)
                predistanceList.removeAt(0)
                while (polylinesList.size != prePolylinesList.size + 1) {
                    for (i in 0 until prePolylinesList.size) {
                        for (j in 0 until polylinesList.size) {
                            if (prePolylinesList[i][prePolylinesList[i].size - 1] == polylinesList[j][0] &&
                                    !polylinesList.contains(prePolylinesList[i])) {
                                polylinesList.add(j, prePolylinesList[i])
                                distanceList.add(j, predistanceList[i])
                            }
                            if (prePolylinesList[i][0] == polylinesList[j][polylinesList[j].size - 1] &&
                                    !polylinesList.contains(prePolylinesList[i])) {
                                polylinesList.add(j + 1, prePolylinesList[i])
                                distanceList.add(j + 1, predistanceList[i])
                            }
                        }
                    }
                }
                polylinesLiveData.postValue(polylinesList)
                distanceLiveData.postValue(distanceList)
            }
        }
    }

    override fun onDirectionFailure(t: Throwable) {
        Log.e("Error", t.localizedMessage)
    }

    //this object is using when guest start a quest to store progress
    companion object {
        lateinit var questMapForGuest: HashMap<String, Int>
    }

}


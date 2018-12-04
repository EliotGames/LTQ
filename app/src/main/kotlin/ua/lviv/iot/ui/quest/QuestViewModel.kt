package ua.lviv.iot.ui.quest

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.location.Location
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
import ua.lviv.iot.model.firebase.UserQuest
import ua.lviv.iot.model.map.Quest
import ua.lviv.iot.model.map.UserLocationManager
import kotlin.math.pow
import kotlin.math.sqrt


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
    val repository = Repository.getInstance(FirebaseDataManager.getInstance())
    private val loginManager = FirebaseLoginManager()
    //var checks if app asks questUserStatus only once for each time maps are opened
    private var isUserStatusRequestSend = false
    private lateinit var locationManager: LocationManager
    var currentLocationIndex = 0
    //this var need to get bottomsheet for first check_in button click and activate location for the second click
    var checkInPreparing = MutableLiveData<CheckInPreparing>().default(CheckInPreparing.NO_CHECK_IN)
    var userCurrentLocation = MutableLiveData<LatLng>().default(defaultLatLng)
    var locationForCheckInAvailable = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    var locationHasChecked = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    var newQuestStarted = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    var isGuestStartQuest = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)
    private var questID: Int = 0
    private lateinit var latestPointOnRoute: LatLng

    //set new user coordinates from gps
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

    //get user status in this quest if user has started it, or create new note if not
    //also check is user sign in and if not  - create temporary data
    fun getUserStatusForQuest(questID: Int) {
        this.questID = questID
        if (!isUserStatusRequestSend) {
            if(locationLiveData.value != null) {
                locationManager = LocationManager(getLatLngList(locationLiveData.value!!))
                if(loginManager.currentUser != null) {
                    repository.getLastLocationByQuest(loginManager.currentUser!!.uid, questID, object : FirebaseDataManager.LastLocationByQuestListener{
                        override fun onSuccess(location: Int) {
                            locationManager.currentLocationIndex = location+1
                        }
                        override fun onError(resultStatus: EventResultStatus) {
                            when(resultStatus) {
                                EventResultStatus.NO_EVENT -> Log.e("CheckIn", "firebase call cancelled!")
                                EventResultStatus.EVENT_FAILED -> {
                                    repository.setLastLocationByQuest(loginManager.currentUser!!.uid, questID, 0)
                                    repository.getLastLocationByQuest(loginManager.currentUser!!.uid, questID, object : FirebaseDataManager.LastLocationByQuestListener {
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
                else {startQuestWithGuest(questID)}
                isUserStatusRequestSend = true
            }

        }
    }

    //create temporary data for users when they have not sign in
    fun startQuestWithGuest(questID: Int) {
        isGuestStartQuest.value = EventResultStatus.EVENT_SUCCESS
        questMapForGuest = HashMap()
        questMapForGuest["ID"+questID.toString()] = UserQuest(0)
    }

    //inform if user is near landmark
    fun locationCheckInListener() {
        if(locationLiveData.value != null) {
            locationManager = LocationManager(getLatLngList(locationLiveData.value!!))
            locationManager.locationCheckInListener(userCurrentLocation.value!!, object: ua.lviv.iot.model.map.LocationManager.LocationCheckInListener{
                override fun onChange(result: EventResultStatus) {
                    locationForCheckInAvailable.value = result
                }

            })
        }
    }

    //update user status in quest when user press check in button
    fun activateCheckIn(questID: Int) {
        if(checkInPreparing.value == CheckInPreparing.NO_CHECK_IN) {
            checkInPreparing.value = CheckInPreparing.BOTTOM_SHEET_UP
        }
        else {
            if (isGuestStartQuest.value == EventResultStatus.NO_EVENT) {
                checkInLocation(questID, repository, object : LocationManager.OnLocationChecked {
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
                questMapForGuest["ID"+questID.toString()] = UserQuest(locationManager.currentLocationIndex++)
                locationHasChecked.value = EventResultStatus.EVENT_SUCCESS
            }
            checkInPreparing.value = CheckInPreparing.NO_CHECK_IN
        }
    }

    //call when user click CheckIn button to update user data
    fun checkInLocation(questID: Int, repository: Repository, listener: LocationManager.OnLocationChecked) {
        repository.setLastLocationByQuest(FirebaseLoginManager().currentUser!!.uid, questID, currentLocationIndex+1)
        repository.getLastLocationByQuest(FirebaseLoginManager().currentUser!!.uid, questID, object: FirebaseDataManager.LastLocationByQuestListener{
            override fun onSuccess(location: Int) {
                if (location == currentLocationIndex+1) {
                    if (checkEndOfQuest(location)) {
                        TODO()//quest has ended YAHHHHOOOOOOOOO
                    }
                    else {
                        currentLocationIndex++
                        listener.onSuccess()
                    }
                }
                else {
                    listener.onError(EventResultStatus.EVENT_SUCCESS)
                }
            }
            override fun onError(resultStatus: EventResultStatus) {
                listener.onError(resultStatus)
            }
        })
    }

    fun checkEndOfQuest(location: Int) : Boolean {
        return location == locationLiveData.value!!.size - 1
    }


    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

    //------------------------------------------------------------------------------------------------------

    fun drawRoute(questID: Int) {

        firebaseDataManager.questRetrieverByID(questID, object : FirebaseDataManager.DataRetrieverListenerForSingleQuestStructure {
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

    fun getLatLngList(locationStructureList: List<LocationStructure>): ArrayList<LatLng> {
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
                            if (prePolylinesList[i].last() == polylinesList[j].first() &&
                                    !polylinesList.contains(prePolylinesList[i])) {
                                polylinesList.add(j, prePolylinesList[i])
                                distanceList.add(j, predistanceList[i])
                            }
                            if (prePolylinesList[i].first() == polylinesList[j].last() &&
                                    !polylinesList.contains(prePolylinesList[i])) {
                                polylinesList.add(j + 1, prePolylinesList[i])
                                distanceList.add(j + 1, predistanceList[i])
                            }
                        }
                    }
                }
                latestPointOnRoute = polylinesList[0][0]
                polylinesLiveData.postValue(polylinesList)
                distanceLiveData.postValue(distanceList)
            }
        }
    }

    enum class CheckInPreparing {
        NO_CHECK_IN,
        BOTTOM_SHEET_UP
    }

    override fun onDirectionFailure(t: Throwable) {
        Log.e("Error", t.localizedMessage)
    }

    //this object is using when guest start a quest to store progress
    companion object {
        lateinit var questMapForGuest: HashMap<String, UserQuest>
    }

    private fun drawUsersPath(usersLat: Double, usersLon: Double, polylines: ArrayList<ArrayList<LatLng>>){
        val polylinesSingle = polylines.flatten()
        var intersectPoints = ArrayList<ArrayList<LatLng>>()
        for (i in 0..(polylinesSingle.size-2)){
            var result = findIntersectionOfLineAndCircle(polylinesSingle, usersLat, usersLon, 2.0)
            if (result.size != 0){

            }
        }
        if (intersectPoints.size == 1){
            if (intersectPoints[0].size == 2){

            }

        }
    }

    fun findUserOnRoute(usersLat: Double, usersLon: Double, latestLocation: LatLng, polylines: ArrayList<ArrayList<LatLng>>): Array<Array<LatLng>> {
        val polylinesSingle = polylines.flatten()
        val intersectionsLists = findIntersectionOfLineAndCircle(polylinesSingle, usersLat, usersLon, 2.0)
        var userOnRoute = emptyArray<Array<LatLng>>()
        if (intersectionsLists.size == 1) {
            userOnRoute[0] = intersectionsLists[0].toTypedArray()
        }
        else if (intersectionsLists.size > 1 ){
            userOnRoute = findClosestPoinToPoint(latestLocation, intersectionsLists)
        }
        else{
            //do again or give up
        }
        return userOnRoute
    }

    private fun findClosestPoinToPoint(pointA: LatLng, pointsList: List<List<LatLng>>): Array<Array<LatLng>> {
        val closestPoint = emptyArray<Array<LatLng>>()
        closestPoint.plus(arrayOf(pointsList[0][0], pointsList[0][1]))
        for (pointsPair in pointsList){
            val previousDistance = distanceBetweenPoints(pointA, closestPoint[0][0])
            val newDistance = distanceBetweenPoints(pointA, pointsPair[0])
            if (newDistance < previousDistance){
                closestPoint[0] = pointsPair.toTypedArray()
            }
        }
        return closestPoint
    }

    private fun distanceBetweenPoints (pointA: LatLng, pointB: LatLng): Float{
        val locationA = Location("locationA")
        locationA.latitude = pointA.latitude
        locationA.longitude = pointA.longitude
        val locationB = Location("locationB")
        locationB.latitude = pointB.latitude
        locationB.longitude = pointB.longitude
        return locationA.distanceTo(locationB)
    }

    private fun findIntersectionOfLineAndCircle(polylines: List<LatLng>, usersLat: Double, usersLon: Double, r: Double): List<List<LatLng>> {
        //TODO intersect just sector of a line
        //TODO round values of LatLng
        val intersections = emptyList<List<LatLng>>() // [n][0]: point of intersection, [n][1]: point on polyline before intersection
        for (i in 0..(polylines.size-2)) {
            val x1 = polylines[i].latitude
            val y1 = polylines[i].longitude
            val x2 = polylines[i + 1].latitude
            val y2 = polylines[i + 1].longitude
            val a = usersLat
            val b = usersLon
            val t = if ((y2 - y1) != 0.0) {
                (x2 - x1) / (y2 - y1)
            } else {
                0.0
            }
            val d = -a.pow(2.0) + 2 * a * b * t - 2 * a * y1 * t + 2 * a * x1 - b.pow(2.0) * t.pow(2.0) + 2 * b * y1 * t.pow(2.0) - 2 * b * t * x1 - y1.pow(2.0) * t.pow(2.0) + 2 * y1 * t * x1 + r.pow(2.0) * t.pow(2.0) + r.pow(2.0) - x1.pow(2.0)

            if (d >= 0) {
                val z = (-sqrt(
                        -a.pow(2.0) + 2 * a * b * t - 2 * a * y1 * t + 2 * a * x1 - b.pow(2.0) * t.pow(2.0) + 2 * b * y1 * t.pow(2.0) - 2 * b * t * x1 - y1.pow(2.0) * t.pow(2.0) + 2 * y1 * t * x1 + r.pow(2.0) * t.pow(2.0) + r.pow(2.0) - x1.pow(2.0)) + a * t + b + y1 * t.pow(2.0) - t * x1) / (
                        t.pow(2.0) + 1)
                val x3 = (z - y1) * t + x1
                val z2 = (sqrt(
                        -a.pow(2.0) + 2 * a * b * t - 2 * a * y1 * t + 2 * a * x1 - b.pow(2.0) * t.pow(2.0) + 2 * b * y1 * t.pow(2.0) - 2 * b * t * x1 - y1.pow(2.0) * t.pow(2.0) + 2 * y1 * t * x1 + r.pow(2.0) * t.pow(2.0) + r.pow(2.0) - x1.pow(2.0)) + a * t + b + y1 * t.pow(2.0) - t * x1) / (
                        t.pow(2.0) + 1)
                val x4 = (z2 - y1) * t + x1

                if (z == z2 && x3 == x4) {
                    intersections.plus(listOf(LatLng(z, x3), polylines[i]))
                } else {
                    intersections.plus(listOf(LatLng(z, x3), polylines[i]))
                    intersections.plus(listOf(LatLng(z2, x4), polylines[i]))
                }
            }
        }
        return intersections
    }
    fun test(polylinesList: ArrayList<ArrayList<LatLng>>){
        val test = findUserOnRoute(userCurrentLocation.value!!.latitude, userCurrentLocation.value!!.longitude, latestPointOnRoute, polylinesList)
    }

}


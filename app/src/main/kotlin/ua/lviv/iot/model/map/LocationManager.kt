package ua.lviv.iot.model.map

import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.firebase.Repository
import kotlin.math.pow

class LocationManager(private val locationsList: ArrayList<LatLng>) {

    private val CHECKIN_ZONE = 0.00015
    private var currentLocationIndex = 0

    //listen if user is in location check_zone
    fun locationCheckInListener(userLocation: LatLng, listener: LocationCheckInListener) {
        for(i in currentLocationIndex..locationsList.size) {
            if(isCheckInAvailable(locationsList[currentLocationIndex], userLocation, CHECKIN_ZONE)) {
                listener.onChange(EventResultStatus.EVENT_SUCCESS)
            }
            else {listener.onChange(EventResultStatus.NO_EVENT)}
        }
    }

    //call when user click CheckIn button to update user data
    fun checkInLocation(questName: String, listener: OnLocationChecked) {
        TODO()//repository.setLastLocationByQuest(FirebaseLoginManager.auth.currentUser!!.uid, questName, currentLocationIndex)
        //repository.getLastLocationByQuest(FirebaseLoginManager.auth.currentUser!!.uid, questName, object: listener: FirebaseDataManager.LastLocationByQuestListener{
        // override onSuccess(location) {
        //    if location == currentLocationIndex {
        //        listener.onSuccess()
        //        currentLocationIndex++
        // }
        //    else {
        //        listener.onError(EventResultStatus)
        // }
        // }
        //
        // override onError(resultStatus) {
        //    listener.onError(resultStatus)
        // }
        // })
    }

    //method to find distance between locations---------------------------------------------------------
    private fun isCheckInAvailable(landmark : LatLng, user : LatLng, radius: Double) : Boolean {
        return (user.latitude - landmark.latitude).pow(2) + (user.longitude - landmark.longitude).pow(2) <= radius.pow(2)
    }
    //---------------------------------------------------------------------------------------------------

    interface LocationCheckInListener {
        fun onChange(result: EventResultStatus)
    }

    interface OnLocationChecked {
        fun onSuccess()

        fun onError(result: EventResultStatus)
    }
}
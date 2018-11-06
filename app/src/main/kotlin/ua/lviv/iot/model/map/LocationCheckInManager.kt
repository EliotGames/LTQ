package ua.lviv.iot.model.map

import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.model.EventResultStatus
import kotlin.math.pow
import kotlin.math.sqrt

class LocationCheckInManager(private val locationsList: ArrayList<LatLng>) {

    private val CHECKIN_ZONE = 0.00015
    private var currentLocationIndex = 0

    fun locationCheckInListener(userLocation: LatLng, listener: LocationCheckInListener) {
        for(i in currentLocationIndex..locationsList.size) {
            if(isCheckInAvailable(locationsList[currentLocationIndex], userLocation, CHECKIN_ZONE)) {
                listener.onChange(EventResultStatus.EVENT_SUCCESS)
            }
            else {listener.onChange(EventResultStatus.NO_EVENT)}
        }
    }

    //method to find distance between locations---------------------------------------------------------
    private fun isCheckInAvailable(landmark : LatLng, user : LatLng, radius: Double) : Boolean {
        return (user.latitude - landmark.latitude).pow(2) + (user.longitude - landmark.longitude).pow(2) <= radius.pow(2)
    }
    //---------------------------------------------------------------------------------------------------

    interface LocationCheckInListener {
        fun onChange(result: EventResultStatus)
    }
}
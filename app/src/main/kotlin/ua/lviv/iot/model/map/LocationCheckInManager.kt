package ua.lviv.iot.model.map

import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.model.EventResultStatus

class LocationCheckInManager(private val locationsList: ArrayList<LatLng>) {

    private val CHECKIN_ZONE = 0.05
    private var currentLocationIndex = 0

    fun locationCheckInListener(userLocation: LatLng, listener: LocationCheckInListener) {
        for(i in currentLocationIndex..locationsList.size) {
            if(UserLocationManager.getDistance(locationsList[currentLocationIndex], userLocation) <= CHECKIN_ZONE) {
                listener.onChange(EventResultStatus.EVENT_SUCCESS)
            }
            else {listener.onChange(EventResultStatus.NO_EVENT)}
        }
    }

    interface LocationCheckInListener {
        fun onChange(result: EventResultStatus)
    }
}
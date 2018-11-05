package ua.lviv.iot.model.map

import com.google.android.gms.maps.model.LatLng
import ua.lviv.iot.model.EventResultStatus

class LocationCheckInManager(private val locationsList: ArrayList<LatLng>) {

    private var currentLocationIndex = 0

    fun locationCheckInListener(userLocation: LatLng, listener: LocationCheckInListener) {
        for(i in currentLocationIndex..locationsList.size) {
            if(UserLocationManager.getDistance(locationsList[currentLocationIndex], userLocation) <= 10) {
                listener.onChange(EventResultStatus.EVENT_SUCCESS)
            }
            else {listener.onChange(EventResultStatus.NO_EVENT)}
        }
    }

    interface LocationCheckInListener {
        fun onChange(result: EventResultStatus)
    }
}
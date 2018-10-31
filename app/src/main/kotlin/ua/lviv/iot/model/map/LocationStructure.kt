package ua.lviv.iot.model.map

import com.google.android.gms.maps.model.LatLng

class LocationStructure {
    var isSecret: Boolean = false
    var lat: Double = 0.toDouble()
    var lon: Double = 0.toDouble()

    var locationDescription: String? = null
    var locationID: Int? = null
    var locationName: String? = null
    var distanceToPrevious = "start"

    constructor() {}

    constructor(latLng: LatLng) {
        this.lat = latLng.latitude
        this.lon = latLng.longitude
    }
}
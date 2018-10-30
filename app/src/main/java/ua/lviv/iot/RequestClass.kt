package ua.lviv.iot

import com.google.android.gms.maps.model.LatLng

class RequestClass {
    var origin: LatLng? = null
    var dest: LatLng? = null
    var waypoints: List<LatLng>? = null

    constructor() {}

    constructor(origin: LatLng, dest: LatLng, waypoints: List<LatLng>) {
        this.origin = origin
        this.dest = dest
        this.waypoints = waypoints
    }


}
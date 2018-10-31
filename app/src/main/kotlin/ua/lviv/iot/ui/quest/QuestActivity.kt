package ua.lviv.iot.ui.quest

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.akexorcist.googledirection.GoogleDirection
import com.google.android.gms.maps.CameraUpdateFactory
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.map.LocationStructure
//import sun.management.VMOptionCompositeData.getOrigin
import android.util.Log
import com.akexorcist.googledirection.model.Direction
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.constant.TransportMode
import com.androidmapsextensions.GoogleMap
import com.androidmapsextensions.Marker
import com.androidmapsextensions.MarkerOptions
import com.androidmapsextensions.OnMapReadyCallback
import com.androidmapsextensions.PolylineOptions
import com.androidmapsextensions.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DatabaseError
import ua.lviv.iot.R
import ua.lviv.iot.model.map.Quest


class QuestActivity : AppCompatActivity(), OnMapReadyCallback, DirectionCallback {


    private lateinit var mMap: GoogleMap
    var numberOfPoint: TextView? = null
    private val locationManager: LocationManager? = null
    private val firstCameraOnMyPosition = true
    private val LOCATION_PERMISSION_REQUEST_CODE = 111
    private val DEFAULT_LATITUDE = 49.841787
    private val DEFAULT_LONGITUDE = 24.031686
    private val mPositionMarker: Marker? = null
    private val myLocationButton: View? = null
    private val screen1: View? = null
    private val screen2: View? = null
    private val drawerLayout: DrawerLayout? = null
    private val currentLatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private val firebaseAuthManager: FirebaseLoginManager? = null
    private val navigationView: NavigationView? = null
    private var data = ArrayList<LatLng>()
    private val changedMarkerInflated: View? = null
    private val changedMarkerNumber: TextView? = null
    private var distanceBetweenPoint: TextView? = null
    private val cMarkerdistanceBetweenPoint: TextView? = null
    private var inflater: LayoutInflater? = null
    private var markerInflated: View? = null
    private var secretMarkerInflated: View? = null
    private val polylinesList = ArrayList<LatLng>()
    private var origin: LatLng? = null
    private var dest: LatLng? = null
    private var counter: Int = 0
    private var requestIndex = 0
    private val bottomSheet: View? = null
    private val mBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private val bottomSheetName: TextView? = null
    private val bottomSheetInfo: TextView? = null
    private val bottomSheetSkipButton: Button? = null
    private val isQuestOn: Boolean = false
    private val currentQuestCategory: Int = 0
    private val distanceList = ArrayList<String>()
    private var locationListFromDatabase: List<LocationStructure>? = null
    private var currentQuestName: String? = null
    private val currentUserId: String? = null
    private val markersList = ArrayList<Marker>()
    private var requestList = ArrayList<RequestClass>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getExtendedMapAsync(this)


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.silver_style_maps))
        } catch (e: Resources.NotFoundException) {
            e.message
        }
        inflater = applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        markerInflated = inflater!!.inflate(R.layout.marker, null)
        numberOfPoint = markerInflated!!.findViewById(R.id.number_text_view) as TextView
        val intent = intent
        currentQuestName = intent.getStringExtra("questName")
        secretMarkerInflated = inflater!!.inflate(R.layout.marker, null)
        distanceBetweenPoint = markerInflated!!.findViewById(R.id.text_text_view) as TextView
        drawRoute(currentQuestName!!)
        // Add a marker in Sydney and move the camera
        //mMap.moveCamera(CameraUpdateFactory.newLatLng())
    }

    private fun getMyLocation(location: Location): LatLng {
        val latitude = location.latitude
        val longitude = location.longitude
        return LatLng(latitude, longitude)
    }

    fun drawRoute(questName: String) {

        firebaseDataManager.questRetrieverByName(questName, object: FirebaseDataManager.DataRetrieverListenerForSingleQuestStructure {
            override fun onSuccess(questStructure: Quest, locationsIdList: List<Int>) {
                var currentQuestCategory = questStructure.parentCategoryID
                firebaseDataManager.locationsListRetriever(locationsIdList, object: FirebaseDataManager.DataRetrieveListenerForLocationsStructure {
                    override fun onSuccess(locationStructureList: List<LocationStructure> ) {
                        locationListFromDatabase = locationStructureList
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

    private fun focusMapOnMarkers(markersList: List<Marker> ){
        val builder = LatLngBounds.builder()
        for (marker in markersList) {
            builder.include(marker.position)
        }
        val bounds = builder.build()
        val padding = 65 // offset from edges of the map in pixels
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        mMap.animateCamera(cameraUpdate)
    }

    private fun prepareDataAndDrawingRoute(locationStructureList: List<LocationStructure>) {
        data = getLatLngList(locationStructureList)
        polylinesList.clear()

        if (data.size == 8) {
            counter = 1
        } else {
            counter = data.size / 8 + 1
        }
        var latlngList: List<LatLng>
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
                val request = RequestClass(origin!!, dest!!, latlngList)
                requestList.add(request)
                i += 7
            }

            GoogleDirection.withServerKey("AIzaSyALGNj3GZI8DpCLzYeoqQz2Kr0HuqUdiGg")
                    .from(requestList[0].origin)
                    .and(requestList[0].waypoints)
                    .to(requestList[0].dest)
                    .transportMode(TransportMode.WALKING)
                    .execute(this)


        } else {
            origin = LatLng(data[0].latitude, data[0].longitude)
            dest = LatLng(data[data.size - 1].latitude, data[data.size - 1].longitude)
            latlngList = data.subList(1, data.size - 1)

            GoogleDirection.withServerKey("AIzaSyALGNj3GZI8DpCLzYeoqQz2Kr0HuqUdiGg")
                    .from(origin)
                    .and(latlngList)
                    .to(dest)
                    .transportMode(TransportMode.WALKING)
                    .execute(this)
        }
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
            requestIndex++
            for (j in 0 until direction.routeList[0].legList.size) {
                val leg = direction.routeList[0].legList[j]
                distanceList.add(direction.routeList[0].legList[j].distance.text)
                for (i in 0 until leg.stepList.size) {
                    polylinesList.addAll(leg.stepList[i].polyline.pointList)
                }
            }

            if (counter == 0) {
                requestIndex = 0
                createPolylines(polylinesList)
                createMarkers(locationListFromDatabase!!)
                polylinesList.clear()
                distanceList.clear()
            } else {
                GoogleDirection.withServerKey("AIzaSyALGNj3GZI8DpCLzYeoqQz2Kr0HuqUdiGg")
                        .from(requestList[requestIndex].origin)
                        .and(requestList[requestIndex].waypoints)
                        .to(requestList[requestIndex].dest)
                        .transportMode(TransportMode.WALKING)
                        .execute(this)
            }
        }
    }

    override fun onDirectionFailure(t: Throwable) {
        Log.e("Error", t.localizedMessage)
    }

    private fun createPolylines(list: ArrayList<LatLng>){
        mMap.addPolyline(PolylineOptions()
                .addAll(list)
                .width(11F)
                .jointType(JointType.BEVEL)
                .color(Color.rgb(145, 121, 241)))
    }

    private fun createMarkers(locationStructureList: List<LocationStructure>) {
        for (i in 0 until locationStructureList.size - 1) {
            locationStructureList[i + 1].distanceToPrevious = distanceList[i]
        }
        distanceList.clear()
        for (i in locationStructureList.indices) {
            val j = i + 1
            if (!locationStructureList[i].isSecret) {
                numberOfPoint!!.text = j.toString()
                distanceBetweenPoint!!.text = locationStructureList[i].distanceToPrevious
                val marker = mMap.addMarker(MarkerOptions()
                        .position(LatLng(locationStructureList[i].lat, locationStructureList[i].lon))
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(markerInflated!!))))
                locationStructureList[i].locationID = i + 1
                marker.setData(locationStructureList[i])
                markersList.add(marker)
            } else {
                val secretMarker1 = mMap.addMarker(MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(secretMarkerInflated!!)))
                        .anchor(0.5f, 0.5f)
                        .position(LatLng(locationStructureList[i].lat, locationStructureList[i].lon)))
                secretMarker1.setData(locationStructureList[i])
                markersList.add(secretMarker1)
            }
        }
        changeMarkerListener()
        focusMapOnMarkers(markersList)
    }
    private fun getBitmapFromView(view: View): Bitmap {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight,
                Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.draw(canvas)
        return bitmap
    }

    private fun changeMarkerListener() {
        mMap.setOnMarkerClickListener(object: GoogleMap.OnMarkerClickListener {


            override fun onMarkerClick(marker: Marker): Boolean {
                val locationStructure = marker.getData<LocationStructure>()
                if (!locationStructure.isSecret) {
                    val number = locationStructure.locationID
                    changedMarkerNumber!!.text = number.toString()
                    cMarkerdistanceBetweenPoint!!.text = "done"
//                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(changedMarkerInflated)));
//                    mBottomSheetBehavior!!.isHideable = true
//                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//                    bottomSheetName!!.text = locationStructure.locationName
//                    bottomSheetInfo!!.text = locationStructure.locationDescription
//                    bottomSheetSkipButton!!.setOnClickListener(object: View.OnClickListener {
//                        override fun onClick(v: View) {
//                            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN;
//                        }
//                    })
//
//
//                    runOnUiThread(object: Runnable() {
//                        override fun run() {
//
//
//                        }
//                    })
//
//                    val handler = Handler()
//                    handler.postDelayed(object: Runnable {
//                        override fun run() {
//                            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//                        }
//                    }, 300)
//                } else {
//
                }

                return true

            }
        })
    }



}
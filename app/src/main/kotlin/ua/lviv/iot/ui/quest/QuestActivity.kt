package ua.lviv.iot.ui.quest

import android.Manifest
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
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
import android.widget.Toast
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
import kotlinx.android.synthetic.main.activity_quest.*
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.map.Quest
import ua.lviv.iot.utils.MarkerType


class QuestActivity : AppCompatActivity(), OnMapReadyCallback, DirectionCallback {


    private val MY_LOCATION_PERMISSIONS_REQUEST = 121
    private val DEFAULT_LATITUDE = 49.841787
    private val DEFAULT_LONGITUDE = 24.031686
    private val defaultLatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    private lateinit var mMap: GoogleMap
    private var numberOfNormalMarker: TextView? = null
    private var numberOfBlackMarker: TextView? = null
    private var mPositionMarker: Marker? = null
    private val myLocationButton: View? = null
    private val drawerLayout: DrawerLayout? = null
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private val firebaseAuthManager: FirebaseLoginManager? = null
    private val navigationView: NavigationView? = null
    private var data = ArrayList<LatLng>()
    private var distanceNormalMarker: TextView? = null
    private var distanceBlackMarker: TextView? = null
    private var inflater: LayoutInflater? = null
    private var normalMarkerInflated: View? = null
    private var blackMarkerInflated: View? = null
    private var secretMarkerInflated: View? = null
    private val polylinesList = ArrayList<LatLng>()
    private var origin: LatLng? = null
    private var dest: LatLng? = null
    private var counter: Int = 0
    private var requestIndex = 0
    private var bottomSheet: View? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var bottomSheetName: TextView? = null
    private var bottomSheetInfo: TextView? = null
    private var bottomSheetSkipButton: Button? = null
    private val isQuestOn: Boolean = false
    private val currentQuestCategory: Int = 0
    private val distanceList = ArrayList<String>()
    private var locationListFromDatabase: List<LocationStructure>? = null
    private var currentQuestName: String? = null
    private val currentUserId: String? = null
    private val markersList = ArrayList<Marker>()
    private var requestList = ArrayList<RequestClass>()
    private lateinit var questViewModel : QuestViewModel
    private var userCurrentLocation = defaultLatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)
        currentQuestName = intent.getStringExtra("questName")
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getExtendedMapAsync(this)
        questViewModel = ViewModelProviders.of(this).get(QuestViewModel::class.java)
        initUserLocationUpdates(questViewModel, getSystemService(Context.LOCATION_SERVICE))
        questViewModel.userCurrentLocation.observe(this, Observer {
            userCurrentLocation = it!!
            if(mPositionMarker != null) {
                mPositionMarker!!.position = userCurrentLocation
                if(locationListFromDatabase != null) {
                    questViewModel.locationCheckInListener(getLatLngList(locationListFromDatabase!!))
                }
            }
        })
        questViewModel.locationForCheckInAvailable.observe(this, Observer {
            if(it == EventResultStatus.EVENT_SUCCESS) {
                check_in_button.visibility = View.VISIBLE
                Toast.makeText(this, "Check in is available", Toast.LENGTH_SHORT).show()
            }
            else if (it == EventResultStatus.NO_EVENT) {
                check_in_button.visibility = View.INVISIBLE
            }
        })
        bottomSheetInit()
        fun <T> LiveData<T>.observe(observe: (T?) -> Unit) = observe(this@QuestActivity, Observer {
            observe(it)})
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.silver_style_maps))
        } catch (e: Resources.NotFoundException) {
            e.message
        }
        inflater = applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        normalMarkerInflated = inflater!!.inflate(R.layout.marker, null)
        numberOfNormalMarker = normalMarkerInflated!!.findViewById(R.id.marker_number) as TextView
        distanceNormalMarker = normalMarkerInflated!!.findViewById(R.id.marker_distance) as TextView

        blackMarkerInflated = inflater!!.inflate(R.layout.black_marker, null)
        numberOfBlackMarker = blackMarkerInflated!!.findViewById(R.id.changed_marker_number) as TextView
        distanceBlackMarker = blackMarkerInflated!!.findViewById(R.id.changed_marker_distance) as TextView

        secretMarkerInflated = inflater!!.inflate(R.layout.marker, null)

        drawRoute(currentQuestName!!)

        //set user marker and user location button
        if (fineLocationEnabled()||coarceLocationEnabled()) {
            setUserLocationMarker(mMap)
        }
    }

    //USER CURRENT LOCATION---------------------------------------------------------------------------

    private fun fineLocationEnabled() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    private fun coarceLocationEnabled() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun initUserLocationUpdates(questViewModel: QuestViewModel, locationSystemService: Any) {
        if(fineLocationEnabled()||coarceLocationEnabled()) {
            questViewModel.checkUserLocationUpdates(locationSystemService)
        }
        else requestPermission()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                MY_LOCATION_PERMISSIONS_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_LOCATION_PERMISSIONS_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initUserLocationUpdates(questViewModel, getSystemService(Context.LOCATION_SERVICE))
                    setUserLocationMarker(mMap)
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun setUserLocationMarker(mMap: GoogleMap) {
        mMap.setMyLocationEnabled(false)
        mPositionMarker = mMap.addMarker(MarkerOptions()
                .flat(false)
                .icon(BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.quest_user_location_marker)))
                .anchor(0.5f, 1.0f)
                .position(userCurrentLocation)
                .draggable(false))
    }

    private fun getBitmap(drawableRes: Int) : Bitmap {
        var draw = resources.getDrawable(drawableRes, null)
        var canvas = Canvas()
        var bitmap = Bitmap.createBitmap(draw.intrinsicWidth, draw.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap);
        draw.setBounds(0, 0, draw.intrinsicWidth, draw.intrinsicHeight)
        draw.draw(canvas)
        return bitmap
    }


    //-----------------------------------------------------------------------------------------------

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
                numberOfNormalMarker!!.text = j.toString()
                distanceNormalMarker!!.text = locationStructureList[i].distanceToPrevious
                val marker = mMap.addMarker(MarkerOptions()
                        .position(LatLng(locationStructureList[i].lat, locationStructureList[i].lon))
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(normalMarkerInflated!!))))
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
        mMap.setOnMarkerClickListener (
            object: GoogleMap.OnMarkerClickListener {

                override fun onMarkerClick(marker: Marker): Boolean {
                    if(mBottomSheetBehavior!!.state == BottomSheetBehavior.STATE_HIDDEN){
                        val locationStructure = marker.getData<LocationStructure>()
                        if (!locationStructure.isSecret) {
                            changeMarkerView(marker, MarkerType.BLACK)
                            mBottomSheetBehavior!!.isHideable = true
                            mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
                            bottomSheetInfo!!.text = locationStructure.locationDescription
                            bottomSheetName!!.text = locationStructure.locationName
                            bottomSheetSkipButton!!.setOnClickListener(object : View.OnClickListener {
                                override fun onClick(v: View) {
                                    mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
                                    changeMarkerView(marker, MarkerType.NORMAL)
                                }
                            })
                            mBottomSheetBehavior!!.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
                                override fun onStateChanged(view: View, currentState: Int) {
                                    if(currentState == BottomSheetBehavior.STATE_HIDDEN){
                                        changeMarkerView(marker, MarkerType.NORMAL)
                                    }
                                }

                                override fun onSlide(p0: View, p1: Float) {
                                }

                            })

                            val handler = Handler()
                            handler.postDelayed(object : Runnable {
                                override fun run() {
                                    mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED;
                                }
                            }, 300)
                        }
                    }
                    return true
                }
            }
        )
        }

    private fun bottomSheetInit() {
        bottomSheet = findViewById(R.id.bottom_sheet)
        bottomSheetName = findViewById(R.id.bottom_sheet_name)
        bottomSheetInfo = findViewById(R.id.bottom_sheet_info)
        bottomSheetSkipButton = findViewById(R.id.bottom_sheet_skip)
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBottomSheetBehavior!!.isHideable = true
        mBottomSheetBehavior!!.peekHeight = 384
        mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun changeMarkerView(marker: Marker, markerTypeToChange: MarkerType){
        val distance = marker.getData<LocationStructure>().distanceToPrevious
        val locationId = marker.getData<LocationStructure>().locationID
        when(markerTypeToChange){
            MarkerType.NORMAL -> {
                distanceNormalMarker!!.text = distance
                numberOfNormalMarker!!.text = locationId!!.toString()
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(normalMarkerInflated!!)))
            }
            MarkerType.BLACK -> {
                distanceBlackMarker!!.text = distance
                numberOfBlackMarker!!.text = locationId!!.toString()
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(blackMarkerInflated!!)))
            }
            MarkerType.SECRET -> {

            }
        }
    }



}

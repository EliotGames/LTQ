package ua.lviv.iot.ui.quest

//import sun.management.VMOptionCompositeData.getOrigin
import android.Manifest
import android.app.AlertDialog
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.androidmapsextensions.*
import com.androidmapsextensions.Marker
import com.androidmapsextensions.MarkerOptions
import com.androidmapsextensions.PolylineOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_quest.*
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.map.LocationManager
import ua.lviv.iot.model.map.LocationStructure
import ua.lviv.iot.ui.user.UserActivity
import ua.lviv.iot.utils.InjectorUtils
import ua.lviv.iot.utils.LVIV_LAT
import ua.lviv.iot.utils.LVIV_LNG
import ua.lviv.iot.utils.MarkerType
import java.nio.file.Files.find


class QuestActivity : AppCompatActivity(), OnMapReadyCallback {
    private val MY_LOCATION_PERMISSIONS_REQUEST = 121
    private val POINTS_REWARD_FOR_LOCATION = 50

    private lateinit var mMap: GoogleMap
    private var numberOfNormalMarker: TextView? = null
    private var numberOfBlackMarker: TextView? = null
    private var numberOfClickedMarker: TextView? = null
    private var numberOfClickedBlackMarker: TextView? = null
    private var mPositionMarker: Marker? = null
    private var distanceNormalMarker: TextView? = null
    private var distanceBlackMarker: TextView? = null
    private var distanceClickedBlackMarker: TextView? = null
    private var distanceClickedMarker: TextView? = null
    private var inflater: LayoutInflater? = null
    private var normalMarkerInflated: View? = null
    private var blackMarkerInflated: View? = null
    private var clickedMarkerInflated: View? = null
    private var secretMarkerInflated: View? = null
    private var clickedBlackMarkerInflated: View? = null

    private var bottomSheet: View? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var bottomSheetName: TextView? = null
    private var bottomSheetInfo: TextView? = null
    private var bottomSheetSkipButton: Button? = null
    private var currentQuestID: Int? = null
    private val markersList = ArrayList<Marker>()
    private lateinit var questViewModel: QuestViewModel
    private var userCurrentLocation = LatLng(LVIV_LAT, LVIV_LNG)
    private var previousClickedMarker: Marker? = null
    private var previousClickedMarkerZindex = 0.0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)
        currentQuestID = intent.getIntExtra("questID", -1)
        val factory = InjectorUtils.provideQuestViewModelFactory()
        questViewModel = ViewModelProviders.of(this, factory).get(QuestViewModel::class.java)
        initUserLocationUpdates(questViewModel, getSystemService(Context.LOCATION_SERVICE))
        questViewModel.getUserStatusForQuest(currentQuestID!!)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getExtendedMapAsync(this)
        questViewModel.updateUserBalance()

        questViewModel.userCurrentLocation.observe(this, Observer {
            userCurrentLocation = it!!
            if (mPositionMarker != null) {
                animateMarker(userCurrentLocation)
                questViewModel.locationCheckInListener()
            }
        })

        questViewModel.newQuestStarted.observe(this, Observer {
            when (it) {
                EventResultStatus.EVENT_SUCCESS -> Toast.makeText(this, R.string.new_quest_start_success, Toast.LENGTH_SHORT).show()
                EventResultStatus.EVENT_FAILED -> Toast.makeText(this, R.string.new_quest_start_fail, Toast.LENGTH_SHORT).show()
            }
        })

        questViewModel.locationForCheckInAvailable.observe(this, Observer {
            if (it == EventResultStatus.EVENT_SUCCESS) {
                fab_quest_checkin.show()
            } else if (it == EventResultStatus.NO_EVENT) {
                fab_quest_checkin.hide()
            }
        })

        questViewModel.locationHasChecked.observe(this, Observer {
            when (it) {
                EventResultStatus.EVENT_SUCCESS -> {
                    questViewModel.locationHasChecked.value = EventResultStatus.NO_EVENT
                    //Toast.makeText(this, R.string.check_in_success, Toast.LENGTH_SHORT).show()
                    alertDialogSuccessCheckIn()
                    changeMarkerView(markersList[questViewModel.currentLocationIndex - 1], MarkerType.BLACK)
                }
                EventResultStatus.EVENT_FAILED -> {
                    Toast.makeText(this, R.string.check_in_failed, Toast.LENGTH_SHORT).show()
                }
                EventResultStatus.NO_EVENT -> {
                }
            }
        })

        questViewModel.isGuestStartQuest.observe(this, Observer {
            when (it) {
                EventResultStatus.EVENT_SUCCESS -> {
                    alertDialogForGuest()
                }
                EventResultStatus.NO_EVENT -> {}
            }
        })

        questViewModel.checkInPreparing.observe(this, Observer {
            if(it == QuestViewModel.CheckInPreparing.BOTTOM_SHEET_UP) {
                markerClickListenerBody(markersList[questViewModel.currentLocationIndex])
            }
        })

        questViewModel.userBalance.observe(this, Observer {
            user_balance.text = it.toString()
        })
        //-------------------------------------------------------------------------------------------------
        initBottomSheet()

        fab_quest_checkin.setOnClickListener {
            questViewModel.activateCheckIn(currentQuestID!!)
        }

        fun <T> LiveData<T>.observe(observe: (T?) -> Unit) = observe(this@QuestActivity, Observer {
            observe(it)
        })
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        with(mMap) {
            setMinZoomPreference(12.0f)
            setMaxZoomPreference(19.0f)
            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(LVIV_LAT, LVIV_LNG), 15.5f))
        }

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.silver_style_maps))
        } catch (e: Resources.NotFoundException) {
            Log.e("Quest Activity", e.message)
        }

        mMap.isBuildingsEnabled = false

        mMap.setOnMapClickListener {
            mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            setImagesListVisibility(false)
        }

        inflater = applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        normalMarkerInflated = inflater!!.inflate(R.layout.view_marker_colored, null)
        numberOfNormalMarker = normalMarkerInflated!!.findViewById(R.id.marker_number) as TextView
        distanceNormalMarker = normalMarkerInflated!!.findViewById(R.id.marker_distance) as TextView

        blackMarkerInflated = inflater!!.inflate(R.layout.view_marker_black, null)
        numberOfBlackMarker = blackMarkerInflated!!.findViewById(R.id.black_marker_number) as TextView
        distanceBlackMarker = blackMarkerInflated!!.findViewById(R.id.black_marker_distance) as TextView

        clickedMarkerInflated = inflater!!.inflate(R.layout.view_marker_colored_clicked, null)
        numberOfClickedMarker = clickedMarkerInflated!!.findViewById(R.id.clicked_marker_number) as TextView
        distanceClickedMarker = clickedMarkerInflated!!.findViewById(R.id.clicked_marker_distance) as TextView

        clickedBlackMarkerInflated = inflater!!.inflate(R.layout.view_marker_black_clicked, null)
        numberOfClickedBlackMarker = clickedBlackMarkerInflated!!.findViewById(R.id.clicked_black_marker_number) as TextView
        distanceClickedBlackMarker = clickedBlackMarkerInflated!!.findViewById(R.id.clicked_black_marker_distance) as TextView

        secretMarkerInflated = inflater!!.inflate(R.layout.view_marker_colored, null)

        questViewModel.drawRoute(currentQuestID!!)

        var markersList = emptyList<LocationStructure>()
        val polylinesObserver = Observer<ArrayList<ArrayList<LatLng>>> { polylines -> createPolylines(polylines!!) }
        questViewModel.polylinesLiveData.observe(this, polylinesObserver)

        val markersObserver = Observer<List<LocationStructure>> { markers -> markersList = markers!! }
        questViewModel.locationLiveData.observe(this, markersObserver)

        val distanceObserver = Observer<ArrayList<ArrayList<String>>> { distances -> if (markersList.isNotEmpty() and distances!!.isNotEmpty()) createMarkers(markersList, distances!!) }
        questViewModel.distanceLiveData.observe(this, distanceObserver)


        //set user marker and user location button
        if (fineLocationEnabled() || coarceLocationEnabled()) {
            setUserLocationMarker(mMap)
        }
    }

    //ALERT DIALOGS----------------------------------------------------------------------------------------------

    //ALERT DIALOG SHOWS WHEN GUEST START A QUEST-----------------------------------------------------------------------------------
    private fun alertDialogForGuest() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Quest Alert!")
        alertDialog.setMessage(getString(R.string.quest_alert_for_guest))
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Remind me later", object : DialogInterface.OnClickListener {
            override fun onClick(p0: DialogInterface?, p1: Int) {
                alertDialog.dismiss()
            }
        })
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Sign in now!", object : DialogInterface.OnClickListener {
            override fun onClick(p0: DialogInterface?, p1: Int) {
                startActivity(Intent(this@QuestActivity, UserActivity::class.java).putExtra("fragment", "profile"))
                finish()
            }
        })
        alertDialog.show()
    }

    //ALERT DIALOG SHOWS WHEN USER HAS CHECKED IN LOCATION-----------------------------------------------------------------------------------
    private fun alertDialogSuccessCheckIn() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("WELL DONE!")
        alertDialog.setMessage(getString(R.string.quest_dialog_after_checkin_goodjob)
            +questViewModel.locationLiveData.value!![questViewModel.currentLocationIndex-1].locationName
            +getString(R.string.quest_dialog_after_checkin_reward)
            +"+"+POINTS_REWARD_FOR_LOCATION.toString()+" points")
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "GET NEXT LANDMARK", object : DialogInterface.OnClickListener {
            override fun onClick(p0: DialogInterface?, p1: Int) {
                questViewModel.addPointReward(POINTS_REWARD_FOR_LOCATION)
                alertDialog.dismiss()
            }
        })
        alertDialog.show()
    }

    //-----------------------------------------------------------------------------------------------------

    //USER CURRENT LOCATION---------------------------------------------------------------------------

    private fun fineLocationEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun coarceLocationEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun initUserLocationUpdates(questViewModel: QuestViewModel, locationSystemService: Any) {
        if (fineLocationEnabled() || coarceLocationEnabled()) {
            questViewModel.checkUserLocationUpdates(locationSystemService)
        } else requestPermission()
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
                .draggable(false)
                .title("mPositionMarker"))
        mPositionMarker!!.zIndex = 100.0f
    }

    private fun getBitmap(drawableRes: Int): Bitmap {
        val draw = resources.getDrawable(drawableRes, null)
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(draw.intrinsicWidth, draw.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        draw.setBounds(0, 0, draw.intrinsicWidth, draw.intrinsicHeight)
        draw.draw(canvas)
        return bitmap
    }

    private fun animateMarker(toPosition: LatLng) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val proj = mMap.projection
        val startPoint = proj.toScreenLocation(mPositionMarker!!.position)
        val startLatLng = proj.fromScreenLocation(startPoint)
        val duration = 500

        val interpolator = LinearInterpolator()

        val runnable = object : Runnable {
            override fun run() {
                val t = interpolator.getInterpolation((SystemClock.uptimeMillis() - start).toFloat() / duration)
                val lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                val lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                mPositionMarker!!.position = LatLng(lat, lng)

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this,16)
                } else {
                    mPositionMarker!!.isVisible = true
                }
            }
        }
        runnable.run()
    }

    //-----------------------------------------------------------------------------------------------


    private fun focusMapOnMarkers(markersList: List<Marker>) {
        val builder = LatLngBounds.builder()
        for (marker in markersList) {
            builder.include(marker.position)
        }
        val bounds = builder.build()
        val padding = 65 // offset from edges of the map in pixels
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        mMap.animateCamera(cameraUpdate)
    }


    private fun createPolylines(list: ArrayList<ArrayList<LatLng>>) {
        val finalPolylineList = ArrayList<LatLng>()
        for (i in list) {
            finalPolylineList.addAll(i)
        }
        mMap.addPolyline(PolylineOptions()
                .addAll(finalPolylineList)
                .width(11F)
                .jointType(JointType.BEVEL)
                .color(Color.rgb(145, 121, 241)))
    }

    private fun createMarkers(locationStructureList: List<LocationStructure>, distanceList: ArrayList<ArrayList<String>>) {
        if (locationStructureList.isNotEmpty()) {
            val finalDistanceList = ArrayList<String>()
            for (i in distanceList) {
                finalDistanceList.addAll(i)
            }
            for (i in 0 until locationStructureList.size - 1) {
                locationStructureList[i + 1].distanceToPrevious = finalDistanceList[i]
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
                            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(normalMarkerInflated!!)))
                            .title("mMarker")
                            .zIndex((locationStructureList.size - i).toFloat()))
                    locationStructureList[i].locationID = i + 1
                    marker.setData(locationStructureList[i])
                    markersList.add(marker)
                    if (i < questViewModel.currentLocationIndex) {
                        changeMarkerView(marker, MarkerType.BLACK)
                    }
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
        mMap.setOnMarkerClickListener { marker ->
            if (marker.title != "mPositionMarker") {
                markerClickListenerBody(marker)
            }
            true
        }
    }

    //it is a body of mMap.setOnClickListener
    //it needs twice during this activity:
    //first time, when user clicks on location's marker directly
    //second time, when user click check_in button for the first time and bottom sheet should appear
    private fun markerClickListenerBody(marker: Marker) {
        val locationStructure = marker.getData<LocationStructure>()
        if (!locationStructure.isSecret) {
            if (previousClickedMarker != null && previousClickedMarker != marker) {
                previousClickedMarker!!.zIndex = previousClickedMarkerZindex
                if(questViewModel.getLatLngList(questViewModel.locationLiveData.value!!)
                                .indexOf(previousClickedMarker!!.position) >= questViewModel.currentLocationIndex) {
                    changeMarkerView(previousClickedMarker!!, MarkerType.NORMAL)
                }
            }
            previousClickedMarker = marker
            previousClickedMarkerZindex = marker.zIndex
            if (marker.title == "mMarker") {
                changeMarkerView(marker, MarkerType.CLICKED)
            } else {
                changeMarkerView(marker, MarkerType.BLACK_CLICKED)
            }
            mBottomSheetBehavior!!.isHideable = true
            bottomSheetInfo!!.text = locationStructure.locationDescription
            bottomSheetName!!.text = locationStructure.locationName
            mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED

            setImagesListVisibility(true)
            bottomSheetSkipButton!!.setOnClickListener {
                mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
                checkMarkerViewAfterClick(marker)
                resetCheckInPreparing()
                setImagesListVisibility(false)
            }

            mBottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(view: View, currentState: Int) {
                    when (currentState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            checkMarkerViewAfterClick(marker)
                            resetCheckInPreparing()
                            setImagesListVisibility(false)
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            fab_quest_checkin.hide()
                        }
                    }
                }

                override fun onSlide(p0: View, p1: Float) {
                }

            })
        }
    }

    private fun initBottomSheet() {
        bottomSheet = findViewById(R.id.bottom_sheet)
        bottomSheetName = findViewById(R.id.bottom_sheet_name)
        bottomSheetInfo = findViewById(R.id.bottom_sheet_info)
        bottomSheetSkipButton = findViewById(R.id.bottom_sheet_skip)
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun changeMarkerView(marker: Marker, markerTypeToChange: MarkerType) {
        val distance = marker.getData<LocationStructure>().distanceToPrevious
        val locationId = marker.getData<LocationStructure>().locationID
        when (markerTypeToChange) {
            MarkerType.NORMAL -> {
                distanceNormalMarker!!.text = distance
                numberOfNormalMarker!!.text = locationId!!.toString()
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(normalMarkerInflated!!)))
            }
            MarkerType.BLACK -> {
                marker.title = "mBlackMarker"
                marker.zIndex = 0.0f
                distanceBlackMarker!!.text = "done"
                numberOfBlackMarker!!.text = locationId!!.toString()
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(blackMarkerInflated!!)))
            }
            MarkerType.CLICKED -> {
                distanceClickedMarker!!.text = distance
                numberOfClickedMarker!!.text = locationId!!.toString()
                marker.zIndex = 100.0f
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(clickedMarkerInflated!!)))
            }
            MarkerType.BLACK_CLICKED -> {
                distanceClickedBlackMarker!!.text = "done"
                numberOfClickedBlackMarker!!.text = locationId!!.toString()
                marker.zIndex = 100.0f
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(clickedBlackMarkerInflated!!)))
            }
            MarkerType.SECRET -> {

            }
        }
    }

    //reset value of checkInPreparing var when user click button without result several time
    private fun resetCheckInPreparing() {
        if(questViewModel.checkInPreparing.value == QuestViewModel.CheckInPreparing.BOTTOM_SHEET_UP) {
            questViewModel.checkInPreparing.value = QuestViewModel.CheckInPreparing.NO_CHECK_IN
        }
    }

    //check marker view after its click
    private fun checkMarkerViewAfterClick(marker: Marker) {
        if(questViewModel.getLatLngList(questViewModel.locationLiveData.value!!)
                        .indexOf(previousClickedMarker!!.position) >= questViewModel.currentLocationIndex) {
            changeMarkerView(marker, MarkerType.NORMAL)
        }
        else {
            changeMarkerView(marker, MarkerType.BLACK)
        }
    }


    /**
     * If true smoothly moves images list out, otherwise smoothly hides it
     */
    private fun setImagesListVisibility(shouldBeShown: Boolean) {
        val imageListView = findViewById<View>(R.id.horizontalscrollview_user)

        if (shouldBeShown) {
            imageListView.animate()
                    .translationY(imageListView.height.toFloat())
                    .duration = 300
        } else {
            imageListView.animate().translationY(0f)
        }
    }
}

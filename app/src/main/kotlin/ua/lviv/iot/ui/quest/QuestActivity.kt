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
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.androidmapsextensions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.android.synthetic.main.activity_quest.*
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.map.LocationStructure
import ua.lviv.iot.ui.MainActivity
import ua.lviv.iot.ui.user.UserActivity
import ua.lviv.iot.utils.InjectorUtils
import ua.lviv.iot.utils.LVIV_LAT
import ua.lviv.iot.utils.LVIV_LNG
import ua.lviv.iot.utils.MarkerType


class QuestActivity : AppCompatActivity(), OnMapReadyCallback {
    private val MY_LOCATION_PERMISSIONS_REQUEST = 121

    private lateinit var mMap: GoogleMap
    private var numberOfNormalMarker: TextView? = null
    private var numberOfBlackMarker: TextView? = null
    private var mPositionMarker: Marker? = null
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

    private var bottomSheet: View? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var bottomSheetName: TextView? = null
    private var bottomSheetInfo: TextView? = null
    private var bottomSheetSkipButton: Button? = null
    private val isQuestOn: Boolean = false
    private val currentQuestCategory: Int = 0
    private var locationListFromDatabase: List<LocationStructure>? = null
    private var currentQuestName: String? = null
    private val currentUserId: String? = null
    private val markersList = ArrayList<Marker>()
    private lateinit var questViewModel: QuestViewModel
    private var userCurrentLocation = LatLng(LVIV_LAT, LVIV_LNG)
    private var previousClickedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)
        currentQuestName = intent.getStringExtra("questName")
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getExtendedMapAsync(this)
        val factory = InjectorUtils.provideQuestViewModelFactory()
        questViewModel = ViewModelProviders.of(this, factory).get(QuestViewModel::class.java)
        initUserLocationUpdates(questViewModel, getSystemService(Context.LOCATION_SERVICE))

        questViewModel.userCurrentLocation.observe(this, Observer {
            userCurrentLocation = it!!
            if (mPositionMarker != null) {
                mPositionMarker!!.position = userCurrentLocation
                questViewModel.getUserStatusForQuest(currentQuestName!!)
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
                    Toast.makeText(this, R.string.check_in_success, Toast.LENGTH_SHORT).show()
                    questViewModel.locationHasChecked.value = EventResultStatus.NO_EVENT
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
        //-------------------------------------------------------------------------------------------------
        initBottomSheet()

        fab_quest_checkin.setOnClickListener {
            questViewModel.activateCheckIn(currentQuestName!!)
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

        mMap.setOnMapClickListener {
            mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            setImagesListVisibility(false)
        }

        inflater = applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        normalMarkerInflated = inflater!!.inflate(R.layout.view_marker_colored, null)
        numberOfNormalMarker = normalMarkerInflated!!.findViewById(R.id.marker_number) as TextView
        distanceNormalMarker = normalMarkerInflated!!.findViewById(R.id.marker_distance) as TextView

        blackMarkerInflated = inflater!!.inflate(R.layout.view_marker_black, null)
        numberOfBlackMarker = blackMarkerInflated!!.findViewById(R.id.changed_marker_number) as TextView
        distanceBlackMarker = blackMarkerInflated!!.findViewById(R.id.changed_marker_distance) as TextView

        secretMarkerInflated = inflater!!.inflate(R.layout.view_marker_colored, null)

        questViewModel.drawRoute(currentQuestName!!)

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

    //-----------------------------------------------------------------------------------------------


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
                val locationStructure = marker.getData<LocationStructure>()
                if (!locationStructure.isSecret) {
                    if (previousClickedMarker != null && previousClickedMarker != marker) {
                        changeMarkerView(previousClickedMarker!!, MarkerType.NORMAL)
                    }
                    previousClickedMarker = marker
                    changeMarkerView(marker, MarkerType.BLACK)

                    bottomSheetInfo!!.text = locationStructure.locationDescription
                    bottomSheetName!!.text = locationStructure.locationName
                    mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED

                    setImagesListVisibility(true)
                    bottomSheetSkipButton!!.setOnClickListener {
                        mBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
                        changeMarkerView(marker, MarkerType.NORMAL)

                        setImagesListVisibility(false)
                    }

                    mBottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(view: View, currentState: Int) {
                            when (currentState) {
                                BottomSheetBehavior.STATE_HIDDEN -> {
                                    changeMarkerView(marker, MarkerType.NORMAL)
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
            true
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
                distanceBlackMarker!!.text = distance
                numberOfBlackMarker!!.text = locationId!!.toString()
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromView(blackMarkerInflated!!)))
            }
            MarkerType.SECRET -> {

            }
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

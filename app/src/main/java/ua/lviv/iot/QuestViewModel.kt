package ua.lviv.iot

import android.arch.lifecycle.ViewModel
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.support.design.widget.BottomSheetBehavior
import android.util.Log
import android.view.View
import android.widget.TextView
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.androidmapsextensions.GoogleMap
import com.androidmapsextensions.Marker
import com.androidmapsextensions.MarkerOptions
import com.androidmapsextensions.PolylineOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DatabaseError
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.map.LocationStructure
import ua.lviv.iot.model.map.Quest

class QuestViewModel(): ViewModel(){

    private val markersList = ArrayList<Marker>()
    private var origin: LatLng? = null
    private var counter: Int = 0
    private val polylinesList = ArrayList<LatLng>()
    private var data = ArrayList<LatLng>()
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private var dest: LatLng? = null
    private var requestList = ArrayList<RequestClass>()
    private var requestIndex = 0
    private val distanceList = ArrayList<String>()
    private var locationListFromDatabase:  List<LocationStructure>? = null




}


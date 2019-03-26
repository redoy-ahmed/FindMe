package com.findme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.pin_user_dialog.view.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.InfoWindowAdapter,
    GoogleMap.OnInfoWindowClickListener {

    private var mMap: GoogleMap? = null
    private var mapView: MapView? = null
    internal var latitude: Double = 0.toDouble()
    internal var longitude: Double = 0.toDouble()
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mCurrLocationMarker: Marker? = null
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var latLng: LatLng
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null

    private var mFirebaseDatabase: DatabaseReference? = null
    private var mFirebaseInstance: FirebaseDatabase? = null
    private val TAG = MainActivity::class.java.simpleName
    var pDialog: SweetAlertDialog? = null

    var userId: String? = null

    companion object {
        private const val PLACE_PICKER_REQUEST = 3
        const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private var MIN_UPDATE_INTERVAL = (1000).toLong()
    }

    private val isGooglePlayServicesAvailable: Boolean
        get() {
            val googleAPI = GoogleApiAvailability.getInstance()
            val result = googleAPI.isGooglePlayServicesAvailable(applicationContext)
            if (result != ConnectionResult.SUCCESS) {
                if (googleAPI.isUserResolvableError(result)) {
                    googleAPI.getErrorDialog(this, result, 0).show()
                }
                return false
            }
            return true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext!!)
        initializeWidgets(savedInstanceState)
    }

    private fun initializeWidgets(savedInstanceState: Bundle?) {

        mFirebaseInstance = FirebaseDatabase.getInstance()

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission()
        }

        if (!isGooglePlayServicesAvailable) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.")
        } else {
            Log.d("onCreate", "Google Play Services available. Continuing.")
        }

        mapView = findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.onResume()
        mapView!!.getMapAsync(this)
    }

    override fun onMapReady(mGoogleMap: GoogleMap) {
        mMap = mGoogleMap
        mMap!!.mapType = GoogleMap.MAP_TYPE_TERRAIN
        mMap!!.uiSettings.isMyLocationButtonEnabled = true
        mMap!!.uiSettings.isZoomControlsEnabled = true
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(17F))

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                buildGoogleApiClient()
                mMap!!.isMyLocationEnabled = true
            } else {
                checkLocationPermission()
            }
        } else {
            buildGoogleApiClient()
            mMap!!.isMyLocationEnabled = true
        }

        mMap!!.setInfoWindowAdapter(this)
        mMap!!.setOnInfoWindowClickListener(this)
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(applicationContext)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient!!.connect()
    }

    @SuppressLint("RestrictedApi")
    override fun onConnected(bundle: Bundle?) {
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = MIN_UPDATE_INTERVAL
        mLocationRequest.fastestInterval = MIN_UPDATE_INTERVAL
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
        }

        /*val latLng = LatLng(23.777176, 90.399452)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("My Location")
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.boot_marker))
        mCurrLocationMarker = mMap!!.addMarker(markerOptions)*/
    }

    override fun onConnectionSuspended(i: Int) {

    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    override fun onLocationChanged(location: Location?) {
        mLastLocation = location
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker!!.remove()
        }

        //Place current location marker
        val latLng = LatLng(location?.latitude!!, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("My Location")
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.my_location_marker))
        mCurrLocationMarker = mMap?.addMarker(markerOptions)

        //move map camera
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17F))
    }

    private fun checkLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient()
                        }
                        mMap!!.isMyLocationEnabled = true
                    }
                } else {
                    Toast.makeText(applicationContext, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                val place = PlacePicker.getPlace(applicationContext, data!!)
                var addressText = place.name.toString()
                addressText += "\n" + place.address!!.toString()
                Toast.makeText(applicationContext, addressText, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getInfoWindow(marker: Marker?): View? {
        return null
    }

    override fun getInfoContents(marker: Marker?): View {
        val view = layoutInflater.inflate(R.layout.custom_marker_info_window, null)

        val textViewName = view.findViewById(R.id.textViewName) as TextView
        val labelLocation = view.findViewById(R.id.lbl_location) as TextView
        val labelPhone = view.findViewById(R.id.lbl_price) as TextView

        return view
    }

    override fun onInfoWindowClick(marker: Marker?) {

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.pin_user_dialog, null)

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
            .setTitle("Set Location")
        val mAlertDialog = mBuilder.show()
        mDialogView.shareButton.setOnClickListener {
            mAlertDialog.dismiss()
            val name = mDialogView.nameEditText.text.toString()
            val email = mDialogView.emailEditText.text.toString()
            val phone = mDialogView.phoneEditText.text.toString()
            val location = mDialogView.locationEditText.text.toString()

            shareLocation(name, email, phone, location)
        }
        mDialogView.cancelButton.setOnClickListener {
            mAlertDialog.dismiss()
        }

        /*val intent = Intent(applicationContext, ProfileActivity::class.java)
        startActivity(intent)*/
    }

    private fun shareLocation(name: String, email: String, phone: String, location: String) {

        if (TextUtils.isEmpty(userId)) {
            userId = mFirebaseDatabase!!.push().key
        }

        val user = User(name, email, phone, "0.0", "0.0", location)

        mFirebaseDatabase!!.child(userId!!).setValue(user)
        pDialog!!.cancel()

        addUserChangeListener()
    }

    private fun addUserChangeListener() {

        mFirebaseDatabase!!.child(userId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)

                if (user == null) {
                    Log.e(TAG, "User data is null!")
                    return
                }
                Log.e(TAG, "User data is changed!" + user.name)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read user", error.toException())
            }
        })
    }
}

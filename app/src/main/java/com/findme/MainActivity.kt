package com.findme

import android.Manifest
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
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
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.AccountPicker
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.pin_user_dialog.view.*
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.InfoWindowAdapter,
    GoogleMap.OnInfoWindowClickListener {

    private var mMap: GoogleMap? = null
    private var mapView: MapView? = null
    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mCurrLocationMarker: Marker? = null
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var latLng: LatLng
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null
    private var address: String? = null
    private var userEmail: String? = null

    private val peoples: ArrayList<User> = ArrayList()
    private val locationLog: ArrayList<com.findme.Location> = ArrayList()

    private var mFirebaseDatabase: DatabaseReference? = null
    private var mFirebaseInstance: FirebaseDatabase? = null
    private val TAG = MainActivity::class.java.simpleName
    var pDialog: SweetAlertDialog? = null

    private var userId: String? = null

    private var sharedPreference: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null

    private var isFirstTime: Boolean = true

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
        showPeoples()
    }

    private fun initializeWidgets(savedInstanceState: Bundle?) {

        mFirebaseInstance = FirebaseDatabase.getInstance()
        mFirebaseDatabase = mFirebaseInstance!!.getReference("users")

        sharedPreference = getSharedPreferences("FIND_ME", Context.MODE_PRIVATE)
        editor = sharedPreference?.edit()
        userId = sharedPreference?.getString("userId", "")

        pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog!!.progressHelper.barColor = Color.parseColor("#00bcd4")
        pDialog!!.titleText = "Loading"
        pDialog!!.setCancelable(false)

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

    private fun showPeoples() {

        val sharedPref = getSharedPreferences("NEAR_ME", Context.MODE_PRIVATE)
        pDialog!!.show()

        val database = FirebaseDatabase.getInstance().reference
        val ref = database.child("users")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val user = singleSnapshot.getValue(User::class.java)
                    if (user?.email != sharedPref.getString("email", ""))
                        peoples.add(user!!)
                }
                showPeoplesOnMap()
                pDialog!!.cancel()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException())
            }
        })
    }

    private fun showPeoplesOnMap() {
        for (user in this.peoples) {
            latLng = LatLng(user.lat.toDouble(), user.lng.toDouble())
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            markerOptions.title(user.name)
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.public_location_marker))
            mCurrLocationMarker = mMap!!.addMarker(markerOptions)
        }
    }

    private fun setOnClickListeners() {
        mMap?.setOnMapClickListener {

            if (mCurrLocationMarker != null) {
                mCurrLocationMarker!!.remove()
            }

            latitude = it?.latitude!!
            longitude = it.longitude

            address = getAddress(latitude, longitude)

            val latLng = LatLng(latitude, longitude)
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            markerOptions.title("My Location")
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.my_location_marker))

            val mDialogView = LayoutInflater.from(this).inflate(R.layout.pin_user_dialog, null)

            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)
                .setTitle("Share Location")
            val mAlertDialog = mBuilder.show()

            mDialogView.locationEditText.setText(address)

            mDialogView.shareButton.setOnClickListener {
                val name = mDialogView.nameEditText.text.toString()
                val email = mDialogView.emailEditText.text.toString()
                val phone = mDialogView.phoneEditText.text.toString()
                val location = mDialogView.locationEditText.text.toString()

                when {
                    TextUtils.isEmpty(name) -> Toast.makeText(
                        applicationContext,
                        "Enter name!",
                        Toast.LENGTH_SHORT
                    ).show()
                    TextUtils.isEmpty(email) -> Toast.makeText(
                        applicationContext,
                        "Enter email!",
                        Toast.LENGTH_SHORT
                    ).show()
                    TextUtils.isEmpty(phone) -> Toast.makeText(
                        applicationContext,
                        "Enter phone!",
                        Toast.LENGTH_SHORT
                    ).show()
                    else -> {
                        mAlertDialog.dismiss()
                        shareLocation(name, email, phone, latitude.toString(), longitude.toString(), location)
                        mCurrLocationMarker = mMap?.addMarker(markerOptions)
                    }
                }
            }
            mDialogView.cancelButton.setOnClickListener {
                mAlertDialog.dismiss()
            }
        }
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
        latitude = location?.latitude!!
        longitude = location.longitude
        address = getAddress(latitude, longitude)
        val latLng = LatLng(latitude, longitude)
        setOnClickListeners()

        if (isFirstTime) {
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17F))
            isFirstTime = false
        }
    }

    private fun getAddress(latitude: Double, longitude: Double): String {

        val geoCoder = Geocoder(this, Locale.getDefault())
        val addresses: ArrayList<Address>
        addresses = geoCoder.getFromLocation(latitude, longitude, 1) as ArrayList<Address>

        val address = addresses[0].getAddressLine(0)
        val city = addresses[0].locality
        val country = addresses[0].countryName

        return "$address,$city,$country"
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
        val textViewEmail = view.findViewById(R.id.textViewEmail) as TextView
        val textViewPhone = view.findViewById(R.id.textViewPhone) as TextView
        val textViewLat = view.findViewById(R.id.textViewLat) as TextView
        val textViewLng = view.findViewById(R.id.textViewLng) as TextView
        val textViewLocation = view.findViewById(R.id.textViewLocation) as TextView

        if (marker?.title == "My Location") {
            textViewName.text = "My Location"
            textViewEmail.text = sharedPreference?.getString("email", "")
            textViewPhone.text = sharedPreference?.getString("phone", "")
            textViewLat.text = sharedPreference?.getString("lat", "")
            textViewLng.text = sharedPreference?.getString("lng", "")
            textViewLocation.text = address
            userEmail = sharedPreference?.getString("email", "")
        } else {
            for (user in this.peoples) {
                if (user.name == marker?.title) {
                    textViewName.text = user.name
                    textViewEmail.text = user.email
                    textViewPhone.text = user.phone
                    textViewLat.text = user.lat
                    textViewLng.text = user.lng
                    textViewLocation.text = user.location
                    userEmail = user.email
                }
            }
        }

        locationLog.clear()

        val database = FirebaseDatabase.getInstance().reference
        val ref = database.child("users")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val user = singleSnapshot.getValue(User::class.java)
                    if (user?.email == userEmail) {
                        for (location in user?.locationLog!!) {
                            locationLog.add(location)
                        }
                        break
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException())
            }
        })

        return view
    }

    override fun onInfoWindowClick(marker: Marker?) {
        val intent = Intent(applicationContext, UserLogActivity::class.java)
        intent.putExtra("userEmail", userEmail)
        startActivity(intent)
    }

    private fun shareLocation(
        name: String,
        email: String,
        phone: String,
        latitude: String,
        longitude: String,
        location: String
    ) {
        userId = mFirebaseDatabase!!.push().key
        editor?.putString("userId", userId)
        editor?.putString("email", email)
        editor?.putString("phone", phone)
        editor?.putString("lat", latitude)
        editor?.putString("lng", longitude)
        editor?.commit()

        val locationObj = Location(name, Calendar.getInstance().time.toString(), latitude, longitude, location)

        locationLog.add(locationObj)

        val user = User(name, email, phone, latitude, longitude, location, locationLog)

        mFirebaseDatabase!!.child(userId!!).setValue(user)

        //addUserChangeListener()
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

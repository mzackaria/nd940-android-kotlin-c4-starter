package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.*
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    
    private lateinit var map: GoogleMap

    private lateinit var snackBar : Snackbar

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val ZOOM = 15f
    private var markerChosenPosition : Marker? =  null

    private val REQUEST_LOCATION_PERMISSION = 1

    //region LIFECYCLE
    //setting the position callback to move camera when user's position is acquired
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    moveCamera(location,true)
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View ? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return binding.root
    }

    //setting the snackbar
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        snackBar = Snackbar.make(
            requireView(),
            R.string.location_confirmation, Snackbar.LENGTH_INDEFINITE
        ).setAction(android.R.string.ok) {
            onLocationSelected()
        }
    }
    //endregion

    //region SETUP MAP
    //setting up the map and calling to enable position
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener {
            it?.let {latLng ->
                markerChosenPosition?.remove()
                markerChosenPosition = map.addMarker(MarkerOptions().position(latLng))
                snackBar.show()
            }
        }
        map.setOnCameraMoveListener {
            snackBar.dismiss()
        }
        enableMyLocation()
    }

    fun moveCamera(location : Location, animate : Boolean = false) {
        val lat = location.latitude
        val lng = location.longitude
        val homeLatLng = LatLng(lat, lng)
        if (animate) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, ZOOM))
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, ZOOM))
        }
    }

    //endregion

    //region ENABLE LOCATION
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.setMyLocationEnabled(true)
            checkDeviceLocationSettings()
        }
        else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun isPermissionGranted() : Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }

    /*
         *  When we get the result from asking the user to turn on device location, we call
         *  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
         *  we don't resolve the check to keep the user from seeing an endless loop.
         */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettings(false)
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettings(resolve:Boolean = true) {
        activity?.let {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(activity as Activity)
            val locationSettingsResponseTask =
                settingsClient.checkLocationSettings(builder.build())
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException && resolve){
                    try {
                        startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null);
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Timber.d("Error getting location settings resolution: ${sendEx.message}")
                    }
                } else {
                    Snackbar.make(
                        requireView(),
                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                    ).setAction(android.R.string.ok) {
                        checkDeviceLocationSettings()
                    }.show()
                }
            }
            locationSettingsResponseTask.addOnCompleteListener {
                if ( it.isSuccessful ) {
                    getLocation(locationRequest)
                }
            }
        }

    }
    //endregion

    //region LOCATION
    private fun getLocation(locationRequest: LocationRequest) {
        if (isPermissionGranted()) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())
        }
    }

    private fun onLocationSelected() {
        markerChosenPosition?.let {
            _viewModel.latitude.value = it.position.latitude
            _viewModel.longitude.value = it.position.longitude
            findNavController().popBackStack()
        }
    }
    //endregion

    //region MENU
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.setMapType(MAP_TYPE_NORMAL)
            true
        }
        R.id.hybrid_map -> {
            map.setMapType(MAP_TYPE_HYBRID)
            true
        }
        R.id.satellite_map -> {
            map.setMapType(MAP_TYPE_SATELLITE)
            true
        }
        R.id.terrain_map -> {
            map.setMapType(MAP_TYPE_TERRAIN)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
    //endregion


}

private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
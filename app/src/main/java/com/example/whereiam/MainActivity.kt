package com.example.whereiam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mSoundPool: SoundPool
    var mSoundMap: HashMap<Int, Int> = HashMap()
    private var mLoaded: Boolean = false
    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    private val locationRequestCode = 10
    private lateinit var locationManager: LocationManager
    private var myLocation : Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        geocoder = Geocoder(this, Locale.getDefault())
        mapFragment.getMapAsync(this)
        loadSounds()
    }

    private fun loadSounds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mSoundPool = SoundPool.Builder().setMaxStreams(10).build()
        } else {
            this.mSoundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 0)
        }
        this.mSoundPool.setOnLoadCompleteListener { _, _, _ -> mLoaded = true }
        this.mSoundMap[R.raw.tap_sound] = this.mSoundPool.load(this, R.raw.tap_sound, 1)
    }

    private fun trackLocation() {
        if(hasPermissionCompat(locationPermission)){
            startTrackingLocation()
        } else {
            requestPermisionCompat(arrayOf(locationPermission), locationRequestCode)
        }
    }

    private fun startTrackingLocation() {
        Log.d("TAG", "Tracking location")
        val criteria: Criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        val provider = locationManager.getBestProvider(criteria, true)
        val minTime = 10L
        val minDistance = 10.0F
        try{
            locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener)
        } catch (e: SecurityException){
            Toast.makeText(this, "Nemas dozvolu", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationListener = object: LocationListener {
        override fun onProviderEnabled(provider: String?) { }
        override fun onProviderDisabled(provider: String?) { }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
        override fun onLocationChanged(location: Location?) {
            updateLocationDisplay(location)
            Log.d("TAG", "Location changed")
        }
    }

    private fun updateLocationDisplay(location: Location?) {
        val lat = location?.latitude ?: 0
        val lon = location?.longitude ?: 0
        val position = LatLng(lat as Double, lon as Double)

        myLocation?.remove()
        myLocation = map.addMarker(MarkerOptions().position(position).title("I am here!"))
        map.moveCamera(CameraUpdateFactory.newLatLng(position))

        val addresses = geocoder.getFromLocation(lat,lon,1)[0].getAddressLine(0).split(",")

        val city = addresses[2]
        val street = addresses[0]
        val country = addresses[3]
        locationDetails.text =
            "Geogrfska širina: $lat\nGeografska dužina: $lon\nDržava: $country\nGrad: $city\nStreet: $street\n"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        trackLocation()
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        map.uiSettings.isZoomControlsEnabled = true

        map.setOnMapClickListener {
            playSound(R.raw.tap_sound)
            map.addMarker(MarkerOptions().position(it))
        }
    }
    private fun playSound(soundToPlay: Int) {
        val soundID = this.mSoundMap[soundToPlay] ?: 0
        this.mSoundPool.play(soundID, 1f, 1f, 1, 0, 1f)

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        when(requestCode){
            locationRequestCode -> {
                if(grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    trackLocation()
                else
                    Toast.makeText(this,"Permission not granted", Toast.LENGTH_SHORT).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }



}

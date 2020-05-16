package com.example.whereiam

import android.Manifest
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.*
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val CHANNEL_ID = "imageNotification"
    private lateinit var mSoundPool: SoundPool
    var mSoundMap: HashMap<Int, Int> = HashMap()
    private var mLoaded: Boolean = false
    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    private val locationRequestCode = 10
    private lateinit var locationManager: LocationManager
    private var myLocation : Marker? = null
    private var myCity = "noLocation"
    //camera
    val REQUEST_IMAGE_CAPTURE = 1
    private val PERMISSION_REQUEST_CODE: Int = 101
    private var mCurrentPhotoPath: String? = null
    private var file: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()
        setUpMaps()
        loadSounds()
        setUpUi()
    }

    private fun setUpMaps() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        geocoder = Geocoder(this, Locale.getDefault())
        mapFragment.getMapAsync(this)
    }

    private fun setUpUi() {
        cameraBtn.setOnClickListener{startCamera()}
    }

    private fun startCamera() {
        if (checkPersmission())
            takePicture()
        else requestPermission()
    }
    //sounds
    private fun loadSounds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mSoundPool = SoundPool.Builder().setMaxStreams(10).build()
        } else {
            this.mSoundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 0)
        }
        this.mSoundPool.setOnLoadCompleteListener { _, _, _ -> mLoaded = true }
        this.mSoundMap[R.raw.tap_sound] = this.mSoundPool.load(this, R.raw.tap_sound, 1)
    }
    private fun playSound(soundToPlay: Int) {
        val soundID = this.mSoundMap[soundToPlay] ?: 0
        this.mSoundPool.play(soundID, 1f, 1f, 1, 0, 1f)
    }
    //maps
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

        myCity = addresses[2]
        val street = addresses[0]
        val country = addresses[3]
        locationDetails.text =
            "Geogrfska širina: $lat\nGeografska dužina: $lon\nDržava: $country\nGrad: $myCity\nStreet: $street\n"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        trackLocation()
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        map.uiSettings.isZoomControlsEnabled = true


        map.setOnMapClickListener {
            playSound(R.raw.tap_sound)
            map.addMarker(MarkerOptions().position(it)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))
        }
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
                    Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show()
            }
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    &&grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                } else {
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
                }
                return
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }

    //camera
    @Throws(IOException::class)
    private fun createFile(): File {
        // Create an image file name
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "$myCity",
            ".jpg",
            storageDir
        ).apply {
            mCurrentPhotoPath = absolutePath
        }
    }
    private fun takePicture() {
        val intent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file: File = createFile()
        this.file = file
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "com.example.android.fileprovider",
            file
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT,uri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }
    private fun checkPersmission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
            READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, CAMERA),
            PERMISSION_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
           displayImageNotification()
        }
    }


    //notifikacija
    private fun displayImageNotification() {
        val intent = Intent(Intent.ACTION_VIEW) //
            .setDataAndType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) FileProvider.getUriForFile(
                    this,
                    "com.example.android.fileprovider",
                    this.file!!
                ) else Uri.fromFile(file),
                "image/*"
            ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        var notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("New Image!")
            .setContentText("Tap to open")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(1001, notification)
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

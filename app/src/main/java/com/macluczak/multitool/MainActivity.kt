package com.macluczak.multitool

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import java.sql.Time
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

const val notifiactionID = 1
const val channelID = 0


class MainActivity : AppCompatActivity(), SensorEventListener, OnMapReadyCallback , LocationListener{

    private lateinit var mMap: GoogleMap
    private lateinit var longitudeTxt: TextView
    private lateinit var latitudeTxt: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var  myLocation : LatLng
    private lateinit var xTxt: TextView
    private lateinit var yTxt: TextView
    private lateinit var zTxt: TextView
    private lateinit var batteryTxt: TextView
    private lateinit var lightTxt: TextView
    private lateinit var ambientTemp: TextView
    private lateinit var deviceTemp: TextView
    private lateinit var sensorManager: SensorManager
    private lateinit var phonePosition: ImageView
    private lateinit var clockTxt : TextClock
    private lateinit var alertButton: Button
    private lateinit var alarmManager: AlarmManager






    private fun setUpSensor(){
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this,
            it,
            SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST)
        }

        val pm: PackageManager = packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_AMBIENT_TEMPERATURE)) {
           ambientTemp.text = "Sensor not found!"
        }
        else{
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.also {
                sensorManager.registerListener(this,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.also {
            sensorManager.registerListener(this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.also {
            sensorManager.registerListener(this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST)
        }



    }

    private fun getBatteryLevel(){
        val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batLevel:Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        batteryTxt.text = "${batLevel}%"
    }

    private fun setRecord(saveData: String){
        val sharedScore = this.getSharedPreferences("com.example.myapplication.shared",0)
        val edit = sharedScore.edit()
        edit.putString("data", saveData)
        edit.apply()
    }


    private fun hideSystemBars() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun createNotificationChannel(){
//        val name = "Notif Channel"
//        val desc = "A description of the Channel"
//        val importance = NotificationManager.IMPORTANCE_DEFAULT
//        val channel = NotificationChannel(channelID.toString(), name, importance)
//        channel.description = desc
//        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.createNotificationChannel(channel)
//    }


//    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemBars()

        Places.initialize(getApplicationContext(), BuildConfig.MAP_KEY)

        val temperatureCard = findViewById<LinearLayout>(R.id.temperatureLayout)
        val temperatureDrawable = temperatureCard.background as AnimationDrawable
        temperatureDrawable.apply{

            setExitFadeDuration(2000)

        }.start()

        val lightLayout = findViewById<RelativeLayout>(R.id.lightLayout)
        val lightDrawable = lightLayout.background as AnimationDrawable
        lightDrawable.apply{

            setExitFadeDuration(2000)

        }.start()

        val batteryLayout = findViewById<RelativeLayout>(R.id.batteryLayout)
        val batteryDrawable = batteryLayout.background as AnimationDrawable
        batteryDrawable.apply{

            setExitFadeDuration(2000)

        }.start()


        longitudeTxt = findViewById(R.id.longitude)
        latitudeTxt = findViewById(R.id.latitude)
        phonePosition = findViewById(R.id.phonePosition)
        xTxt = findViewById(R.id.x)
        yTxt = findViewById(R.id.y)
        zTxt = findViewById(R.id.z)
        lightTxt= findViewById(R.id.lightTxt)
        ambientTemp = findViewById(R.id.ambientTemp)
        deviceTemp = findViewById(R.id.deviceTemp)
        batteryTxt = findViewById(R.id.batteryTxt)
        clockTxt = findViewById(R.id.txtClok)
        alertButton = findViewById(R.id.alertButton)





        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)



        val gridView = findViewById<GridLayout>(R.id.gridview)
        ViewCompat.setNestedScrollingEnabled(gridView,true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager


//        createNotificationChannel()

        getLastKnownLocation()

        setUpSensor()

        setRecord(ambientTemp.text.toString())

        checkAlarm()

        clockTxt.addTextChangedListener(){
            getBatteryLevel()
        }

        alertButton.setOnClickListener {

            if(alertButton.text == "ON"){

                setAlarm()
            alertButton.text = "OFF"
            }
            else if(alertButton.text == "OFF"){
                cancelAlarm()
                alertButton.text = "ON"
            }

        }




    }
    private fun setAlarm() {

        val intent = Intent(applicationContext, MyAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+60000,60000, pendingIntent)
        Log.d("BROADCAST", "UTWORZONO")
        Toast.makeText(this, "Alarm is set", Toast.LENGTH_SHORT).show()
    }
    private fun cancelAlarm() {

        val intent = Intent(this, MyAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
        pendingIntent.cancel()
        alarmManager.cancel(pendingIntent)

        Log.d("BROADCAST", "CANCEL")
    }

    private fun checkAlarm(){
        val intent = Intent(this, MyAlarm::class.java)
        var isWorking: Boolean = (PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_NO_CREATE) != null)
        Log.d("BROADCAST", "Istnieje ${isWorking}")

        if(isWorking){
            alertButton.text = "OFF"
        }
        else{
            alertButton.text = "ON"
        }
    }



    class MyAlarm : BroadcastReceiver() {


        override fun onReceive(context: Context?, intent: Intent) {

            val sharedScore = context?.getSharedPreferences("com.example.myapplication.shared",0)
            val data = sharedScore?.getString("data", 0.toString())


            Log.d("BROADCAST", "UDALO SIE ODEBRAC ${data}")

//            val notification = context?.let { NotificationCompat.Builder(it, channelID.toString())
//                .setSmallIcon(R.drawable.ic_multitool_foreground)
//                .setContentTitle("Temperature Alert!")
//                .setContentText("current temperature ${data}")
//                .build()}
//
//            val manager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            manager.notify(notifiactionID, notification)



        }
    }

    override fun onResume() {
        super.onResume()
        setUpSensor()
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            xTxt.text = "x: ${x.toInt()}"
            yTxt.text = "y: ${y.toInt()}"
            zTxt.text = "z: ${z.toInt()}"

            phonePosition.apply {
                rotationX = -y * 5f
                rotationY = -x * 5f

                rotation = -x

                translationX = -x * -5
                translationY = -y * 5
            }

        }

        if(event?.sensor?.type == Sensor.TYPE_AMBIENT_TEMPERATURE){
            ambientTemp.text = "${event.values[0].toInt()} \u2103"
            setRecord(ambientTemp.text.toString())
        }

        if(event?.sensor?.type == Sensor.TYPE_PRESSURE){
            deviceTemp.text = "${event.values[0].toInt()} hPa"
        }

        if(event?.sensor?.type == Sensor.TYPE_LIGHT){
            lightTxt.text = "${event.values[0].toInt()} lx"
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }




    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.setAllGesturesEnabled(false);

    }

    override fun onLocationChanged(p0: Location) {
        getLastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location->
                if (location != null) {
                    latitudeTxt.text = location.latitude.toString()
                    longitudeTxt.text = location.longitude.toString()
                    myLocation = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(myLocation).title("My current location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15.0f))

                }


            }

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


}


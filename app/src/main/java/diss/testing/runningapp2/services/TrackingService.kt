package diss.testing.runningapp2.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.Action
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import diss.testing.runningapp2.R
import diss.testing.runningapp2.other.Constants.ACTION_PAUSE_SERVICE
import diss.testing.runningapp2.other.Constants.ACTION_SET_LEEWARD
import diss.testing.runningapp2.other.Constants.ACTION_SET_WINDWARD
import diss.testing.runningapp2.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import diss.testing.runningapp2.other.Constants.ACTION_START_OR_RESUME_SERVICE
import diss.testing.runningapp2.other.Constants.ACTION_STOP_SERVICE
import diss.testing.runningapp2.other.Constants.FASTEST_UPDATE_INTERVAL
import diss.testing.runningapp2.other.Constants.LOCATION_UPDATE_INTERVAL
import diss.testing.runningapp2.other.Constants.NOTIFICATION_CHANNEL_ID
import diss.testing.runningapp2.other.Constants.NOTIFICATION_CHANNEL_NAME
import diss.testing.runningapp2.other.Constants.NOTIFICATION_ID
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_DEFAULT
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_RIVERBANK
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_TACK_ON_WHISTLE
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_TIME_TO_LINE
import diss.testing.runningapp2.other.Constants.TIMER_UPDATE_INTERVAL
import diss.testing.runningapp2.other.TrackingUtility
import diss.testing.runningapp2.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

typealias ListOfSpeeds = MutableList<Float>
typealias ListOfListOfSpeeds = MutableList<ListOfSpeeds>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    private var firstRun = true

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currNotificationBuilder: NotificationCompat.Builder

    var serviceKilled = false
    var windwardSet = false
    var leewardSet = false



    companion object {
        val timeSailedInSeconds = MutableLiveData<Long>()
        val timeSailedInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val points = MutableLiveData<Polylines>()
        val speedPoints = MutableLiveData<ListOfListOfSpeeds>()
        var windwardLocation = MutableLiveData<LatLng?>()
        var leewardLocation = MutableLiveData<LatLng?>()
        var bothMarksSet = MutableLiveData<Boolean>()
        var pointTimeStamps = MutableLiveData<MutableList<Long>>()
    }

    private fun postInitialValues() {
        bothMarksSet.postValue(false)
        isTracking.postValue(false)
        points.postValue(mutableListOf())
        speedPoints.postValue(mutableListOf())
        pointTimeStamps.postValue(mutableListOf())
        timeSailedInSeconds.postValue(0L)
        timeSailedInMillis.postValue(0L)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        currNotificationBuilder = baseNotificationBuilder
        isTracking.observe(this, Observer {
            updateTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(firstRun) {
                        myStartForegroundService()
                        firstRun = false
                    } else {
                        startTimer()
                        Timber.d("Resuming service")
                    }
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped service")
                    killService()
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                    pauseService()
                }
                ACTION_SET_WINDWARD -> {
                    Timber.d("Windward mark location set")
                    setMarkLocation(2)
                }
                ACTION_SET_LEEWARD -> {
                    Timber.d("Leeward mark location set")
                    setMarkLocation(1)
                }
                SESSION_TYPE_DEFAULT -> {
                    bothMarksSet.postValue(true)
                }
                SESSION_TYPE_TACK_ON_WHISTLE -> {
                    bothMarksSet.postValue(true)
                }
                SESSION_TYPE_RIVERBANK -> {
                    bothMarksSet.postValue(false)
                }
                SESSION_TYPE_TIME_TO_LINE -> {
                    bothMarksSet.postValue(false)
                }

            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    //Constructors for managing the timer
    private var isTimerEnabled = false
    private var lapTime = 0L
    private var totalTimeSailed = 0L
    private var timeStarted = 0L
    private var previousSecondTimeStamp = 0L

    private fun startTimer() {
        addEmptyPolylinePoints()
        addEmptyPolylineSpeedPoints()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                lapTime = System.currentTimeMillis() - timeStarted
                val totalTime = totalTimeSailed + lapTime
                timeSailedInMillis.postValue(totalTime)

                if(timeSailedInMillis.value!! >= previousSecondTimeStamp + 1000L) {
                    timeSailedInSeconds.postValue(timeSailedInSeconds.value!! + 1)
                    previousSecondTimeStamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            totalTimeSailed += lapTime
        }
    }

    private fun pauseService() {
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if(isTracking) "Pause" else "Resume"
        val pendingIntent = if(isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).also {
                it.action = ACTION_PAUSE_SERVICE
            }
//            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
            PendingIntent.getService(
                this,
                1,
                pauseIntent,
                FLAG_UPDATE_CURRENT or FLAG_MUTABLE,
            )
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).also {
                it.action = ACTION_START_OR_RESUME_SERVICE
            }
//            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
            PendingIntent.getService(
                this,
                2,
                resumeIntent,
                FLAG_UPDATE_CURRENT or FLAG_MUTABLE,
            )
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        currNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(currNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }

        if(!serviceKilled) {
            currNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
            notificationManager.notify(NOTIFICATION_ID, currNotificationBuilder.build())
        }
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if(isTracking.value!!) {
                result.locations.let { locations ->
                    for(location in locations) {
                        addPoints(location)
                        Timber.d("new location: ${location.latitude}, ${location.longitude} speed: ${location.speed}")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateTracking(isTracking : Boolean) {
        if(isTracking) {
            if(TrackingUtility.hasLocationPermissions(this)) {
                val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL).apply {
                    setMinUpdateDistanceMeters(0.0F)
                    setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                }.build()
                fusedLocationProviderClient.requestLocationUpdates(
                    req,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }

        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }


    private fun addPoints(location: Location?) {
        location?.let {
            val position = LatLng(location.latitude, location.longitude)
            val speed = location.speed
            points.value?.apply {
                last().add(position)
                points.postValue(this)
            }
            speedPoints.value?.apply {
                last().add(speed)
                speedPoints.postValue(this)
            }
            val newList = pointTimeStamps.value
            newList?.add(timeSailedInMillis.value!!)
            pointTimeStamps.postValue(newList!!)
        }
    }

    private fun addEmptyPolylinePoints() = points.value?.apply {
        add(mutableListOf())
        points.postValue(this)
    } ?: points.postValue(mutableListOf(mutableListOf()))

    private fun addEmptyPolylineSpeedPoints() = speedPoints.value?.apply {
        add(mutableListOf())
        speedPoints.postValue(this)
    } ?: speedPoints.postValue(mutableListOf(mutableListOf()))

    private fun myStartForegroundService() {
        startTimer()
        isTracking.postValue(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }


        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeSailedInSeconds.observe(this, Observer {
            if(!serviceKilled) {
                val notification = currNotificationBuilder
                    .setContentText(TrackingUtility.getFormattedTime(it * 1000))

                notificationManager.notify(NOTIFICATION_ID, notification.build())

            }
        })
    }

    private fun killService() {
        serviceKilled = true
        firstRun = true
        pauseService()
        postInitialValues()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun setMarkLocation(markNumber : Int) {
        if(TrackingUtility.hasLocationPermissions(this)) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    val position = location?.let { LatLng(it.latitude, location.longitude) }
                    if(markNumber == 1) {
                        leewardLocation.postValue(position)
                        Timber.d("mark location: ${location?.latitude}, ${location?.longitude}")
                        leewardSet = true
                        if(windwardSet) {
                            bothMarksSet.postValue(true)
                        }
                    } else if(markNumber == 2) {
                        windwardLocation.postValue(position)
                        Timber.d("mark location: ${location?.latitude}, ${location?.longitude}")
                        windwardSet = true
                        if(leewardSet) {
                            bothMarksSet.postValue(true)
                        }
                    }
                }
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}
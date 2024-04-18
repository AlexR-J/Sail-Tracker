package diss.testing.runningapp2.other

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import diss.testing.runningapp2.services.Polyline
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.sql.Time
import java.util.concurrent.TimeUnit

object TrackingUtility {

    fun hasLocationPermissions(context: Context) =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    fun calculatePolylineLength(polyline: Polyline): Float{
        var distance = 0F
        for(i in 0..polyline.size - 2) {
            val point1 = polyline[i]
            val point2 = polyline[i+1]
            val results = FloatArray(1)
            Location.distanceBetween(
                point1.latitude,
                point1.longitude,
                point2.latitude,
                point2.longitude,
                results
            )
            distance += results[0]
        }
        return distance

    }

    fun getFormattedTime(ms: Long, useMillis: Boolean = false): String {
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        if(!useMillis) {
            val formattedString = "${if(hours < 10) "0" else ""}$hours:" +
                    "${if(minutes < 10) "0" else ""}$minutes:" +
                    "${if(seconds < 10) "0" else ""}$seconds"
            return formattedString
        }
        milliseconds -= TimeUnit.SECONDS.toMillis(seconds)
        milliseconds /= 10
        val formattedString =  "${if(hours < 10) "0" else ""}$hours:" +
                "${if(minutes < 10) "0" else ""}$minutes:" +
                "${if(seconds < 10) "0" else ""}$seconds:" +
                "${if(milliseconds < 10) "0" else ""}$milliseconds"
        return formattedString
    }

    fun getSecondsToMillis(seconds: Long): Long {
        return TimeUnit.SECONDS.toMillis(seconds)
    }

    fun getDistanceBetween(point1: LatLng, point2: LatLng): Float {
        val latLngArray = FloatArray(1)
        Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, latLngArray)
        return latLngArray[0]
    }
}
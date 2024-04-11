package diss.testing.runningapp2.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import diss.testing.runningapp2.other.MarkerList
import diss.testing.runningapp2.other.MarkerTimeStampList
import diss.testing.runningapp2.other.PointTimeStamps
import diss.testing.runningapp2.other.PolylinesList
import diss.testing.runningapp2.other.RedPointsList
import diss.testing.runningapp2.other.SpeedsList
import diss.testing.runningapp2.services.ListOfSpeeds
import diss.testing.runningapp2.services.Polyline

@Entity(tableName = "session_table")
data class SessionClass(

    var img: Bitmap?,
    var timestamp: Long,
    var avgSpeedInKmp: Float,
    var distanceInMeters: Int,
    var timeInMillis: Long,
    var caloriesBurned: Int,
    var points: PolylinesList,
    var speeds: SpeedsList,
    var redPoints: RedPointsList,
    var markers: MarkerList,
    var markerTimeStamps: MarkerTimeStampList,
    var pointTimeStamps: PointTimeStamps,
    var sessionId: Int

) {
    @PrimaryKey(autoGenerate = true)
    var idAuto: Int? = null
}
package diss.testing.runningapp2.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.google.gson.Gson
import diss.testing.runningapp2.other.MarkerList
import diss.testing.runningapp2.other.MarkerTimeStampList
import diss.testing.runningapp2.other.PointTimeStamps
import diss.testing.runningapp2.other.PolylinesList
import diss.testing.runningapp2.other.RedPointsList
import diss.testing.runningapp2.other.SpeedsList
import java.io.ByteArrayOutputStream

class Converters {
    private val gson = Gson()


    @TypeConverter
    fun toBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @TypeConverter
    fun fromBitmap(bmp: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun fromString(string: String): PolylinesList {
        return gson.fromJson(string, PolylinesList::class.java)
    }

    @TypeConverter
    fun toString(entity: PolylinesList): String {
        return gson.toJson(entity)
    }

    @TypeConverter
    fun toStringSpeed(entity: SpeedsList): String {
        return gson.toJson(entity)
    }

    @TypeConverter
    fun fromStringSpeed(string: String): SpeedsList {
        return gson.fromJson(string, SpeedsList::class.java)
    }

    @TypeConverter
    fun toStringRedPoints(entity: RedPointsList): String {
        return gson.toJson(entity)
    }

    @TypeConverter
    fun fromStringRedPoints(string: String): RedPointsList {
        return gson.fromJson(string, RedPointsList::class.java)
    }

    @TypeConverter
    fun fromStringMarkers(string: String): MarkerList {
        return gson.fromJson(string, MarkerList::class.java)
    }

    @TypeConverter
    fun toStringMarkers(entity: MarkerList): String {
        return gson.toJson(entity)
    }

    @TypeConverter
    fun fromStringMarkerTimestamps(string: String): MarkerTimeStampList {
        return gson.fromJson(string, MarkerTimeStampList::class.java)
    }

    @TypeConverter
    fun toStringMarkerTimestamps(entity: MarkerTimeStampList): String {
        return gson.toJson(entity)
    }

    @TypeConverter
    fun fromStringPointTimestamps(string: String): PointTimeStamps {
        return gson.fromJson(string, PointTimeStamps::class.java)
    }

    @TypeConverter
    fun toStringPointTimestamps(entity: PointTimeStamps): String {
        return gson.toJson(entity)
    }
}
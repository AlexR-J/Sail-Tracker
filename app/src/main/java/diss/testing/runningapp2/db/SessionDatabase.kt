package diss.testing.runningapp2.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SessionClass::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SessionDatabase: RoomDatabase() {

    abstract fun getRunDao(): RunDAO
}
package diss.testing.runningapp2.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.sql.Timestamp

@Dao
interface RunDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run : SessionClass)

    @Delete
    suspend fun deleteRun(run: SessionClass)

    @Query("SELECT * FROM session_table ORDER BY timestamp DESC")
    fun getAllSessionsSortedByDate(): LiveData<List<SessionClass>>

    @Query("SELECT * FROM session_table ORDER BY timeInMillis ASC")
    fun getAllSessionsSortedByTimeInMillis(): LiveData<List<SessionClass>>

    @Query("SELECT * FROM session_table ORDER BY distanceInMeters DESC")
    fun getAllSessionsSortedByDistance(): LiveData<List<SessionClass>>

    @Query("SELECT * FROM session_table ORDER BY avgSpeedInKmp ASC")
    fun getAllSessionsSortedByAverageSpeed(): LiveData<List<SessionClass>>

    @Query("SELECT * FROM session_table ORDER BY caloriesBurned ASC")
    fun getAllSessionsSortedByCaloriesBurned(): LiveData<List<SessionClass>>

    @Query("SELECT SUM(timeInMillis) FROM session_table")
    fun getTotalTimeInMillis(): LiveData<Long>

    @Query("SELECT SUM(caloriesBurned) FROM session_table")
    fun getTotalCaloriesBurnt(): LiveData<Int>

    @Query("SELECT SUM(distanceInMeters) FROM session_table")
    fun getTotalDistance(): LiveData<Int>

    @Query("SELECT AVG(avgSpeedInKmp) FROM session_table")
    fun getTotalAverageSpeed(): LiveData<Float>

    @Query("SELECT * FROM session_table WHERE timestamp=:timestamp")
    fun getBySessionId(timestamp: Float): SessionClass?
}
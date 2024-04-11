package diss.testing.runningapp2.repositories

import androidx.lifecycle.MutableLiveData
import diss.testing.runningapp2.db.SessionClass
import diss.testing.runningapp2.db.RunDAO
import timber.log.Timber
import javax.inject.Inject

class MainRepository @Inject constructor(
    val runDao: RunDAO
) {
    suspend fun insertRun(run: SessionClass) = runDao.insertRun(run)

    suspend fun deleteRun(run: SessionClass) = runDao.deleteRun(run)

    fun getAllSessionsSortedByDate() = runDao.getAllSessionsSortedByDate()

    fun getAllSessionsSortedByDistance() = runDao.getAllSessionsSortedByDistance()

    fun getAllSessionsSortedByAvgSpeed() = runDao.getAllSessionsSortedByAverageSpeed()

    fun getAllSessionsSortedByTimeInMillis() = runDao.getAllSessionsSortedByTimeInMillis()

    fun getAllSessionsSortedByCaloriesBurned() = runDao.getAllSessionsSortedByCaloriesBurned()

    fun getTotalDistance() = runDao.getTotalDistance()

    fun getTotalCaloriesBurned() = runDao.getTotalCaloriesBurnt()

    fun getTotalTimeInMillis() = runDao.getTotalTimeInMillis()

    fun getTotalAvgSpeed() = runDao.getTotalAverageSpeed()

    fun getBySessionId(id: Long) : SessionClass {
        Timber.d("Searching for session with id: $id 2222")
        return getBySessionId(id)
    }
}
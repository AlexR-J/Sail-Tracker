package diss.testing.runningapp2.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import diss.testing.runningapp2.db.SessionClass
import diss.testing.runningapp2.other.SortType
import diss.testing.runningapp2.repositories.MainRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {


    val sessionSortedByDate = mainRepository.getAllSessionsSortedByDate()
    val sessionsSortedByCaloriesBurned = mainRepository.getAllSessionsSortedByCaloriesBurned()
    val sessionsSortedByTime = mainRepository.getAllSessionsSortedByTimeInMillis()
    val sessionSortedByAvgSpeed = mainRepository.getAllSessionsSortedByAvgSpeed()
    val sessionsSortedByDistance = mainRepository.getAllSessionsSortedByDistance()


    val sessionsLiveData = MediatorLiveData<List<SessionClass>>()
    val sessionLiveData = MediatorLiveData<SessionClass>()
    var sortType = SortType.DATE

    init {
        sessionsLiveData.addSource(sessionSortedByDate) { result ->
            if(sortType == SortType.DATE) {
                result?.let{ sessionsLiveData.value = it}
            }
        }
        sessionsLiveData.addSource(sessionsSortedByDistance) { result ->
            if(sortType == SortType.DISTANCE) {
                result?.let{ sessionsLiveData.value = it}
            }
        }
        sessionsLiveData.addSource(sessionsSortedByCaloriesBurned) { result ->
            if(sortType == SortType.CALORIES_BURNED) {
                result?.let{ sessionsLiveData.value = it}
            }
        }
        sessionsLiveData.addSource(sessionsSortedByTime) { result ->
            if(sortType == SortType.SESSION_TIME) {
                result?.let{ sessionsLiveData.value = it}
            }
        }
        sessionsLiveData.addSource(sessionSortedByAvgSpeed) { result ->
            if(sortType == SortType.AVG_SPEED) {
                result?.let{ sessionsLiveData.value = it}
            }
        }
    }

    fun sortSessions(sortType: SortType) = when(sortType) {
        SortType.DATE -> sessionSortedByDate.value?.let{sessionsLiveData.value = it}
        SortType.SESSION_TIME -> sessionsSortedByTime.value?.let{sessionsLiveData.value = it}
        SortType.AVG_SPEED -> sessionSortedByAvgSpeed.value?.let{sessionsLiveData.value = it}
        SortType.CALORIES_BURNED -> sessionsSortedByCaloriesBurned.value?.let{sessionsLiveData.value = it}
        SortType.DISTANCE -> sessionsSortedByDistance.value?.let{sessionsLiveData.value = it}
    }.also {
        this.sortType = sortType
    }



    fun insertSession(session: SessionClass) = viewModelScope.launch {
        mainRepository.insertRun(session)
    }

}
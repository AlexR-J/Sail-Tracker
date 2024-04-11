package diss.testing.runningapp2.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import diss.testing.runningapp2.db.SessionClass
import diss.testing.runningapp2.repositories.MainRepository
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {



    companion object {
        var searchId = MutableLiveData<Long>()
        var foundSession = MutableLiveData<SessionClass?>()
    }

    fun setValuesById() {
        val session = searchId.value?.let { mainRepository.getBySessionId(it) }
        foundSession.postValue(session)
    }
}
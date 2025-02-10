package org.piramalswasthya.cho.ui.login_activity.cho_login.hwc

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.piramalswasthya.cho.database.shared_preferences.PreferenceDao
import org.piramalswasthya.cho.model.UserCache
import org.piramalswasthya.cho.repositories.UserRepo
import org.piramalswasthya.cho.ui.login_activity.cho_login.outreach.OutreachViewModel
import timber.log.Timber
import javax.inject.Inject
@HiltViewModel
class HwcViewModel @Inject constructor(
    private val userRepo: UserRepo,
    private val pref: PreferenceDao,


    ) : ViewModel() {
    enum class State {
        IDLE,
        LOADING,
        SAVING,
        ERROR_INPUT,
        ERROR_SERVER,
        ERROR_NETWORK,
        SUCCESS
    }

    val _state = MutableLiveData(OutreachViewModel.State.IDLE)
    val state: LiveData<OutreachViewModel.State>
        get() = _state

    private val _currentLocation = MutableLiveData<Location>(null)
    val currentLocation: LiveData<Location?>
        get() = _currentLocation

    private val _loggedInUser = MutableLiveData<UserCache?>(null)
    val loggedInUser: LiveData<UserCache?>
        get() = _loggedInUser

    private val _isLoggedIn = MutableLiveData<Boolean>(null)
    val isLoggedIn: LiveData<Boolean?>
        get() = _isLoggedIn
    fun rememberUser(username: String,password: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pref.registerLoginCred(username,password)
            }
        }
    }
    fun fetchRememberedPassword(): String? =
        pref.getRememberedPassword()
    suspend fun authUser(
        username: String,
        password: String,
        loginType: String?,
        selectedOption: String?,
        loginTimeStamp: String?,
        logoutTimeStamp: String?,
        lat: Double?,
        long: Double?,
        logoutType: String?,
        context: Context
    ) {
        _state.value  = OutreachViewModel.State.SAVING
        viewModelScope.launch {
            _state.value = userRepo.authenticateUser(
                username,
                password,
                loginType,
                selectedOption,
                loginTimeStamp,
                logoutTimeStamp,
                lat,
                long,
                null,
                logoutType,
            false,
            context)
//            userRepo.setOutreachProgram(selectedOption,timestamp,lat,long)
//            _state.value = State.SUCCESS
        }
        _isLoggedIn.value = true
    }
    suspend fun setOutreachDetails(
        loginType: String?,
        selectedOption: String?,
        loginTimeStamp: String?,
        logoutTimeStamp: String?,
        lat: Double?,
        long: Double?,
        logoutType: String?,
        isOutOfReach : Boolean?
    ){
        userRepo.setOutreachProgram(
            loginType,
            selectedOption,
            loginTimeStamp,
            logoutTimeStamp,
            lat,
            long,
            logoutType,
            null,
            isOutOfReach
        )
    }
    fun forgetUser() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pref.deleteLoginCred()
            }
        }
    }

    fun getLoggedInUserDetails(){
        viewModelScope.launch {
            try {
                val user = userRepo.getUserCacheDetails()
                _loggedInUser.value = user
                _isLoggedIn.value = user?.loggedIn
            } catch (e: Exception){
                Timber.d("Error in calling getLoggedInUserDetails() $e")
                _isLoggedIn.value = false
            }
        }
    }
    fun resetState() {
        _state.value = OutreachViewModel.State.IDLE
    }

    fun setLocation(location: Location) {
        _currentLocation.value = location
    }

    fun setLogin(b: Boolean) {
        _isLoggedIn.value = b
    }
}
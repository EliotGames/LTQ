package ua.lviv.iot.ui.profile

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import ua.lviv.iot.model.firebase.FirebaseLoginManager

class ProfileViewModel : ViewModel() {

    var isUserRegistered: LiveData<Boolean> = FirebaseLoginManager._isUserLoggedIn
}
package ua.lviv.iot.ui.splash

import android.arch.lifecycle.ViewModel
import ua.lviv.iot.model.firebase.FirebaseLoginManager

class SplashViewModel: ViewModel() {
    private lateinit var firebaseLogin: FirebaseLoginManager

    fun checkUserLogin() {
        firebaseLogin = FirebaseLoginManager()
        firebaseLogin.isUserLoggedIn()
    }
}
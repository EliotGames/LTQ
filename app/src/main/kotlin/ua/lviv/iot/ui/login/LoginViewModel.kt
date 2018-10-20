package ua.lviv.iot.ui.login

import android.arch.lifecycle.ViewModel
import android.content.Context
import ua.lviv.iot.`interface`.LoginNavigator
import java.lang.ref.WeakReference

class LoginViewModel: ViewModel() {
    private lateinit var loginNavigator: WeakReference<LoginNavigator>

    fun callFacebookLogin(c: Context) {

    }

    fun callGoogleLogin(c: Context) {
        (loginNavigator.get() as LoginNavigator).openMainActivity()
    }

}
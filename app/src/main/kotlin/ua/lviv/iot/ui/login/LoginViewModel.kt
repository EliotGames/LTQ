package ua.lviv.iot.ui.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.facebook.AccessToken
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import ua.lviv.iot.`interface`.LoginNavigator
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.firebase.User

class LoginViewModel: ViewModel() {
    private var firebaseLoginManager: FirebaseLoginManager = FirebaseLoginManager()
    var isLoginSuccessfull = FirebaseLoginManager.isLoginSuccessfull


    fun callFacebookLogin(result: LoginResult) {
        FirebaseLoginManager().firebaseAuthWithFacebook(result.accessToken, object: FirebaseLoginManager.UserLoginListener{
            override fun onSuccess() {
                FirebaseLoginManager().createUserData()
            }

            override fun onError(massage: String) {
                FirebaseLoginManager.isLoginSuccessfull.value = EventResultStatus.EVENT_FAILED
            }

        })
    }

    fun callGoogleLogin(navigator: LoginNavigator) {
        navigator.openLoginActivity()
    }

    fun authWithGoogle(googleAccount: GoogleSignInAccount) {
        firebaseLoginManager.firebaseAuthWithGoogle(googleAccount, object: FirebaseLoginManager.UserLoginListener {
            //user has registered: write personal data to firebase
            override fun onSuccess() {
                FirebaseLoginManager().createUserData()
                isLoginSuccessfull.value = FirebaseLoginManager.isLoginSuccessfull.value
            }
            //user cannot login on firebase
            override fun onError(massage: String) {
                FirebaseLoginManager.isLoginSuccessfull.value = EventResultStatus.EVENT_FAILED
                isLoginSuccessfull.value = FirebaseLoginManager.isLoginSuccessfull.value}
        })
    }

    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
}
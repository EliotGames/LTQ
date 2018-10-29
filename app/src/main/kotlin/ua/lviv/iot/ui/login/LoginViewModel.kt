package ua.lviv.iot.ui.login

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import ua.lviv.iot.`interface`.LoginNavigator
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.firebase.User

class LoginViewModel: ViewModel() {
    private var firebaseLoginManager: FirebaseLoginManager = FirebaseLoginManager()
    private var firebaseDataManager: FirebaseDataManager = FirebaseDataManager.getInstance()
    private lateinit var user: User
    var isLoginSuccessfull = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)


    fun callFacebookLogin() {

    }

    fun callGoogleLogin(navigator: LoginNavigator) {
        navigator.openLoginActivity()
    }

    fun authWithGoogle(googleAccount: GoogleSignInAccount) {
        firebaseLoginManager.firebaseAuthWithGoogle(googleAccount, object: FirebaseLoginManager.UserLoginListener {
            //user has registered: write personal data to firebase
            override fun onSuccess() {
                if(FirebaseLoginManager.isNewUser) {
                    user = User(firebaseLoginManager.currentUser!!.displayName!!,
                            firebaseLoginManager.currentUser!!.email!!)
                    firebaseDataManager.writeCurrentUserData(
                            firebaseLoginManager.currentUser!!.uid,
                            user,
                            object: FirebaseDataManager.UserWritingListener{
                                //personal data has written on firebase: change user status
                                override fun onSuccess() {
                                    onSuccessStatusChange()
                                }
                                //some mistake with wititng personal data: logout user
                                override fun onError() {
                                    val cUser = firebaseLoginManager.currentUser!!
                                    firebaseLoginManager.logout(object : FirebaseLoginManager.UserLoginListener{
                                        //user logout successfully
                                        override fun onSuccess() {
                                            firebaseLoginManager.deleteUser(cUser, object : FirebaseLoginManager.UserLoginListener{
                                                override fun onSuccess() { isLoginSuccessfull.value = EventResultStatus.EVENT_FAILED}
                                                override fun onError(massage: String) {isLoginSuccessfull.value = EventResultStatus.EVENT_FAILED}

                                            })
                                        }
                                        //user cannot logout
                                        override fun onError(massage: String) {isLoginSuccessfull.value = EventResultStatus.EVENT_FAILED}
                                    })
                                }

                            }
                    )
                }
                else {
                    onSuccessStatusChange()
                }
            }
            //user cannot login on firebase
            override fun onError(massage: String) {isLoginSuccessfull.value = EventResultStatus.EVENT_FAILED}
        })
    }

    private fun onSuccessStatusChange() {
        FirebaseLoginManager._isUserLoggedIn.value = true
        isLoginSuccessfull.value = EventResultStatus.EVENT_SUCCESS
    }

    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
}
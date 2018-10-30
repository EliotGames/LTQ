package ua.lviv.iot.model.firebase

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.provider.Settings.Global.getString
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import android.widget.Toast
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.*
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.ui.MainActivity
import ua.lviv.iot.ui.login.LoginActivity


class FirebaseLoginManager {
    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var user: User

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser


    fun logout(listener: UserLoginListener) {
        try {
            firebaseAuth.signOut()
            listener.onSuccess()
        } catch (e: Exception) {
            listener.onError(e.localizedMessage)
        }

    }

    fun deleteUser(user: FirebaseUser, listener: UserLoginListener) {
        user.delete()
                .addOnSuccessListener { listener.onSuccess() }
                .addOnFailureListener { e -> listener.onError(e.localizedMessage) }
    }

    fun createUserData() {
        if(FirebaseLoginManager.isNewUser) {
            user = User(currentUser!!.displayName!!,
                    currentUser!!.providerId)
            firebaseDataManager.writeCurrentUserData(
                    currentUser!!.uid, user, object: FirebaseDataManager.UserWritingListener{
                        //personal data has written on firebase: change user status
                        override fun onSuccess() {
                            onSuccessStatusChange()
                        }
                        //some mistake with wititng personal data: logout user
                        override fun onError() {
                            val cUser = currentUser!!
                            logout(object : FirebaseLoginManager.UserLoginListener{
                                //user logout successfully
                                override fun onSuccess() {
                                    deleteUser(cUser, object : FirebaseLoginManager.UserLoginListener{
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

    private fun onSuccessStatusChange() {
        FirebaseLoginManager.isLoginSuccessfull.value = EventResultStatus.EVENT_SUCCESS
        isLoginSuccessfull.value = EventResultStatus.EVENT_SUCCESS
    }

    fun firebaseAuthWithFacebook(token: AccessToken, listener: UserLoginListener) {
        Log.d("TAG", "firebaseAuthWithFacebook:" + token)
        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth!!.signInWithCredential(credential)
                .addOnSuccessListener {
                    setIsNewUser(it)
                    listener.onSuccess()
                }
                .addOnFailureListener {
                    listener.onError(it.localizedMessage)
                }
    }


    fun firebaseAuthWithGoogle(account: GoogleSignInAccount, listener: UserLoginListener) {

        Log.i("TAG", "Authenticating user with firebase.")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    setIsNewUser(authResult)
                    listener.onSuccess()
                }
                .addOnFailureListener { listener.onError("Cannot sing in with your Google account") }
    }

    fun isUserLoggedIn() {
        if(firebaseAuth.currentUser != null) {
            FirebaseLoginManager.isLoginSuccessfull.value = EventResultStatus.EVENT_SUCCESS
        }
    }

    interface UserLoginListener {
        fun onSuccess()

        fun onError(massage: String)
    }

    fun setIsNewUser(authResult: AuthResult) {
        if (authResult.additionalUserInfo.isNewUser) {
            FirebaseLoginManager.setIsNewUser(true)
        } else {
            FirebaseLoginManager.setIsNewUser(false)
        }
    }

    companion object {
        lateinit var auth: FirebaseAuth

        //value is user registered
        fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

        var isLoginSuccessfull = MutableLiveData<EventResultStatus>().default(EventResultStatus.NO_EVENT)

        var isNewUser = true

        fun setIsNewUser(isNewUser: Boolean) {
            FirebaseLoginManager.isNewUser = isNewUser
        }

    }


}
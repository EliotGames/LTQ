package ua.lviv.iot.model.firebase

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.provider.Settings.Global.getString
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.*
import ua.lviv.iot.R
import ua.lviv.iot.ui.MainActivity
import ua.lviv.iot.ui.login.LoginActivity


class FirebaseLoginManager {
    private val firebaseDataManager = FirebaseDataManager()
    private val firebaseAuth = FirebaseAuth.getInstance()

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


    fun firebaseAuthWithGoogle(account: GoogleSignInAccount, listener: UserLoginListener) {

        Log.i("TAG", "Authenticating user with firebase.")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    if (authResult.additionalUserInfo.isNewUser) {
                        FirebaseLoginManager.setIsNewUser(true)
                    } else {
                        FirebaseLoginManager.setIsNewUser(false)
                    }
                    listener.onSuccess()
                }
                .addOnFailureListener { listener.onError("Cannot sing in with your Google account") }
    }

    interface UserLoginListener {
        fun onSuccess()

        fun onError(massage: String)
    }

    companion object {
        lateinit var auth: FirebaseAuth

        //value is user registered
        fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

        var _isUserLoggedIn = MutableLiveData<Boolean>().default(false)

        var isNewUser = true

        fun setIsNewUser(isNewUser: Boolean) {
            FirebaseLoginManager.isNewUser = isNewUser
        }
    }

}
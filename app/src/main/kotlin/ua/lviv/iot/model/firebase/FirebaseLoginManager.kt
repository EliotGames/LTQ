package ua.lviv.iot.model.firebase

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.*
import ua.lviv.iot.ui.login.LoginActivity


class FirebaseLoginManager {
    private val firebaseDataManager = FirebaseDataManager()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    init {
        auth = FirebaseAuth.getInstance()

    }

    fun registerUser(email: String, password: String, listener: UserLoginListener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnFailureListener(OnFailureListener { e -> listener.onError(e.localizedMessage) })
                .addOnSuccessListener(OnSuccessListener<AuthResult> { authResult ->
                    val isNewUser = authResult.getAdditionalUserInfo().isNewUser()
                    LoginActivity.setIsNewUser(isNewUser)
                    listener.onSuccess()
                })
    }

    fun loginUser(email: String, password: String, listener: UserLoginListener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnFailureListener(OnFailureListener { e -> listener.onError(e.localizedMessage) })
                .addOnSuccessListener(OnSuccessListener<Any> { listener.onSuccess() })
    }

    fun logout(listener: UserLoginListener) {
        try {
            auth.signOut()
            listener.onSuccess()
        } catch (e: Exception) {
            listener.onError(e.localizedMessage)
        }

    }

    fun deleteUser(user: FirebaseUser, listener: UserLoginListener) {
        user.delete()
                .addOnSuccessListener(OnSuccessListener<Void> { listener.onSuccess() })
                .addOnFailureListener(OnFailureListener { e -> listener.onError(e.localizedMessage) })
    }


    fun firebaseAuthWithGoogle(credential: AuthCredential, listener: UserLoginListener) {

        auth.signInWithCredential(credential)
                .addOnSuccessListener(OnSuccessListener<Any> { authResult ->
                    if (authResult.getAdditionalUserInfo().isNewUser()) {
                        LoginActivity.setIsNewUser(true)
                    } else {
                        LoginActivity.setIsNewUser(false)
                    }
                    listener.onSuccess()
                })
                .addOnFailureListener(OnFailureListener { listener.onError("Cannot sing in with your Google account") })
    }

    interface UserLoginListener {
        fun onSuccess()

        fun onError(massage: String)
    }

    companion object {
        lateinit var auth: FirebaseAuth
    }
}
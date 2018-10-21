package ua.lviv.iot.ui.login

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.internal.AccountType
import com.google.firebase.auth.*
import ua.lviv.iot.R
import ua.lviv.iot.`interface`.LoginNavigator
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.firebase.LoginType
import ua.lviv.iot.model.firebase.User
import ua.lviv.iot.ui.MainActivity


class LoginActivity : AppCompatActivity(), LoginNavigator {
    private val REQUEST_CODE_GOOGLE_LOGIN = 121

    lateinit var loginViewModel: LoginViewModel

    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var googleSignInOption: GoogleSignInOptions
    lateinit var googleSingInAccount: GoogleSignInAccount

    lateinit var firebaseLoginManager: FirebaseLoginManager
    lateinit var firebaseDataManager: FirebaseDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loginViewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)


        val buttonFacebook = findViewById<ImageButton>(R.id.btn_login_facebook)
        val buttonGoogle = findViewById<ImageButton>(R.id.btn_login_google)

        buttonFacebook.setOnClickListener(View.OnClickListener {
            loginViewModel.callFacebookLogin(this)
        })
        buttonGoogle.setOnClickListener(View.OnClickListener {
            loginViewModel.callGoogleLogin(this)
        })
    }

    override fun openMainActivity() {
        googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(this.resources.getString(R.string.google_sign_in_token))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOption)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_LOGIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        googleSingInAccount = GoogleSignIn.getLastSignedInAccount(this)!!
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == REQUEST_CODE_GOOGLE_LOGIN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                googleSingInAccount = task.getResult(ApiException::class.java)!!
                val userLoginListener = object : FirebaseLoginManager.UserLoginListener {
                    override fun onSuccess() {
                        if (FirebaseLoginManager.isNewUser) {
                            firebaseDataManager.writeCurrentUserData(firebaseLoginManager.currentUser?.uid!!,
                                    User(LoginType.GOOGLE, googleSingInAccount.displayName!!, googleSingInAccount.email!!),
                                   object : FirebaseDataManager.UserWritingListener {
                                override fun onSuccess() {
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                }

                                override fun onError() {
                                    Toast.makeText(this@LoginActivity, "Something wrong with your sign in, please try again", Toast.LENGTH_SHORT).show()
                                    val user = firebaseLoginManager.currentUser!!
                                    firebaseLoginManager.logout(object : FirebaseLoginManager.UserLoginListener {
                                        override fun onSuccess() {

                                        }
                                        override fun  onError(msg: String) {

                                        }
                                    })
                                    firebaseLoginManager.deleteUser(user, object : FirebaseLoginManager.UserLoginListener {
                                        override fun onSuccess() {

                                        }

                                        override fun onError(msg: String) {
                                            Log.e("GogRegUsDelFail:", msg)
                                        }
                                    })
                                }
                            })
                        } else startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    }

                    override fun onError(massage: String) {
                        if (isNetworkAvailable()) {
                            Toast.makeText(this@LoginActivity, "Cannot sign in with google, some problems found", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@LoginActivity, "Don't have Internet connection", Toast.LENGTH_SHORT).show()
                        }
                        Log.e("User Google sign: ", massage)
                    }
                }

                val credential = GoogleAuthProvider.getCredential(googleSingInAccount.idToken, null)
                firebaseLoginManager.firebaseAuthWithGoogle(credential, userLoginListener)
            } catch (e: ApiException) {
                Log.e("Error", e.localizedMessage)
            }

        } else {
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Don't have Internet connection", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("Error", "Cannot sing in with your Google account")
            }
        }

    }


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}
package ua.lviv.iot.ui.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import ua.lviv.iot.R
import ua.lviv.iot.`interface`.LoginNavigator
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.ui.MainActivity
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.facebook.login.LoginManager
import com.facebook.CallbackManager
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger


class LoginActivity : AppCompatActivity() {
    //code for signUp using google or facebook
    private val REQUEST_CODE_GOOGLE_LOGIN = 121
    private val REQUEST_CODE_FACEBOOK_LOGIN = 212

    //viewmodel
    lateinit var loginViewModel: LoginViewModel

    //objects for google registration
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var googleSignInOption: GoogleSignInOptions
    lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //init and setOnClickListener for skip_login button
        val skipLogin = findViewById<Button>(R.id.skip_login)
        skipLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))}

        //viewmodel init
        loginViewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)

        //observe if user login is successful
        loginViewModel.isLoginSuccessfull.observe(this, Observer {
            when(it) {
                EventResultStatus.EVENT_SUCCESS -> {
                    startActivity(Intent(this, MainActivity::class.java))}
                EventResultStatus.EVENT_FAILED -> {
                    Toast.makeText(this, "Registration failed!", Toast.LENGTH_SHORT).show()}
                else -> {}
            }
        })

        //ui elements init
        val buttonFacebook = findViewById<ImageButton>(R.id.btn_login_facebook)
        val buttonGoogle = findViewById<ImageButton>(R.id.btn_login_google)
        val hideFbBtn = findViewById<com.facebook.login.widget.LoginButton>(R.id.hided_fb_button)

        //ui buttons clicklistener
        //facebook onClickListener
        buttonFacebook.setOnClickListener {
            if (isNetworkAvailable()) {
                hideFbBtn.performClick()
            } else {
                Toast.makeText(this, R.string.network_failed, Toast.LENGTH_SHORT).show()
            }
        }

        //Facebook init
        FacebookSdk.sdkInitialize(getApplicationContext())
        AppEventsLogger.activateApp(this@LoginActivity)
        callbackManager = CallbackManager.Factory.create()
        hideFbBtn.setReadPermissions("email", "public_profile", "user_friends");
        LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        loginViewModel.callFacebookLogin(loginResult)
                    }

                    override fun onCancel() {
                        Log.e("Facebook", "Auth cancelled!")
                        // App code
                    }

                    override fun onError(exception: FacebookException) {
                        Log.e("Facebook", "Auth error!")
                        // App code
                    }
                })

        //google onClickListener
        buttonGoogle.setOnClickListener {
            if (isNetworkAvailable()) {
                loginViewModel.callGoogleLogin(object : LoginNavigator {
                    override fun openLoginActivity() {
                        googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(this@LoginActivity.resources.getString(R.string.google_sign_in_token))
                                .requestEmail()
                                .build()

                        googleSignInClient = GoogleSignIn.getClient(this@LoginActivity, googleSignInOption)
                        val signInIntent = googleSignInClient.signInIntent

                        startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_LOGIN)
                    }
                })
            } else {
                Toast.makeText(this, R.string.network_failed, Toast.LENGTH_SHORT).show()
            }
        }

        //LiveData observe fun
        fun <T> LiveData<T>.observe(observe: (T?) -> Unit) = observe(this@LoginActivity, Observer {
            observe(it)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)

        Log.i("Tag", "Got Result code $requestCode.")

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == REQUEST_CODE_GOOGLE_LOGIN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            Log.i("TAG", "With Google LogIn, is result a success? ${result.isSuccess}.")
            if (result.isSuccess) {
                // Google Sign In was successful, authenticate with Firebase
                loginViewModel.authWithGoogle(result.signInAccount!!)
            } else {
                Toast.makeText(this@LoginActivity, "Some error occurred.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    //check network connection
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}
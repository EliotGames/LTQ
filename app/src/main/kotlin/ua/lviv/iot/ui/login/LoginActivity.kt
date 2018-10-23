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
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import ua.lviv.iot.R
import ua.lviv.iot.`interface`.LoginNavigator
import ua.lviv.iot.ui.profile.ProfileActivity


class LoginActivity : AppCompatActivity() {
    //code for signUp using google or facebook
    private val REQUEST_CODE_GOOGLE_LOGIN = 121
    private val REQUEST_CODE_FACEBOOK_LOGIN = 212

    //viewmodel
    lateinit var loginViewModel: LoginViewModel

    //objects for google registration
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var googleSignInOption: GoogleSignInOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //viewmodel init
        loginViewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)

        loginViewModel.isLoginSuccessfull.observe(this, Observer{
            if(it!!) {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            else {Toast.makeText(this, "Registration failed!", Toast.LENGTH_SHORT).show()}
        })

        //ui elements init
        val buttonFacebook = findViewById<ImageButton>(R.id.btn_login_facebook)
        val buttonGoogle = findViewById<ImageButton>(R.id.btn_login_google)

        //ui buttons clicklistener
        //facebook onClickListener
        buttonFacebook.setOnClickListener {
            if(isNetworkAvailable()) {
                loginViewModel.callFacebookLogin()
            }
            else {Toast.makeText(this, R.string.network_failed, Toast.LENGTH_SHORT).show()}
        }
        //google onClickListener
        buttonGoogle.setOnClickListener {
            if(isNetworkAvailable()) {
                loginViewModel.callGoogleLogin(object : LoginNavigator{
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
            }
            else {Toast.makeText(this, R.string.network_failed, Toast.LENGTH_SHORT).show()}
        }

        //LiveData observe fun
        fun <T> LiveData<T>.observe(observe:(T?)->Unit) = observe(this@LoginActivity, Observer {
            observe (it)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

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
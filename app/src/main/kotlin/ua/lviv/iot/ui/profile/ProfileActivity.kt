package ua.lviv.iot.ui.profile

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_profile.*
import ua.lviv.iot.R
import ua.lviv.iot.ui.login.LoginActivity

class ProfileActivity : AppCompatActivity() {

    lateinit var profileViewModel :ProfileViewModel

    var isUserRegistered : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //actionBar.title = "User profile"
        //linearlayout_profile_main.requestFocus()

        //create viewmodel obj
        profileViewModel = ViewModelProviders.of(this).get(ProfileViewModel::class.java)

        //init livedata
        profileViewModel.isUserRegistered.observe(this , Observer {
            if (it!!) {
                setContentView(R.layout.activity_profile)
            }
            else { startActivity(Intent(this, LoginActivity::class.java))}
        })

        //livedata func
        fun <T> LiveData<T>.observe(observe:(T?)->Unit) = observe(this@ProfileActivity, Observer {
            observe (it)
        })

    }
}
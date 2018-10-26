package ua.lviv.iot.ui.profile

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.partial_profile_header.*
import ua.lviv.iot.R
import ua.lviv.iot.model.firebase.User
import ua.lviv.iot.ui.login.LoginActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileViewModel :ProfileViewModel
    private lateinit var userEmail: EditText
    private lateinit var userFacebook: EditText
    private lateinit var userName: TextView
    private lateinit var userSex: TextView
    private lateinit var userPoints: TextView


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
        profileViewModel.getCurrentUser()
        profileViewModel.currentUserData.observe(this@ProfileActivity, Observer {
            user_email.setText(it!!.email, TextView.BufferType.EDITABLE)
            user_sex.text = it.sex.toString()
            user_name.text = it.name

        })



        
        //livedata func
        fun <T> LiveData<T>.observe(observe:(T?)->Unit) = observe(this@ProfileActivity, Observer {
            observe (it)
        })

    }
}


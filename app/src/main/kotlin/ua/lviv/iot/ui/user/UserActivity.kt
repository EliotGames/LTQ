package ua.lviv.iot.ui.user

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.partial_profile_header.*
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.firebase.User
import ua.lviv.iot.ui.balance.BalanceFragment
import ua.lviv.iot.ui.login.LoginActivity
import ua.lviv.iot.ui.profile.ProfileFragment
import ua.lviv.iot.ui.profile.ProfileViewModel
import ua.lviv.iot.ui.rating.RatingFragment

class UserActivity : AppCompatActivity() {
    private lateinit var profileViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init LIVE DATA
        profileViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)

        profileViewModel.isUserRegistered.observe(this, Observer { parentIt ->
            if (parentIt == EventResultStatus.EVENT_SUCCESS) {
                setContentView(R.layout.activity_user)
                initToolbar()
                init()
                //get current user data
                profileViewModel.getCurrentUser()
                profileViewModel.currentUserData.observe(this@UserActivity, Observer {
                    user_email.setText(it!!.email, TextView.BufferType.EDITABLE)
                    user_sex.text = it.sex.toString()
                    user_name.text = it.name
                })

                //init and setOnClickListener on Logout button
                val logout = findViewById<Button>(R.id.logout_button)
                logout.setOnClickListener { profileViewModel.userLogout() }

                profileViewModel.isUserLogout.observe(this, Observer {
                    when (it) {
                        EventResultStatus.EVENT_SUCCESS -> {
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                        EventResultStatus.EVENT_FAILED -> {
                            Toast.makeText(this, "Something went wrong! Please, try again!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                        }
                    }
                })
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        })

        fun <T> LiveData<T>.observe(observe: (T?) -> Unit) = observe(this@UserActivity, Observer {
            observe(it)
        })
    }

    private fun init() {
        // choosing the fragment to display
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val extras = intent.extras
        val fragment: Fragment

        if (extras != null) {
            when (extras.getString("fragment")) {
                "profile" -> {
                    fragment = ProfileFragment()
                    fragmentTransaction.replace(R.id.fl_user_fragment_container, fragment)
                }
                "rating" -> {
                    fragment = RatingFragment()
                    fragmentTransaction.replace(R.id.fl_user_fragment_container, fragment)
                }
                "balance" -> {
                    fragment = BalanceFragment()
                    fragmentTransaction.replace(R.id.fl_user_fragment_container, fragment)
                }
                else -> {
                    Log.e("User Activity", "No fragment is mentioned in intent")
                }
            }
        }
        fragmentTransaction.commit()
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.user_toolbar)
        toolbar.title = "User page"
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }
}

package ua.lviv.iot.ui.profile

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.ui.login.LoginActivity
import ua.lviv.iot.ui.user.UserViewModel

class ProfileFragment : Fragment() {
    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        userViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)


        userViewModel.isUserRegistered.observe(this, Observer { parentIt ->
            if (parentIt == EventResultStatus.EVENT_SUCCESS) {
                //get current user data
                userViewModel.getCurrentUser()
                userViewModel.currentUserData.observe(this@ProfileFragment, Observer {
                    val userEmailET = view.findViewById<EditText>(R.id.user_email)
                    userEmailET.setText(it!!.email, TextView.BufferType.EDITABLE)
                })

                //init and setOnClickListener on Logout button
                val logout = view.findViewById<Button>(R.id.logout_button)
                logout.setOnClickListener { userViewModel.userLogout() }

                userViewModel.isUserLogout.observe(this, Observer {
                    when (it) {
                        EventResultStatus.EVENT_SUCCESS -> {
                            startActivity(Intent(activity, LoginActivity::class.java))
                        }
                        EventResultStatus.EVENT_FAILED -> {
                            Toast.makeText(activity, "Something went wrong! Please, try again!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                        }
                    }
                })
            } else {
                startActivity(Intent(activity, LoginActivity::class.java))
            }
        })

        fun <T> LiveData<T>.observe(observe: (T?) -> Unit) = observe(this@ProfileFragment, Observer {
            observe(it)
        })

        return view
    }
}
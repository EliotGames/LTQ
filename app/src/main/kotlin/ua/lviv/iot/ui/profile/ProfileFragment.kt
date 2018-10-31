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
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.partial_profile_header.*
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.ui.login.LoginActivity
import ua.lviv.iot.ui.user.UserActivity

class ProfileFragment : Fragment() {
    private lateinit var profileViewModel :ProfileViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        //create viewmodel obj
        profileViewModel = ViewModelProviders.of(this).get(ProfileViewModel::class.java)

        //init livedata
        profileViewModel.isUserRegistered.observe(this , Observer { parentIt ->
            if (parentIt == EventResultStatus.EVENT_SUCCESS) {
                //get current user data
                profileViewModel.getCurrentUser()
                profileViewModel.currentUserData.observe(this@ProfileFragment, Observer {
                    user_email.setText(it!!.email, TextView.BufferType.EDITABLE)
                    user_sex.text = it.sex.toString()
                    user_name.text = it.name })

                //init and setOnClickListener on Logout button
                val logout = view.findViewById<Button>(R.id.logout_button)
                logout.setOnClickListener { profileViewModel.userLogout() }

                profileViewModel.isUserLogout.observe(this, Observer {
                    when(it) {
                        EventResultStatus.EVENT_SUCCESS -> {
                            startActivity(Intent(activity, LoginActivity::class.java))}
                        EventResultStatus.EVENT_FAILED -> {
                            Toast.makeText(activity, "Something went wrong! Please, try again!", Toast.LENGTH_SHORT).show()}
                        else -> {}
                    }
                })
            }
            else { startActivity(Intent(activity, LoginActivity::class.java))}
        })

        //livedata func
        fun <T> LiveData<T>.observe(observe:(T?)->Unit) = observe(this@ProfileFragment, Observer {
            observe (it)
        })


        return view
    }
}
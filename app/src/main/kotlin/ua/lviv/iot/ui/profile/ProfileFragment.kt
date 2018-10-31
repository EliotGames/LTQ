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


        return view
    }
}
package ua.lviv.iot.ui.user

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import ua.lviv.iot.R
import ua.lviv.iot.ui.balance.BalanceFragment
import ua.lviv.iot.ui.profile.ProfileFragment
import ua.lviv.iot.ui.rating.RatingFragment

class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val toolbar = findViewById<Toolbar>(R.id.user_toolbar)
        toolbar.title = "User page"
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

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
}

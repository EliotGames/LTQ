package ua.lviv.iot.ui.user

import android.animation.LayoutTransition
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState
import ua.lviv.iot.R
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.ui.balance.BalanceFragment
import ua.lviv.iot.ui.login.LoginActivity
import ua.lviv.iot.ui.profile.ProfileFragment
import ua.lviv.iot.ui.rating.RatingFragment
import ua.lviv.iot.utils.USER_FRAGMENT_BALANCE
import ua.lviv.iot.utils.USER_FRAGMENT_PROFILE
import ua.lviv.iot.utils.USER_FRAGMENT_RATING

class UserActivity : AppCompatActivity(), ObservableScrollViewCallbacks {
    private val TAG = "User Activity"
    private lateinit var profileViewModel: UserViewModel

    private lateinit var observableScrollView: ObservableScrollView
    private lateinit var imageListView: View
    private lateinit var containerLayout: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init LIVE DATA
        profileViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)

        profileViewModel.isUserRegistered.observe(this, Observer { parentIt ->
            if (parentIt == EventResultStatus.EVENT_SUCCESS) {
                setContentView(R.layout.activity_user)
                initToolbar()
                initFragment()

                containerLayout = findViewById(R.id.rl_user_container)
                observableScrollView = findViewById(R.id.observablescrollview_user)
                imageListView = findViewById(R.id.horizontalscrollview_user)
                containerLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                observableScrollView.setScrollViewCallbacks(this)

                //get current user data
                profileViewModel.getCurrentUser()
                profileViewModel.currentUserData.observe(this@UserActivity, Observer {
                    val userNameTV = findViewById<TextView>(R.id.tv_user_name)

                    userNameTV.text = it!!.name
                })

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
                //startActivity(Intent(this, LoginActivity::class.java))
                setContentView(R.layout.activity_user)
                initToolbar()
                initFragment()
            }
        })

        fun <T> LiveData<T>.observe(observe: (T?) -> Unit) = observe(this@UserActivity, Observer {
            observe(it)
        })
    }

    private fun initFragment() {
        // choosing the fragment to display
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val extras = intent.extras
        val fragment: Fragment

        if (extras != null) {
            when (extras.getString("fragment")) {
                USER_FRAGMENT_PROFILE -> {
                    fragment = ProfileFragment()
                    fragmentTransaction.replace(R.id.fl_user_fragment_container, fragment)
                }
                USER_FRAGMENT_RATING -> {
                    fragment = RatingFragment()
                    fragmentTransaction.replace(R.id.fl_user_fragment_container, fragment)
                }
                USER_FRAGMENT_BALANCE -> {
                    fragment = BalanceFragment()
                    fragmentTransaction.replace(R.id.fl_user_fragment_container, fragment)
                }
                else -> {
                    Log.e(TAG, "No fragment is mentioned in intent")
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

    override fun onUpOrCancelMotionEvent(scrollState: ScrollState?) {
        if (scrollState == ScrollState.DOWN) {
            Log.d("SCROLL STATE", "UP")
            imageListView.animate().translationY(0f)
        } else if (scrollState == ScrollState.UP) {
            Log.d("SCROLL STATE", "DOWN")
            imageListView.animate()
                    .translationY(-imageListView.height.toFloat())
                    .duration = 300
        }
    }

    override fun onScrollChanged(scrollY: Int, firstScroll: Boolean, dragging: Boolean) {

    }

    override fun onDownMotionEvent() {
    }
}

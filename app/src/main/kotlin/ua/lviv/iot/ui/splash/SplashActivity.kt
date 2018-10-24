package ua.lviv.iot.ui.splash

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ua.lviv.iot.ui.MainActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var splashViewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))

        splashViewModel = ViewModelProviders.of(this).get(SplashViewModel::class.java)

        splashViewModel.checkUserLogin()

        //LiveData observe fun
        fun <T> LiveData<T>.observe(observe:(T?)->Unit) = observe(this@SplashActivity, Observer {
            observe (it)
        })
    }


}
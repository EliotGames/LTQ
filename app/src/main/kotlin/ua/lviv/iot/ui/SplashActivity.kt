package ua.lviv.iot.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ua.lviv.iot.ui.login.LoginActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
    }
}
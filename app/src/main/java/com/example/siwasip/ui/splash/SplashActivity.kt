package com.example.siwasip.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.siwasip.data.local.Prefs
import com.example.siwasip.ui.auth.LoginActivity
import com.example.siwasip.ui.main.MainActivity
import com.example.siwasip.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Prefs.init(applicationContext)

        lifecycleScope.launch {
            delay(800L)
            val token = Prefs.authToken
            if (token.isNullOrBlank()) {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
            finish()
        }
    }
}
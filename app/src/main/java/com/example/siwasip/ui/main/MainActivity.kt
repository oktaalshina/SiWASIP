package com.example.siwasip.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.siwasip.R
import com.example.siwasip.ui.main.home.HomeFragment
import com.example.siwasip.ui.main.profile.ProfileFragment
import com.example.siwasip.ui.main.upload.UploadFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    openHome()
                    true
                }
                R.id.nav_upload -> {
                    openUpload()
                    true
                }
                R.id.nav_profile -> {
                    openProfile()
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun openHome() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerMain, HomeFragment())
            .commit()
    }

    private fun openUpload() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerMain, UploadFragment())
            .commit()
    }

    private fun openProfile() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerMain, ProfileFragment())
            .commit()
    }
}
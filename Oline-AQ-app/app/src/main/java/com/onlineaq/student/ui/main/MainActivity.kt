package com.onlineaq.student.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.onlineaq.student.BuildConfig
import com.onlineaq.student.OnlineAQApp
import com.onlineaq.student.R
import com.onlineaq.student.ui.examlist.ExamListFragment
import com.onlineaq.student.ui.practice.PracticeFragment
import com.onlineaq.student.ui.profile.ProfileFragment
import com.onlineaq.student.ui.results.ResultsFragment
import com.onlineaq.student.ui.wrongbook.WrongBookFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        if (savedInstanceState == null) {
            switchFragment(ExamListFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_results -> ResultsFragment()
                R.id.nav_practice -> PracticeFragment()
                R.id.nav_wrong_book -> WrongBookFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> ExamListFragment()
            }
            switchFragment(fragment)
            true
        }

        checkVersionUpdate()
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun checkVersionUpdate() {
        val versionInfo = OnlineAQApp.latestVersion ?: return
        if (versionInfo.versionCode <= BuildConfig.VERSION_CODE) return

        val dialog = AlertDialog.Builder(this)
            .setTitle("发现新版本 v${versionInfo.versionName}")
            .setMessage(versionInfo.releaseNotes.ifBlank { "有新版本可用，请更新后使用。" })
            .setCancelable(false)

        if (versionInfo.downloadUrl.isNotBlank()) {
            dialog.setPositiveButton("立即更新") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.downloadUrl))
                startActivity(intent)
            }
        }

        dialog.setNegativeButton("稍后再说", null)

        dialog.show()
    }
}

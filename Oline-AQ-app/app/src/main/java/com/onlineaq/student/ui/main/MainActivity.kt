package com.onlineaq.student.ui.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

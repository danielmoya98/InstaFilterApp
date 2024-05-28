package com.example.instafilterapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager

class ViewPagerMain : AppCompatActivity() {

    private lateinit var viewPagerMain: ViewPager
    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var dotsLayout: LinearLayout
    private lateinit var dots: Array<TextView?>
    private lateinit var layouts: IntArray
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_pager)

        startButton = findViewById(R.id.start_button)
        viewPagerMain = findViewById(R.id.view_pager)
        dotsLayout = findViewById(R.id.dots_layout)

        // Array de layouts para las pantallas del onboarding
        layouts = intArrayOf(
            R.layout.screen1,
            R.layout.screen2,
            R.layout.screen3
        )

        onboardingAdapter = OnboardingAdapter(this, layouts)
        viewPagerMain.adapter = onboardingAdapter

        addDotsIndicator(0)
        viewPagerMain.addOnPageChangeListener(viewPagerPageChangeListener)

        startButton.setOnClickListener {
            openNextActivity()
        }
    }

    private fun addDotsIndicator(position: Int) {
        dots = arrayOfNulls(layouts.size)
        dotsLayout.removeAllViews()

        for (i in dots.indices) {
            dots[i] = TextView(this).apply {
                text = "•"
                textSize = 35f
                setTextColor(resources.getColor(R.color.black))
            }
            dotsLayout.addView(dots[i])
        }

        if (dots.isNotEmpty()) {
            dots[position]?.setTextColor(resources.getColor(R.color.green))
        }
    }

    private val viewPagerPageChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        override fun onPageSelected(position: Int) {
            addDotsIndicator(position)
        }
        override fun onPageScrollStateChanged(state: Int) {}
    }

    private fun openNextActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}

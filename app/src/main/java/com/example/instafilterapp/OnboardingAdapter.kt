package com.example.instafilterapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

class OnboardingAdapter(private val context: Context, private val layouts: IntArray) : PagerAdapter() {

    override fun getCount(): Int {
        return layouts.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layouts[position], container, false)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        container.removeView(view)
    }
}

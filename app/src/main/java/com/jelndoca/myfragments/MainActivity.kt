package com.jelndoca.myfragments

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var tabAdapter: TabsAdapter
        tabAdapter = TabsAdapter(supportFragmentManager)
        tabAdapter.addFragments(Fragment1(), "Tab 1")
        tabAdapter.addFragments(Fragment2(), "Tab 2")
        tabAdapter.addFragments(Fragment3(), "Tab 3")

        viewPager.adapter = tabAdapter

        tabLayout.setupWithViewPager(viewPager)

    }
}

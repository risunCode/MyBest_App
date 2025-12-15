package com.risuncode.mybest.ui.tugas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.risuncode.mybest.R
import com.risuncode.mybest.databinding.ActivityTugasBinding

class TugasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTugasBinding

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_SUBJECT_NAME = "subject_name"
        const val EXTRA_TUGAS_LINK = "tugas_link"

        fun start(context: Context, scheduleId: Int, subjectName: String, tugasLink: String = "") {
            val intent = Intent(context, TugasActivity::class.java).apply {
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(EXTRA_SUBJECT_NAME, subjectName)
                putExtra(EXTRA_TUGAS_LINK, tugasLink)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTugasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: ""
        val tugasLink = intent.getStringExtra(EXTRA_TUGAS_LINK) ?: ""

        setupAppBar(subjectName)
        setupTabs(tugasLink)
    }

    private fun setupAppBar(subjectName: String) {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvSubjectName.text = subjectName
    }

    private fun setupTabs(tugasLink: String) {
        val tabTitles = listOf(
            getString(R.string.data_penugasan),
            getString(R.string.data_nilai_tugas)
        )

        binding.viewPager.adapter = TugasPagerAdapter(this, tabTitles.size, tugasLink)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private inner class TugasPagerAdapter(
        activity: AppCompatActivity,
        private val itemCount: Int,
        private val tugasLink: String
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = itemCount

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TugasListFragment.newInstance(tugasLink)
                else -> TugasNilaiFragment.newInstance(tugasLink)
            }
        }
    }
}

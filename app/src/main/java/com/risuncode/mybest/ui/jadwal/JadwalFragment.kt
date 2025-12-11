package com.risuncode.mybest.ui.jadwal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.entity.ScheduleEntity
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.databinding.FragmentJadwalBinding
import com.risuncode.mybest.ui.presensi.PresensiActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class JadwalFragment : Fragment() {

    private var _binding: FragmentJadwalBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var repository: AppRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJadwalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        repository = AppRepository(database)
        
        setupSwipeRefresh()
        setupListeners()
        loadScheduleData()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Try to sync from server
            val result = repository.syncScheduleFromServer()
            
            result.onSuccess {
                loadScheduleData()
            }.onFailure { error ->
                // Show error but still load local data
                android.widget.Toast.makeText(
                    requireContext(),
                    error.message ?: "Gagal sinkronisasi",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                loadScheduleData()
            }
            
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupListeners() {
        binding.cardUpcoming.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val allSchedules = repository.getAllSchedules().firstOrNull() ?: emptyList()
                val upcomingSchedule = allSchedules.firstOrNull()
                upcomingSchedule?.let { schedule ->
                    PresensiActivity.start(requireContext(), schedule.id)
                }
            }
        }
    }
    
    private fun loadScheduleData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allSchedules = repository.getAllSchedules().firstOrNull() ?: emptyList()
            val totalSks = repository.getTotalSks()
            
            // Update stats
            binding.tvTotalMatkul.text = allSchedules.size.toString()
            binding.tvTotalSks.text = totalSks.toString()
            
            // Update upcoming class
            val upcomingSchedule = allSchedules.firstOrNull()
            upcomingSchedule?.let { schedule ->
                binding.tvUpcomingName.text = schedule.subjectName
                binding.tvUpcomingTime.text = "${schedule.day}, ${schedule.startTime} - ${schedule.endTime}"
                binding.tvCountdown.text = calculateCountdown(schedule)
            }
            
            // Populate schedule list
            populateScheduleList(allSchedules)
            
            // Show/hide empty state
            if (allSchedules.isEmpty()) {
                binding.cardNoSchedule.visibility = View.VISIBLE
            } else {
                binding.cardNoSchedule.visibility = View.GONE
            }
        }
    }
    
    private fun calculateCountdown(schedule: ScheduleEntity): String {
        val now = Calendar.getInstance()
        val dayIndex = when (schedule.day) {
            "Senin" -> Calendar.MONDAY
            "Selasa" -> Calendar.TUESDAY
            "Rabu" -> Calendar.WEDNESDAY
            "Kamis" -> Calendar.THURSDAY
            "Jumat" -> Calendar.FRIDAY
            "Sabtu" -> Calendar.SATURDAY
            "Minggu" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
        
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        var daysUntil = dayIndex - currentDay
        if (daysUntil < 0) daysUntil += 7
        
        // Parse schedule start time
        val timeParts = schedule.startTime.split(":")
        val scheduleHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val scheduleMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        
        // Calculate total minutes until class
        var totalMinutes = (daysUntil * 24 * 60) + 
                          ((scheduleHour - currentHour) * 60) + 
                          (scheduleMinute - currentMinute)
        
        if (totalMinutes < 0) {
            // Already passed this week, calculate for next week
            totalMinutes += 7 * 24 * 60
        }
        
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        
        return when {
            days > 0 && hours > 0 -> "$days hari $hours jam"
            days > 0 -> "$days hari"
            hours > 0 && minutes > 0 -> "$hours jam $minutes menit"
            hours > 0 -> "$hours jam"
            minutes > 0 -> "$minutes menit"
            else -> "Sekarang"
        }
    }
    
    private fun populateScheduleList(schedules: List<ScheduleEntity>) {
        val container = binding.scheduleListContainer
        container.removeAllViews()
        
        schedules.forEachIndexed { index, schedule ->
            val colorRes = getScheduleColorRes(schedule)
            val cardView = createScheduleCard(schedule, colorRes)
            container.addView(cardView)
            
            // Add spacing between cards
            if (index < schedules.size - 1) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.spacing_md)
                    )
                }
                container.addView(spacer)
            }
        }
    }
    
    /**
     * Determine schedule card color based on day and time:
     * - Red: Future day (belum mulai)
     * - Green: Today and will start soon
     * - Gray: Today but time has passed
     */
    private fun getScheduleColorRes(schedule: ScheduleEntity): Int {
        val today = getCurrentDayInIndonesian()
        
        if (schedule.day != today) {
            return R.color.schedule_future
        }
        
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        
        val endTimeParts = schedule.endTime.split(":")
        if (endTimeParts.size == 2) {
            val endHour = endTimeParts[0].toIntOrNull() ?: 0
            val endMinute = endTimeParts[1].toIntOrNull() ?: 0
            
            val currentTotalMinutes = currentHour * 60 + currentMinute
            val endTotalMinutes = endHour * 60 + endMinute
            
            return if (currentTotalMinutes >= endTotalMinutes) {
                R.color.schedule_passed
            } else {
                R.color.schedule_active
            }
        }
        
        return R.color.schedule_active
    }
    
    private fun createScheduleCard(schedule: ScheduleEntity, colorRes: Int): CardView {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.item_schedule_card, binding.scheduleListContainer, false) as CardView
        
        cardView.findViewById<View>(R.id.headerLayout)?.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
        cardView.findViewById<TextView>(R.id.tvSubjectName)?.text = schedule.subjectName
        cardView.findViewById<TextView>(R.id.tvScheduleTime)?.text = "${schedule.day} - ${schedule.startTime}-${schedule.endTime}"
        cardView.findViewById<TextView>(R.id.tvDosen)?.text = "Dosen: ${schedule.dosen}"
        cardView.findViewById<TextView>(R.id.tvKode)?.text = "Kode: ${schedule.subjectCode}"
        cardView.findViewById<TextView>(R.id.tvSks)?.text = "SKS: ${schedule.sks}"
        cardView.findViewById<TextView>(R.id.tvRoom)?.text = "Ruang: ${schedule.room}"
        cardView.findViewById<TextView>(R.id.tvKelPraktek)?.text = schedule.kelompokPraktek
        cardView.findViewById<TextView>(R.id.tvKodeGabung)?.text = schedule.kodeGabung
        
        cardView.setOnClickListener {
            com.risuncode.mybest.ui.presensi.PresensiActivity.start(requireContext(), schedule.id)
        }
        
        return cardView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun getCurrentDayInIndonesian(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> "Senin"
        }
    }
}


package diss.testing.runningapp2.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import diss.testing.runningapp2.R
import diss.testing.runningapp2.databinding.FragmentSettingsBinding
import diss.testing.runningapp2.databinding.FragmentSetupBinding
import diss.testing.runningapp2.databinding.FragmentStatisticsBinding
import diss.testing.runningapp2.other.TrackingUtility
import diss.testing.runningapp2.ui.viewmodels.MainViewModel
import diss.testing.runningapp2.ui.viewmodels.StatisticsViewModel
import kotlin.math.round

@AndroidEntryPoint
class StatisticsFragment: Fragment(R.layout.fragment_statistics) {

    private val viewModel: StatisticsViewModel by viewModels()
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        val view = binding.root
        subscribeToObservers()
        setupBarChart()
        return view
    }

    private fun setupBarChart() {
        binding.lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawLabels(false)
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            setDrawGridLines(false)
            setDrawLabels(false)
        }
        binding.lineChart.axisLeft.apply {
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            setDrawGridLines(false)
            setDrawLabels(false)
        }
        binding.lineChart.axisRight.apply {
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            setDrawGridLines(false)
            setDrawLabels(false)
        }
        binding.lineChart.apply {
            description.text = ""
        }
    }


    private fun subscribeToObservers() {
        viewModel.totalTimeSailed.observe(viewLifecycleOwner, Observer {
            it?.let{
                val totalTimeSailed = TrackingUtility.getFormattedTime(it)
                binding.tvTotalTime.text = totalTimeSailed
            }
        })
        viewModel.totalDistance.observe(viewLifecycleOwner, Observer {
            it?.let {
                val km = it/1000f
                val totalDistance = round(km * 10f)/10f
                val totalDistanceString = "${totalDistance}km"
                binding.tvTotalDistance.text = totalDistanceString
            }
        })
        viewModel.totalAvgSpeed.observe(viewLifecycleOwner, Observer {
            it?.let {
                val totalAvgSpeed = round(it *10f)/10f
                val totalDistanceString = "${totalAvgSpeed}km/h"
                binding.tvAverageSpeed.text = totalDistanceString
            }
        })
        viewModel.totalCaloriesBurnt.observe(viewLifecycleOwner, Observer {
            it?.let {
                val caloriesBurned = "${it}kcal"
                binding.tvTotalCalories.text = caloriesBurned
            }
        })
        viewModel.sessionsSortedByDate.observe(viewLifecycleOwner, Observer {
            it?.let {
                val allDistances = it.indices.map {i -> BarEntry(i.toFloat(), it[i].timeInMillis.toFloat())}
                val barDataset = LineDataSet(allDistances, "Session Duration").apply {
                    valueTextColor = Color.BLACK
                    color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
                }
                barDataset.lineWidth = 5f
                barDataset.setDrawValues(false)
                binding.lineChart.data = LineData(barDataset)
                binding.lineChart.setScaleEnabled(false)
                binding.lineChart.invalidate()
            }
        })


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
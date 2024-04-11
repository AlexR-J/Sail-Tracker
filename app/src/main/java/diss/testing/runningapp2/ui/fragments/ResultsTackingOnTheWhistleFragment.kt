package diss.testing.runningapp2.ui.fragments

import android.graphics.Color
import android.graphics.Color.LTGRAY
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import diss.testing.runningapp2.R
import diss.testing.runningapp2.adapters.SessionAdapter
import diss.testing.runningapp2.databinding.FragmentResultsTackingOnTheWhistleBinding
import diss.testing.runningapp2.db.SessionClass
import diss.testing.runningapp2.other.Constants
import diss.testing.runningapp2.other.TrackingUtility
import diss.testing.runningapp2.ui.viewmodels.ResultsViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class ResultsTackingOnTheWhistleFragment: Fragment(R.layout.fragment_results_tacking_on_the_whistle) {
    private val viewModel: ResultsViewModel by viewModels()

    private var _binding: FragmentResultsTackingOnTheWhistleBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionAdapter: SessionAdapter

    companion object {
        var searchId = MutableLiveData<Long>()
        var viewSession = MutableLiveData<SessionClass>()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResultsTackingOnTheWhistleBinding.inflate(inflater, container, false)
        val view = binding.root
        val mMapFragment = childFragmentManager.findFragmentById(diss.testing.runningapp2.R.id.mapView) as SupportMapFragment?
        val session = viewSession.value
        binding.avgSpeed.text = session?.avgSpeedInKmp.toString()
        binding.distance.text = session?.distanceInMeters.toString()
        binding.caloriesBurned.text = session?.caloriesBurned.toString()
        val formattedDate = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        if (session != null) {
            binding.tvTitle.text = formattedDate.format(session.timestamp)
            drawGraph(session)
        }
        val formattedTime = session?.timeInMillis?.let { TrackingUtility.getFormattedTime(it) }
        binding.duration.text = formattedTime
        mMapFragment?.getMapAsync{map ->
            session?.let { drawMap(map, it) }
        }



        ResultsViewModel.searchId.postValue(searchId.value)


        return view
    }

    private fun drawMap(map: GoogleMap, session: SessionClass) {
        val points = session.points.polylines
        val redPoints = session.redPoints.redPointsList
        val markerLocations = session.markers.markers
        for(polyline in points) {
            polyline.forEachIndexed { index, point ->
                if(index > 0) {
                    val currentPoint = point
                    val previousPoint = polyline[index-1]
                    val polylineOptions = PolylineOptions()
                    if(currentPoint in redPoints) {
                        polylineOptions
                            .color(Constants.POLYLINE_ACCENT_COLOR)
                            .width(Constants.POLYLINE_WIDTH)
                            .add(currentPoint)
                            .add(previousPoint)
                    } else {
                        polylineOptions
                            .color(Constants.POLYLINE_COLOR)
                            .width(Constants.POLYLINE_WIDTH)
                            .add(currentPoint)
                            .add(previousPoint)
                    }

                    map?.addPolyline(polylineOptions)
                }
            }
        }
        for(marker in markerLocations) {
            val markerOptions = MarkerOptions()
                .position(marker)
            map?.addMarker(markerOptions)
        }
        val bounds = LatLngBounds.builder()
        for(polyline in points) {
            for(point in polyline) {
                bounds.include(point)
            }
        }
        map.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                binding.mapView.width,
                binding.mapView.height,
                (binding.mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun drawGraph(session: SessionClass) {
        binding.lineChart.legend.isEnabled = false
        binding.lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawLabels(true)
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
            setDrawLabels(false)
        }
        binding.lineChart.axisLeft.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
            setDrawLabels(true)
        }
        binding.lineChart.axisRight.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
            setDrawLabels(false)
        }
        binding.lineChart.apply {
            description.text = ""
        }
        val initialLineValues = ArrayList<Entry>()
        val speeds = session.speeds.speedsList
        val timestamps = session.pointTimeStamps.pointTimeStamps
        var maxSpeed = 0F
        var minSpeed = 0F
        speeds.forEach { speedList ->
            speedList.forEachIndexed { index, speedValue ->
                if(speedValue >= maxSpeed) {
                    maxSpeed = speedValue
                } else if(speedValue <= minSpeed) {
                    minSpeed = speedValue
                }
                val timestamp = timestamps[index]
                initialLineValues.add(Entry(timestamp.toFloat(), speedValue))
            }
        }
        val initialLineDataset = LineDataSet(initialLineValues, "Speed")
        initialLineDataset.lineWidth = 3F
        initialLineDataset.color = Color.WHITE
        initialLineDataset.circleRadius = 0F
        initialLineDataset.setDrawCircles(false)
        initialLineDataset.setDrawValues(false)
        val data = LineData(initialLineDataset)
        binding.lineChart.data = data

        val markerTimeStamps = session.markerTimeStamps.marketTimeStamps
        markerTimeStamps.forEach { timestamp ->
            val newLineData = ArrayList<Entry>()
            newLineData.add(Entry(timestamp.toFloat(), maxSpeed+1))
            newLineData.add(Entry(timestamp.toFloat(), minSpeed-1))
            val newLineDataset = LineDataSet(newLineData, "Marker")
            newLineDataset.lineWidth = 3F
            newLineDataset.color = Color.RED
            newLineDataset.circleRadius = 0F
            newLineDataset.setDrawCircles(false)
            newLineDataset.setDrawValues(false)
            binding.lineChart.data.addDataSet(newLineDataset)
        }

        binding.lineChart.animateXY(1000, 1000, Easing.EaseInCubic)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
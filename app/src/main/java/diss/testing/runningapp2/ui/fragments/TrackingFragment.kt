package diss.testing.runningapp2.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import diss.testing.runningapp2.R
import diss.testing.runningapp2.databinding.FragmentTrackingBinding
import diss.testing.runningapp2.db.RunDAO
import diss.testing.runningapp2.db.SessionClass
import diss.testing.runningapp2.other.Constants.ACTION_PAUSE_SERVICE
import diss.testing.runningapp2.other.Constants.ACTION_SET_LEEWARD
import diss.testing.runningapp2.other.Constants.ACTION_SET_WINDWARD
import diss.testing.runningapp2.other.Constants.ACTION_START_OR_RESUME_SERVICE
import diss.testing.runningapp2.other.Constants.ACTION_STOP_SERVICE
import diss.testing.runningapp2.other.Constants.KEY_CURRENT_SESSION_ID
import diss.testing.runningapp2.other.Constants.MAP_ZOOM
import diss.testing.runningapp2.other.Constants.MET
import diss.testing.runningapp2.other.Constants.POLYLINE_ACCENT_COLOR
import diss.testing.runningapp2.other.Constants.POLYLINE_COLOR
import diss.testing.runningapp2.other.Constants.POLYLINE_WIDTH
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_CANCELED
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_DEFAULT
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_RIVERBANK
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_TACK_ON_WHISTLE
import diss.testing.runningapp2.other.Constants.SESSION_TYPE_TIME_TO_LINE
import diss.testing.runningapp2.other.MarkerList
import diss.testing.runningapp2.other.MarkerTimeStampList
import diss.testing.runningapp2.other.PointTimeStamps
import diss.testing.runningapp2.other.PolylinesList
import diss.testing.runningapp2.other.RedPointsList
import diss.testing.runningapp2.other.SpeedsList
import diss.testing.runningapp2.other.TrackingUtility
import diss.testing.runningapp2.repositories.MainRepository
import diss.testing.runningapp2.services.ListOfSpeeds
import diss.testing.runningapp2.services.Polyline
import diss.testing.runningapp2.services.TrackingService
import diss.testing.runningapp2.ui.viewmodels.MainViewModel
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class TrackingFragment: Fragment(R.layout.fragment_tracking) {

    private var _binding: FragmentTrackingBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val viewModel: MainViewModel by viewModels()
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private var isTracking = false
    private var points = mutableListOf<Polyline>()
    private var speedPoints = mutableListOf<ListOfSpeeds>()
    private var timeInMillis = 0L
    private var menu: Menu? = null
    private var sessionType = SESSION_TYPE_DEFAULT
    private var timeInSeconds = 0L
    private var markerLocations = mutableListOf<LatLng>()
    private var redPoints = mutableListOf<LatLng>()
    private var markerTimeStamps = mutableListOf<Long>()
    private var isFirstStart = true

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @set:Inject
    var weight = 80F

    @SuppressLint("CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        val view = binding.root
        setHasOptionsMenu(true)
        val mMapFragment = childFragmentManager.findFragmentById(diss.testing.runningapp2.R.id.mapView) as SupportMapFragment?
        mMapFragment?.getMapAsync{map ->
            addAllPolylines()
            subscribeToObservers(map)
            binding.btnFinishRun.setOnClickListener{
                zoomWholeTrack(map)
                endAndSaveSession(map)
            }
            binding.mark1Btn.setOnClickListener{
                sendCommandToService(ACTION_SET_LEEWARD)
                binding.leewardCheck.visibility = View.VISIBLE
            }
            binding.mark2Btn.setOnClickListener{
                sendCommandToService(ACTION_SET_WINDWARD)
                binding.windwardCheck.visibility = View.VISIBLE
            }
        }

        binding.btnToggleRun.setOnClickListener{
            toggleRun()
            binding.spinner.visibility = View.GONE
            binding.spinner.visibility = View.INVISIBLE
        }
        binding.btnToggleRun.isClickable = false
        binding.btnToggleRun.setTextColor(Color.GRAY)

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapter: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when(position) {
                    0 -> {
                        sessionType = SESSION_TYPE_DEFAULT
                        binding.mark1Btn.visibility = View.INVISIBLE
                        binding.mark2Btn.visibility = View.INVISIBLE
                        sendCommandToService(SESSION_TYPE_DEFAULT)
                    }
                    1 -> {
                        sessionType = SESSION_TYPE_TACK_ON_WHISTLE
                        binding.mark1Btn.visibility = View.INVISIBLE
                        binding.mark2Btn.visibility = View.INVISIBLE
                        sendCommandToService(SESSION_TYPE_TACK_ON_WHISTLE)
                    }
                    2 -> {
                        sessionType = SESSION_TYPE_TIME_TO_LINE
                        binding.mark1Btn.visibility = View.VISIBLE
                        binding.mark2Btn.visibility = View.VISIBLE
                        sendCommandToService(SESSION_TYPE_TIME_TO_LINE)
                    }
                    3 -> {
                        sessionType = SESSION_TYPE_RIVERBANK
                        binding.mark1Btn.visibility = View.VISIBLE
                        binding.mark2Btn.visibility = View.VISIBLE
                        sendCommandToService(SESSION_TYPE_RIVERBANK)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }


        return view
    }

    private fun addAllPolylines() {
        for(polyline in points) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
        }
    }

    private fun subscribeToObservers(map: GoogleMap) {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.points.observe(viewLifecycleOwner, Observer {
            points = it
            addLatestPolyline(map)
            moveCamera(map)
        })
        TrackingService.speedPoints.observe(viewLifecycleOwner, Observer {
            speedPoints = it
            updateSpeedo(map)
        })

        TrackingService.timeSailedInMillis.observe(viewLifecycleOwner, Observer {
            timeInMillis = it
            val formattedTime = TrackingUtility.getFormattedTime(timeInMillis, true)
            binding.tvTimer.text = formattedTime
        })
        TrackingService.bothMarksSet.observe(viewLifecycleOwner, Observer {
            if(it == true) {
                binding.btnToggleRun.isClickable = true
                binding.btnToggleRun.setTextColor(Color.WHITE)
            }
            if(it == false) {
                binding.btnToggleRun.isClickable = false
                binding.btnToggleRun.setTextColor(Color.GRAY)
            }
        })

        val mediaPlayer = MediaPlayer.create(requireContext(), R.raw.whistle_sound)
        var nextRandomTack = (0..30).random().toInt() + 20
        var redLineCounter = 0

        TrackingService.timeSailedInSeconds.observe(viewLifecycleOwner, Observer {
            timeInSeconds = it
            when(sessionType) {
                SESSION_TYPE_TACK_ON_WHISTLE ->
                    if(timeInSeconds >= nextRandomTack) {
                        mediaPlayer.start()
                        nextRandomTack = (timeInSeconds + ((0..30).random()) + 15).toInt()
                        Timber.d("Next tack at: ${nextRandomTack}s")
                        markerLocations.add(points.last().last())
                        markerTimeStamps.add(timeInMillis)
                        val markerOptions = MarkerOptions()
                            .position(points.last().last())
                        map?.addMarker(markerOptions)
                        redPoints.add(points.last().last())
                        redLineCounter = 10
                    } else if (redLineCounter >= 1) {
                        redPoints.add(points.last().last())
                        redLineCounter -= 1
                    }
            }


        })

    }

    private fun toggleRun() {
        if(isTracking) {
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTracking(isTracking : Boolean) {
        this.isTracking = isTracking
        if(!isTracking && timeInMillis > 0L) {
            binding.btnToggleRun.text = "Start"
            binding.btnToggleRun.setBackgroundColor(Color.GREEN)
            binding.btnFinishRun.visibility = View.VISIBLE
            binding.btnFinishRun.setBackgroundColor(Color.RED)
        } else if(isTracking){
            menu?.getItem(0)?.isVisible = true
            binding.btnToggleRun.text = "Stop"
            binding.btnToggleRun.setBackgroundColor(Color.RED)
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCamera(map: GoogleMap) {

        if(points.isNotEmpty() && points.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    points.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }



    private fun addLatestPolyline(map: GoogleMap) {
        if(points.isNotEmpty() && points.last().size > 1) {
            val secondLastLatLng = points.last()[points.last().size -2]
            val lastLatLng = points.last().last()
            val polylineOptions = PolylineOptions()
            Timber.d("$redPoints")
            Timber.d("$lastLatLng")
            if(secondLastLatLng in redPoints) {
                polylineOptions
                    .color(POLYLINE_ACCENT_COLOR)
                    .width(POLYLINE_WIDTH)
                    .add(secondLastLatLng)
                    .add(lastLatLng)
            } else {
                polylineOptions
                    .color(POLYLINE_COLOR)
                    .width(POLYLINE_WIDTH)
                    .add(secondLastLatLng)
                    .add(lastLatLng)
            }
            map?.addPolyline(polylineOptions)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateSpeedo(map: GoogleMap) {
        if(speedPoints.isNotEmpty() &&points.last().isNotEmpty()) {
           binding.speedReadout.text = "Speed: ${((speedPoints.last().last() * 18/5) * 10 ) / 10}"
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if(timeInMillis > 0L) {
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.cancel_button ->
                showCancelAlert()
        }
        return super.onOptionsItemSelected(item)
    }


    @SuppressLint("SetTextI18n", "CommitPrefEdits")
    private fun stopRun() {
        binding.tvTimer.text = "00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        when(sessionType) {
            SESSION_TYPE_DEFAULT  -> findNavController().navigate(R.id.action_trackingFragment_to_sessionFragment)
            SESSION_TYPE_TACK_ON_WHISTLE -> findNavController().navigate(R.id.action_trackingFragment_to_resultsTackingOnTheWhistleFragment)
            SESSION_TYPE_TIME_TO_LINE  -> findNavController().navigate(R.id.action_trackingFragment_to_sessionFragment)
            SESSION_TYPE_RIVERBANK  -> findNavController().navigate(R.id.action_trackingFragment_to_sessionFragment)
            SESSION_TYPE_CANCELED -> findNavController().navigate(R.id.action_trackingFragment_to_sessionFragment)

        }

    }

    private fun showCancelAlert() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the run?")
            .setMessage("Are you sure you want to cancel the current run and delete all data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _, _ ->
                sessionType = SESSION_TYPE_CANCELED
                stopRun()
            }
            .setNegativeButton("No") {dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun zoomWholeTrack(map: GoogleMap) {
        val bounds = LatLngBounds.builder()
        for(polyline in points) {
            for(point in polyline) {
                bounds.include(point)
            }
        }
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                binding.mapView.width,
                binding.mapView.height,
                (binding.mapView.height * 0.05f).toInt()
            )
        )

    }

    private fun addAllPointsAndLines(map: GoogleMap) {
        for(polyline in points) {
            polyline.forEachIndexed { index, point ->
                if(index > 0) {
                    val currentPoint = point
                    val previousPoint = polyline[index-1]
                    val polylineOptions = PolylineOptions()
                    if(currentPoint in redPoints) {
                        polylineOptions
                            .color(POLYLINE_ACCENT_COLOR)
                            .width(POLYLINE_WIDTH)
                            .add(currentPoint)
                            .add(previousPoint)
                    } else {
                        polylineOptions
                            .color(POLYLINE_COLOR)
                            .width(POLYLINE_WIDTH)
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
    }

    private fun endAndSaveSession(map: GoogleMap) {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for(polyline in points) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed = round((distanceInMeters / 1000f) / (timeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = (MET  * weight * 3.5).toInt()
            val pointsObject = PolylinesList(points)
            val speedList = SpeedsList(speedPoints)
            val redPointsList = RedPointsList(redPoints)
            val markersList = MarkerList(markerLocations)
            val markerTimeStampList = MarkerTimeStampList(markerTimeStamps)
            val pointTimeStampList = PointTimeStamps(TrackingService.pointTimeStamps.value!!)
            var currentSessionId = sharedPreferences.getInt(KEY_CURRENT_SESSION_ID, 0)
            Timber.d("adding run with id $currentSessionId")
            ResultsTackingOnTheWhistleFragment.searchId.postValue(dateTimestamp)
            val run = SessionClass(bmp, dateTimestamp, avgSpeed, distanceInMeters, timeInMillis, caloriesBurned, pointsObject, speedList, redPointsList, markersList, markerTimeStampList, pointTimeStampList, currentSessionId)
            viewModel.insertSession(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            sharedPreferences.edit()
                .putInt(KEY_CURRENT_SESSION_ID, currentSessionId+1)
                .apply()
            ResultsTackingOnTheWhistleFragment.viewSession.postValue(run)
            addAllPointsAndLines(map)
            stopRun()

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
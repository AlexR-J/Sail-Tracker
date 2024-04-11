package diss.testing.runningapp2.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import diss.testing.runningapp2.R
import diss.testing.runningapp2.databinding.FragmentSessionBinding
import diss.testing.runningapp2.databinding.FragmentSetupBinding
import diss.testing.runningapp2.other.Constants.KEY_CURRENT_SESSION_ID
import diss.testing.runningapp2.other.Constants.KEY_FIRST_TIME_TOGGLE
import diss.testing.runningapp2.other.Constants.KEY_NAME
import diss.testing.runningapp2.other.Constants.KEY_WEIGHT
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment: Fragment(R.layout.fragment_setup) {

    private var _binding: FragmentSetupBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    @Inject
    lateinit var sharedPreferences : SharedPreferences

    @set:Inject
    var isFirstOpen = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val view = binding.root

        if(!isFirstOpen) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()
            findNavController().navigate(
                R.id.action_setupFragment_to_sessionFragment,
                savedInstanceState,
                navOptions
            )
        }

        binding.tvContinue.setOnClickListener {
            val successCheck = writeDataToSharedPref()
            if(successCheck) {
                findNavController().navigate(R.id.action_setupFragment_to_sessionFragment)
            } else {
                Snackbar.make(requireView(), "Please fill all fields", Snackbar.LENGTH_SHORT).show()
            }

        }

        return view


    }

    private fun writeDataToSharedPref():Boolean {
        val name = binding.etName.text.toString()
        val weight = binding.etWeight.text.toString()
        if(name.isEmpty()||weight.isEmpty()) {
            return false
        }
        sharedPreferences.edit()
            .putString(KEY_NAME, name)
            .putFloat(KEY_WEIGHT, weight.toFloat())
            .putBoolean(KEY_FIRST_TIME_TOGGLE, false)
            .putInt(KEY_CURRENT_SESSION_ID, 0)
            .apply()
        val toolBarText = "Let's go, $name!"
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
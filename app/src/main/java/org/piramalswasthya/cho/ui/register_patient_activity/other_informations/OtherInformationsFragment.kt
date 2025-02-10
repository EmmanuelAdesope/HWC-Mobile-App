package org.piramalswasthya.cho.ui.register_patient_activity.other_informations

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.piramalswasthya.cho.R
import org.piramalswasthya.cho.adapter.dropdown_adapters.DropdownAdapter
import org.piramalswasthya.cho.adapter.model.DropdownList
import org.piramalswasthya.cho.databinding.FragmentOtherInformationsBinding
import org.piramalswasthya.cho.model.Patient
import org.piramalswasthya.cho.ui.commons.NavigationAdapter
import org.piramalswasthya.cho.ui.commons.SpeechToTextContract
import org.piramalswasthya.cho.ui.home_activity.HomeActivity
import org.piramalswasthya.cho.utils.generateUuid

@AndroidEntryPoint
class OtherInformationsFragment : Fragment() , NavigationAdapter {


    companion object {
        fun newInstance() = OtherInformationsFragment()
    }

    private var _binding: FragmentOtherInformationsBinding? = null
    private val binding: FragmentOtherInformationsBinding
        get() = _binding!!

    private lateinit var viewModel: OtherInformationsViewModel

    private var patient : Patient? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOtherInformationsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
    private val speechToTextLauncherForName = registerForActivityResult(SpeechToTextContract()) { result ->
        if (result.isNotBlank() && result.isNotEmpty() && !result.any { it.isDigit() }) {
            binding.parentGurdianName.setText(result)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(OtherInformationsViewModel::class.java)
        patient = arguments?.getSerializable("patient") as? Patient
        setChangeListeners()
        setAdapters()
        binding.parentGurdianNameText.setEndIconOnClickListener {
            speechToTextLauncherForName.launch(Unit)
        }
    }

    private fun setChangeListeners(){
        binding.communityDropdown.setOnItemClickListener { parent, _, position, _ ->
            viewModel.selectedCommunity = viewModel.communityList[position];
            binding.communityDropdown.setText(viewModel.selectedCommunity!!.communityType, false)
        }

        binding.religionDropdown.setOnItemClickListener { parent, _, position, _ ->
            viewModel.selectedReligion = viewModel.religionList[position];
            binding.religionDropdown.setText(viewModel.selectedReligion!!.religionType, false)
        }
    }

    private fun setAdapters(){
        viewModel.community.observe(viewLifecycleOwner) { state ->
            when (state!!){
                OtherInformationsViewModel.NetworkState.SUCCESS -> {
                    val dropdownList = viewModel.communityList.map { it -> DropdownList(it.communityID, it.communityType) }
                    val dropdownAdapter = DropdownAdapter(requireContext(), R.layout.drop_down, dropdownList, binding.communityDropdown)
                    binding.communityDropdown.setAdapter(dropdownAdapter)
                }
                else -> {

                }
            }
        }

        viewModel.religion.observe(viewLifecycleOwner) { state ->
            when (state!!){
                OtherInformationsViewModel.NetworkState.SUCCESS -> {
                    val dropdownList = viewModel.religionList.map { it -> DropdownList(it.religionID, it.religionType) }
                    val dropdownAdapter = DropdownAdapter(requireContext(), R.layout.drop_down, dropdownList, binding.religionDropdown)
                    binding.religionDropdown.setAdapter(dropdownAdapter)
                }
                else -> {

                }
            }
        }
    }


    override fun getFragmentId(): Int {
        return R.id.fragment_other_informations;
    }

    override fun onSubmitAction() {
        updatePatientDetails()
        if(patient != null){
            patient!!.patientID = generateUuid()
            viewModel.insertPatient(patient!!)
        }
        val intent = Intent(context, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    fun updatePatientDetails(){

        if(patient != null){
            patient!!.parentName = binding.parentGurdianName.text.toString()
        }

        if(viewModel.selectedCommunity != null && patient != null){
            patient!!.communityID = viewModel.selectedCommunity!!.communityID;
        }

        if(viewModel.selectedReligion != null && patient != null){
            patient!!.religionID = viewModel.selectedReligion!!.religionID;
        }

    }


    override fun onCancelAction() {
        findNavController().navigateUp()
    }

}
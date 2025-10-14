package com.example.tunespipe.ui.donate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tunespipe.databinding.FragmentDonateBinding

class DonateFragment : Fragment() {

    private var _binding: FragmentDonateBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val donateViewModel =
            ViewModelProvider(this).get(DonateViewModel::class.java)

        _binding = FragmentDonateBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDonate
        donateViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
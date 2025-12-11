package com.risuncode.mybest.ui.tugas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.risuncode.mybest.R

class TugasEmptyFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"

        fun newInstance(title: String, description: String): TugasEmptyFragment {
            return TugasEmptyFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_DESCRIPTION, description)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tugas_empty, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val description = arguments?.getString(ARG_DESCRIPTION) ?: ""

        view.findViewById<TextView>(R.id.tvTitle)?.text = title
        view.findViewById<TextView>(R.id.tvDescription)?.text = description
    }
}

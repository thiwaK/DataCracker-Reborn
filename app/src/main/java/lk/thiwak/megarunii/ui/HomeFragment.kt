package lk.thiwak.megarunii.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import lk.thiwak.megarunii.*
import androidx.fragment.app.Fragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val text = view.findViewById<TextView>(R.id.text_a)

        // Optionally start another activity (uncomment if needed)
        // val intent = Intent(requireContext(), WebViewActivity::class.java)
        // startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister resources if required to prevent memory leaks
    }
}


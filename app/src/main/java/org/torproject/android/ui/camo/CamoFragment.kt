package org.torproject.android.ui.camo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.torproject.android.R
import org.torproject.android.ui.MoreActionAdapter
import org.torproject.android.ui.OrbotMenuAction

class CamoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camo, container, false)
        val rvCamoApps = view.findViewById<RecyclerView>(R.id.rvCamoApps)
        val listItems =
            listOf(
                OrbotMenuAction(
                    R.string.app_icon_chooser_label_todo,
                    R.drawable.ic_camouflage_todo,
                    removeTint = true
                ) {
                    showDialog(R.drawable.ic_camouflage_todo, R.string.app_icon_chooser_label_todo)
                },
                OrbotMenuAction(
                    R.string.app_icon_chooser_label_assistant,
                    R.drawable.ic_camouflage_assistant,
                    removeTint = true
                ) {
                    showDialog(R.drawable.ic_camouflage_assistant, R.string.app_icon_chooser_label_assistant)
                },
                OrbotMenuAction(
                    R.string.app_icon_chooser_label_tetras,
                    R.drawable.ic_camouflage_tetras,
                    removeTint = true
                ) {
                    showDialog(R.drawable.ic_camouflage_tetras, R.string.app_icon_chooser_label_tetras)
                },
                OrbotMenuAction(
                    R.string.app_icon_chooser_label_paint,
                    R.drawable.ic_camouflage_paint,
                    removeTint = true
                ) {
                    showDialog(R.drawable.ic_camouflage_paint, R.string.app_icon_chooser_label_paint)
                },
                OrbotMenuAction(
                    R.string.app_icon_chooser_label_night_watch,
                    R.drawable.ic_camouflage_night_watch,
                    removeTint = true
                ) {
                    showDialog(R.drawable.ic_camouflage_night_watch, R.string.app_icon_chooser_label_night_watch)
                }
            )
        rvCamoApps.adapter = MoreActionAdapter(listItems)
        val spanCount = if (resources.configuration.screenWidthDp < 600) 2 else 4
        rvCamoApps.layoutManager = GridLayoutManager(requireContext(), spanCount)
        return view
    }

    private fun showDialog(@DrawableRes imageId: Int, @StringRes appName: Int) {
        CamoConfirmationDialogFragment.newInstance(imageId, appName)
            .show(requireActivity().supportFragmentManager, CamoConfirmationDialogFragment.TAG)
    }


}
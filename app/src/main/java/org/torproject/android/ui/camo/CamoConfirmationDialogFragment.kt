package org.torproject.android.ui.camo

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import `in`.myinnos.library.AppIconNameChanger
import org.torproject.android.BuildConfig
import org.torproject.android.R

class CamoConfirmationDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val foo = mapOf<String?, String>(
            getString(R.string.app_name) to "org.torproject.android.OrbotActivity",
            getString(R.string.app_icon_chooser_label_night_watch) to "org.torproject.android.main.NightWatch",
            getString(R.string.app_icon_chooser_label_assistant) to "org.torproject.android.main.Assistant",
            getString(R.string.app_icon_chooser_label_paint) to "org.torproject.android.main.Paint",
            getString(R.string.app_icon_chooser_label_tetras) to "org.torproject.android.main.Tetras",
            getString(R.string.app_icon_chooser_label_todo) to "org.torproject.android.main.Todo"
        )

        val camoAppName = getString(args.getInt(BUNDLE_KEY_NAME))
        val ad = AlertDialog.Builder(context)
            .setIcon(args.getInt(BUNDLE_KEY_IMAGE_ID))
            .setTitle(
                getString(
                    R.string.camo_dialog_title,
                    camoAppName
                )
            )
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                dismiss()
            }
            .setMessage(
                getString(
                    R.string.camo_dialog_enable_confirm_msg,
                    camoAppName, camoAppName
                )
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val activeName = foo[camoAppName]
                val disabledNames = foo.values.filter { s -> s != activeName }
                AppIconNameChanger.Builder(requireActivity())
                    .packageName(BuildConfig.APPLICATION_ID)
                    .activeName(activeName)
                    .disableNames(disabledNames)
                    .build()
                    .setNow()
            }
            .create()
        return ad
    }

    companion object {
        const val BUNDLE_KEY_IMAGE_ID = "id"
        const val BUNDLE_KEY_NAME = "name"
        const val TAG = "CamoConfirmDialog"
        fun newInstance(
            drawableId: Int,
            name: Int
        ): CamoConfirmationDialogFragment = CamoConfirmationDialogFragment().apply {
            arguments = bundleOf(
                BUNDLE_KEY_IMAGE_ID to drawableId,
                BUNDLE_KEY_NAME to name
            )
        }
    }
}
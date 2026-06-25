package org.torproject.android.ui.kindness

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.torproject.android.R
import org.torproject.android.util.createWithCurves

class UPnPDialogFragment : DialogFragment() {
    @SuppressLint("InlinedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var msg = String.format(
            "%s\n\n%s",
            getString(R.string.kindness_quality_upgrade_line1),
            getString(R.string.kindness_quality_upgrade_line2)
        )
        val builder = AlertDialog.Builder(requireContext(), R.style.OrbotDialogTheme)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.kindness_quality_upgrade_title)
            .setMessage(msg)

        val accessLocalNetworkNeeded =
            needsAccessLocalNetworkPermission(requireContext()) ?: return builder.createWithCurves()
        if (accessLocalNetworkNeeded) {
            msg += "\n\n${getString(R.string.kindness_quality_upgrade_need_local_network)}"
            val repeatedlyDenied = ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_LOCAL_NETWORK
            )
            val permissionButtonText =
                if (repeatedlyDenied) R.string.set_permission_in_settings else R.string.grant_permission
            builder
                .setNeutralButton(permissionButtonText) { _, _ ->
                    if (repeatedlyDenied) {
                        activity?.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", activity?.packageName, null)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    }
                }
        } else {
            msg += "\n\n${getString(R.string.kindness_quality_upgrade_perm_granted)}"
        }
        return builder
            .setMessage(msg)
            .createWithCurves()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }


    companion object {
        private const val TAG = "UPnPDialog"

        fun show(fragmentManager: FragmentManager) {
            UPnPDialogFragment().show(fragmentManager, TAG)
        }

        // returns null if permission Android 36 or lower, else true/false
        fun needsAccessLocalNetworkPermission(context: Context): Boolean? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
                return null
            }
            val checkAccessLocalNetworkPerm =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_LOCAL_NETWORK
                )
            if (checkAccessLocalNetworkPerm == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "local network permission not granted for UPnP")
                return true
            }
            Log.d(TAG, "Local network permission granted for UPnP")
            return false
        }
    }
}
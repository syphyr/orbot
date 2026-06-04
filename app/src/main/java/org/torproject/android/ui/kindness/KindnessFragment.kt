package org.torproject.android.ui.kindness

import IPtProxy.IPtProxy
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.torproject.android.R
import org.torproject.android.Regionalization
import org.torproject.android.databinding.FragmentKindnessBinding
import org.torproject.android.util.Prefs

class KindnessFragment : Fragment() {

    private lateinit var mBinding: FragmentKindnessBinding
    private var mService: SnowflakeProxyService? = null
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SnowflakeProxyService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            mService = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentKindnessBinding.inflate(inflater)

        mBinding.swVolunteerMode.isChecked = Prefs.beSnowflakeProxy()
        mBinding.swVolunteerMode.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setBeSnowflakeProxy(isChecked)
            activity?.let {
                if (isChecked) {
                    SnowflakeProxyService.startSnowflakeProxyForegroundService(it)
                } else {
                    SnowflakeProxyService.stopSnowflakeProxyForegroundService(it)
                    updateNatTypeUi(IPtProxy.NATUnknown)
                }
            }
        }

        mBinding.rowUsageLimits.setOnClickListener {
            KindnessConfigBottomSheet.show(parentFragmentManager)
        }

        updateNatTypeUi(IPtProxy.NATUnknown)

        mBinding.rowProxyQuality.setOnClickListener {
            if (Prefs.lastSnowflakeNatType == IPtProxy.NATRestricted) {
                showQualityHint()
            }
        }

        mBinding.btnActionActivate.setOnClickListener {
            TestingDialogFragment.show(parentFragmentManager)
        }

        if (Regionalization.isKindnessModeDisabledForCountry()) {
            mBinding.btnActionActivate.isEnabled = false

            // set text explaining that kindness mode isn't available from the user's country
            mBinding.tvActivateInstructions.text =
                getString(
                    R.string.kindness_mode_unsupported_country,
                    Regionalization.getLocalizedNameForCountryCode()
                )

            // set the activate button to be gray, making it not the primary button
            ViewCompat.setBackgroundTintList(
                mBinding.btnActionActivate,
                ColorStateList.valueOf(resources.getColor(R.color.orbot_btn_disable_grey, null))
            )
            ViewCompat.setBackgroundTintList(
                mBinding.btnActionLearnMore,
                ColorStateList.valueOf(resources.getColor(R.color.orbot_btn_enabled_purple, null))
            )
        }

        mBinding.btnActionLearnMore.setOnClickListener {
            activity?.let {
                startActivity(Intent(Intent.ACTION_VIEW, URL_ABOUT_KINDNESS.toUri()))
            }
        }

        showPanelStatus(!Prefs.snowflakeNeedsQualityCheck)

        parentFragmentManager.setFragmentResultListener(
            KindnessConfigBottomSheet.KEY_CONFIG_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            updateUsageLimitsUi()
        }

        parentFragmentManager.setFragmentResultListener(
            TestingDialogFragment.KEY_RESULT,
            viewLifecycleOwner
        ) { _, bundle ->

            if (bundle.getBoolean(TestingDialogFragment.KEY_RESULT)) {
                if (!Prefs.snowflakeNeedsQualityCheck) {
                    mBinding.swVolunteerMode.isChecked = true
                    showPanelStatus(true)
                }
            }
        }

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()
        // Updates these values when user returns to screen after running snowflake proxy for some time.
        updateUsageLimitsUi()
        updateNatTypeUi(Prefs.lastSnowflakeNatType)
        mBinding.tvAlltimeTotal.text = "${Prefs.snowflakesServed}"
        mBinding.tvWeeklyTotal.text = "${Prefs.snowflakesServedWeekly}"
    }

    private val natTypeObserver =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key != Prefs.PREF_LAST_SNOWFLAKE_NAT_TYPE) return@OnSharedPreferenceChangeListener
            updateNatTypeUi(Prefs.lastSnowflakeNatType)
        }

    override fun onStart() {
        super.onStart()
        PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(natTypeObserver)
    }

    override fun onStop() {
        super.onStop()
        PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(natTypeObserver)

        if (mBound) {
            context?.unbindService(connection)
            mBound = false
        }
    }

    private fun updateNatTypeUi(natType: String) {
        mBinding.tvProxyQualityStatus.text = when (natType) {
            IPtProxy.NATUnknown -> getString(R.string.kindness_proxy_quality_unknown)
            IPtProxy.NATRestricted -> getString(R.string.kindness_proxy_quality_restricted)
            IPtProxy.NATUnrestricted -> getString(R.string.kindness_proxy_quality_unrestricted)
            else -> natType
        }

        if (natType == IPtProxy.NATRestricted) {
            mBinding.redDot.visibility = View.VISIBLE
            mBinding.chevron2.visibility = View.VISIBLE
        } else {
            mBinding.redDot.visibility = View.GONE
            mBinding.chevron2.visibility = View.GONE
        }
    }

    private fun updateUsageLimitsUi() {
        mBinding.tvUsageLimitsStatus.text =
            getString(
                if (Prefs.limitSnowflakeProxyingWifi() || Prefs.limitSnowflakeProxyingCharging())
                    R.string.kindness_usage_limits_status_on
                else R.string.kindness_usage_limits_status_off
            )
    }

    private fun showQualityHint() =
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.kindness_quality_upgrade_title)
            .setMessage(
                String.format(
                    "%s\n\n%s",
                    getString(R.string.kindness_quality_upgrade_line1),
                    getString(R.string.kindness_quality_upgrade_line2)
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()

    private fun showPanelStatus(isActivated: Boolean) {
        val duration = 250L
        if (isActivated) {
            mBinding.panelKindnessActivate.animate().alpha(0f).setDuration(0).withEndAction {
                mBinding.panelKindnessActivate.visibility = View.GONE
            }

            mBinding.panelKindnessStatus.visibility = View.VISIBLE
            mBinding.panelKindnessStatus.animate().alpha(1f).duration = duration
        } else {
            mBinding.panelKindnessActivate.visibility = View.VISIBLE
            mBinding.panelKindnessActivate.animate().alpha(1f).duration = duration

            mBinding.panelKindnessStatus.animate().alpha(0f).setDuration(0).withEndAction {
                mBinding.panelKindnessStatus.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val URL_ABOUT_KINDNESS = "https://orbot.app/kindness"
    }
}

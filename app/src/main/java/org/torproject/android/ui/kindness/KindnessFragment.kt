package org.torproject.android.ui.kindness

import IPtProxy.IPtProxy
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.torproject.android.R
import org.torproject.android.databinding.FragmentKindnessBinding
import org.torproject.android.service.circumvention.BuiltInBridges
import org.torproject.android.service.circumvention.Transport
import org.torproject.android.util.Prefs
import java.util.Locale

class KindnessFragment : Fragment() {

    private lateinit var mBinding: FragmentKindnessBinding
    private var mService: SnowflakeProxyService? = null
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SnowflakeProxyService.LocalBinder
            mService = binder.getService()
            mBound = true
            observeNatType()
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

        mBinding.tvProxyQualityStatus.text = getString(R.string.kindness_proxy_quality_unknown)

        mBinding.rowProxyQuality.setOnClickListener {
            if (mService?.natType?.value == IPtProxy.NATRestricted) {
                showQualityHint()
            }
        }

        mBinding.btnActionActivate.setOnClickListener {
            TestingDialogFragment.show(parentFragmentManager)
        }

        mBinding.btnActionLearnMore.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, "https://orbot.app/kindness".toUri())
            val pm = context?.packageManager

            if (pm != null && i.resolveActivity(pm) != null) {
                startActivity(i)
            }
        }

        showPanelStatus(!Prefs.snowflakeNeedsQualityCheck)

        parentFragmentManager.setFragmentResultListener(
            KindnessConfigBottomSheet.KEY_CONFIG_CHANGED,
             viewLifecycleOwner) { _, _ ->
                updateUsageLimitsUi()
            }

        parentFragmentManager.setFragmentResultListener(
            TestingDialogFragment.KEY_RESULT,
            viewLifecycleOwner) { _, bundle ->

            if (bundle.getBoolean(TestingDialogFragment.KEY_RESULT)) {
                mBinding.swVolunteerMode.isChecked = true
            }
        }

        return mBinding.root
    }

    override fun onStart() {
        super.onStart()

        context?.let {
            it.bindService(SnowflakeProxyService.getIntent(it), connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()

        // Updates these values when user returns to screen after running snowflake proxy for some time.

        updateUsageLimitsUi()

        mBinding.tvAlltimeTotal.text = "${Prefs.snowflakesServed}"
        mBinding.tvWeeklyTotal.text = "${Prefs.snowflakesServedWeekly}"
    }

    override fun onStop() {
        super.onStop()

        if (mBound) {
            context?.unbindService(connection)
            mBound = false
        }
    }

    private fun observeNatType() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mService?.natType?.collect { natType ->
                    updateNatTypeUi(natType)
                }
            }
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
        }
        else {
            mBinding.redDot.visibility = View.GONE
            mBinding.chevron2.visibility = View.GONE
        }
    }

    private fun updateUsageLimitsUi() {
        mBinding.tvUsageLimitsStatus.text =
            getString(if (Prefs.limitSnowflakeProxyingWifi() || Prefs.limitSnowflakeProxyingCharging())
                R.string.kindness_usage_limits_status_on
            else R.string.kindness_usage_limits_status_off)
    }

    private fun showQualityHint() {
        val context = context ?: return

        AlertDialog.Builder(context)
            .setTitle(R.string.kindness_quality_upgrade_title)
            .setMessage(String.format("%s\n\n%s",
                getString(R.string.kindness_quality_upgrade_line1),
                getString(R.string.kindness_quality_upgrade_line2)))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

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
}

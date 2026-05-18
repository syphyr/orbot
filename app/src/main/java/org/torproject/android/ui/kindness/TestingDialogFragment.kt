package org.torproject.android.ui.kindness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.torproject.android.R
import org.torproject.android.databinding.FragmentTestingBinding
import org.torproject.android.service.circumvention.Transport
import org.torproject.android.ui.connect.ConnectUiState
import org.torproject.android.ui.connect.ConnectViewModel
import org.torproject.android.util.Prefs
import org.torproject.android.util.sendIntentToService
import org.torproject.jni.TorService
import org.torproject.jni.TorService.EXTRA_STATUS
import org.torproject.jni.TorService.STATUS_ON
import kotlin.getValue

class TestingDialogFragment : DialogFragment() {

    private lateinit var mBinding: FragmentTestingBinding
    val torConnectedViewModel: ConnectViewModel by activityViewModels()

    private var stoppedNormalTorConnection = false

    private var connectionTestServiceConnection: ServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO do something to ensure rotation change in fragment doesn't break things
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentTestingBinding.inflate(inflater, container, false)

        mBinding.tvTitleApproved.text = getString(R.string.testing_title_approved, "✅")
        mBinding.tvTitleDeclined.text = getString(R.string.testing_title_declined, "\uD83D\uDEAB")

        mBinding.boxTesting.visibility = View.VISIBLE
        mBinding.boxApproved.visibility = View.GONE
        mBinding.boxDeclined.visibility = View.GONE

        mBinding.btContinue.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean(KEY_RESULT, true)

            setFragmentResult(KEY_RESULT, bundle)
            dismiss()
        }

        mBinding.btOk.setOnClickListener {
            setFragmentResult(KEY_RESULT, Bundle())
            dismiss()
        }

        return mBinding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(requireActivity())
            val width = metrics.bounds.width()
            val height = metrics.bounds.height()

            val dialogHeight =
                if (width > height) (height * 0.9f).toInt() else (height * 0.33f).toInt()

            val dialogWidth =
                if (width > height) (width * 0.33f).toInt() else (width * 0.9f).toInt()

            window.setLayout(dialogWidth, dialogHeight)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }

        // - If connected to Tor without a bridge right now, we're fine: dismiss immediately like `btContinue`.
        // - If last test success timestamp is younger than 1 day, dismiss immediately like `btContinue`.
        // - Else, set bridge to `NONE`, DISABLE proxy, start Tor, wait until success or timeout via SnowflakeTorTestService
        // - After test, stop tor again. If we interrupted the user's tor connection, relaunch OrbotService
        // - Store timestamp of success in `Prefs`.

        if (!Prefs.snowflakeNeedsQualityCheck) {
            Log.wtf(TAG, "we don't need a quality check!")
            mBinding.btContinue.callOnClick()
            return
        }

        val torConnectionState = torConnectedViewModel.uiState.value

        if (torConnectionState == ConnectUiState.On && Prefs.transport == Transport.NONE && Prefs.outboundProxy.first == null) {
            Log.wtf(TAG, "there's an active direct connection to tor, stop testing")
            Prefs.snowflakeNeedsQualityCheck = false
            mBinding.btContinue.callOnClick()
            return
        }

        lifecycleScope.launch {
            if (torConnectionState is ConnectUiState.On || torConnectionState is ConnectUiState.Starting) {
                Log.wtf(TAG, "OrbotService is running, we need to turn it off")
                stoppedNormalTorConnection = true
                requireActivity().sendIntentToService(TorService.ACTION_STOP)
                delay(250)
            }

            if (torConnectedViewModel.uiState.value != ConnectUiState.Off) {
                stoppedNormalTorConnection = false
                showTestFailedUi()
                Log.wtf(TAG, "OrbotService isn't off yet")
            }

            Log.wtf(TAG, "current tor state is ${torConnectedViewModel.uiState.value}")

            connectionTestServiceConnection =
                TestTorForSnowflakeProxyService.launchTorTestingService(
                    requireActivity(),
                    torStatusReceiver
                )

            delay(CONNECTION_TEST_TIMEOUT_MS)
            // if we haven't established a connection, cleanup and show error state
            if (connectionTestServiceConnection != null) {
                Log.wtf(
                    TAG,
                    "Couldn't establish a tor connection after waiting for $CONNECTION_TEST_TIMEOUT_MS"
                )
                unbindServiceIfBound()
                showTestFailedUi()
            }
        }
    }

    val torStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(EXTRA_STATUS)
            Log.wtf(TAG, "Got tor status from testing service: $status")
            if (status == STATUS_ON) {
                lifecycleScope.launch {
                    Prefs.snowflakeNeedsQualityCheck = false
                    unbindServiceIfBound()
                    showTestPassedUi()
                    if (stoppedNormalTorConnection) {
                        delay(250)
                        Log.wtf(TAG, "relaunching OrbotService...")
                        requireActivity().sendIntentToService(TorService.ACTION_START)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindServiceIfBound()
    }

    private fun unbindServiceIfBound() {
        if (connectionTestServiceConnection != null) {
            Log.wtf(TAG, "unregistering receiver, killing service")
            val connection = connectionTestServiceConnection!!
            requireActivity().unregisterReceiver(torStatusReceiver)
            requireActivity().unbindService(connection)
            connectionTestServiceConnection = null
        }
    }

    fun showTestPassedUi() {
        Prefs.snowflakeNeedsQualityCheck = false
        mBinding.boxTesting.visibility = View.GONE
        mBinding.boxApproved.visibility = View.VISIBLE
    }

    fun showTestFailedUi() {
        Prefs.snowflakeNeedsQualityCheck = true
        mBinding.boxTesting.visibility = View.GONE
        mBinding.boxDeclined.visibility = View.VISIBLE
    }

    companion object {
        const val KEY_RESULT = "kindness_test_result"
        const val TAG = "TestingFragment"
        const val CONNECTION_TEST_TIMEOUT_MS = 90 * 1000L

        fun show(fragmentManager: FragmentManager) {
            TestingDialogFragment().show(fragmentManager, TAG)
        }
    }
}

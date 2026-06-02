package org.torproject.android.ui.kindness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.torproject.android.R
import org.torproject.android.databinding.FragmentTestingBinding
import org.torproject.android.service.circumvention.Transport
import org.torproject.android.service.vpn.VpnServicePrepareWrapper
import org.torproject.android.ui.connect.ConnectUiState
import org.torproject.android.ui.connect.ConnectViewModel
import org.torproject.android.util.NetworkUtils
import org.torproject.android.util.Prefs
import org.torproject.android.util.sendIntentToService
import org.torproject.jni.TorService
import kotlin.getValue

/**
 * Kindness Mode Quality Test
 *
 * - First, immediately fail the test if there's a non orbot VPN running
 * - Second, if we've passed a quality test in the past 24 hours, skip retesting
 *      otherwise, take the test:
 *      A: if the user is connected to tor with no bridges/proxy, you pass
 *      B: if the user isn't connected to tor, warn the user about connecting to tor and attempt
 *         a direct connection. Pass if we succeed.
 *      C: if the user has a bridge/proxy, turn tor off. perform option B. When the test is
 *         completed, turn the user's original Tor connection back on.
 *
 *   Set Prefs.snowflakeNeedsQualityCheck to false if test passes, true if otherwise
 */
class TestingDialogFragment : DialogFragment() {

    private lateinit var mBinding: FragmentTestingBinding
    val torConnectedViewModel: ConnectViewModel by activityViewModels()

    private var stoppedNormalTorConnection = false

    private var connectionTestServiceConnection: ServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // disable device rotation while this dialog is running
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentTestingBinding.inflate(inflater, container, false)

        mBinding.tvTitleApproved.text = getString(R.string.testing_title_approved, "✅")
        mBinding.tvTitleDeclined.text = getString(R.string.testing_title_declined, "\uD83D\uDEAB")
        mBinding.btnAbortTest.setOnClickListener { dismiss() }
        mBinding.btContinue.setOnClickListener {
            setFragmentResult(KEY_RESULT, Bundle().apply {
                putBoolean(KEY_RESULT, true)
            })
            dismiss()
        }

        mBinding.btnDeclinedBoxOk.setOnClickListener {
            setFragmentResult(KEY_RESULT, Bundle())
            dismiss()
        }

        return mBinding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // benign tests to immediately see if the user can/can't use kindness mode
            // if we don't get a definite answer, prompt the user for consent to determine for sure
            doQualityTestRequiringNoUserConsent()
        }

    }

    /**
     * This part of the connection test doesn't require user consent
     * It automatically fails if:
     *  - Orbot doesn't have a direct Internet connection
     *  - the user is using a non-Orbot VPN
     *
     * If we didn't automatically fail, we can automatically pass if:
     *  - the user currently has a direct connection to the tor network
     *  - the user has passed the quality test in the past 24 hours
     *
     *  Otherwise, the test is still inconclusive. Obtain the user's consent to complete the text
     *  and give them the option to stop testing...
     */
    private fun doQualityTestRequiringNoUserConsent() {

        // immediately fail if there's another VPN running
        if (NetworkUtils.isNonOrbotVpnActive(requireContext())) {
            showTestFailedUi(
                errorExplanation = getString(R.string.testing_explanation_other_vpn),
                bubbleMsg = getString(R.string.testing_explanation_other_vpn_bubble),
                bubbleAction = {
                    VpnServicePrepareWrapper.openVpnSystemSettings(this)
                    dismiss()
                }
            )
            return
        }

        // immediately fail if there's no internet
        val torConnectionState = torConnectedViewModel.uiState.value
        if (torConnectionState == ConnectUiState.NoInternet) {
            showTestFailedUi(bubbleMsg = getString(R.string.testing_explanation_no_net))
            return
        }

        // immediately succeed if we've recently succeeded
        if (!Prefs.snowflakeNeedsQualityCheck) {
            Log.wtf(TAG, "recently passed quality check, proceeding")
            mBinding.btContinue.callOnClick()
            return
        }

        // immediately succeed if you're already connecting directly to Tor
        if (torConnectionState == ConnectUiState.On && Prefs.transport == Transport.NONE && Prefs.outboundProxy.first == null) {
            Log.wtf(TAG, "there's an active direct connection to tor, stop testing")
            Prefs.snowflakeNeedsQualityCheck = false
            mBinding.btContinue.callOnClick()
            return
        }

        // at this point, we need to obtain user consent to actually do the connection test...
        showUserConsentUI()
    }

    private fun showUserConsentUI() {
        with(mBinding.btnAbortTest) {
            visibility = View.VISIBLE
            setOnClickListener { dismiss() }
        }
        with(mBinding.btnStartTestWithConsent) {
            visibility = View.VISIBLE
            setOnClickListener {
                showUserConsentUI()
                doQualityTestRequiringConsent()
            }
        }

        // if there's a tor connection over a bridge, explain we have to shut tor off
        if (isOrbotOnOrStarting()) {
            mBinding.tvTestingDisconnectVpnDisclaimer.visibility = View.VISIBLE
            mBinding.tvDisclaimerConnectionLeak.visibility = View.VISIBLE
        }
    }

    private fun isOrbotOnOrStarting(): Boolean {
        val torConnectionState = torConnectedViewModel.uiState.value
        return torConnectionState is ConnectUiState.On || torConnectionState is ConnectUiState.Starting
    }


    /* set UI for when the connecting directly to tor test is underway */
    private fun showOngoingTestWithConsentUi() {
        mBinding.progress.visibility = View.VISIBLE
        mBinding.tvTestingConsentTorDisclaimer.visibility = View.GONE
        mBinding.tvDisclaimerConnectionLeak.visibility = View.GONE
        mBinding.tvTestingHeader.text = getString(R.string.testing_explanation_testing)
        mBinding.btnAbortTest.visibility = View.GONE
        mBinding.btnStartTestWithConsent.visibility = View.GONE
        mBinding.tvTitleTesting.text = getString(R.string.testing_title_testing)
        mBinding.tvTestingDisconnectVpnDisclaimer.visibility = View.GONE
    }

    /** This part of the connection test requires the user's consent, since it involves attempting
     * a direct tor connection that censors can trivially detect, and possibly also temporarily
     * disabling Orbot VPN if there's an active connection with censorship circumvention tech.
     */
    private fun doQualityTestRequiringConsent() {
        showOngoingTestWithConsentUi()
        lifecycleScope.launch {
            if (isOrbotOnOrStarting()) {
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
            val status = intent?.getStringExtra(TorService.EXTRA_STATUS)
            Log.wtf(TAG, "Got tor status from testing service: $status")
            if (status == TorService.STATUS_ON) {
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
        // restore device rotation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
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

    fun showTestFailedUi(
        errorExplanation: String? = null,
        bubbleMsg: String? = null,
        bubbleAction: View.OnClickListener = {}
    ) {
        Prefs.snowflakeNeedsQualityCheck = true
        mBinding.boxTesting.visibility = View.GONE
        mBinding.boxDeclined.visibility = View.VISIBLE
        errorExplanation?.let {
            mBinding.tvExplanationDeclined.text = errorExplanation
        }
        bubbleMsg?.let {
            mBinding.tvErrorBubbleMessage.text = bubbleMsg
            mBinding.tvErrorBubbleMessage.setOnClickListener(bubbleAction)
        }
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

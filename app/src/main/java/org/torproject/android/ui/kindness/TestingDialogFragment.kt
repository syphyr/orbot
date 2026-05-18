package org.torproject.android.ui.kindness

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import org.torproject.android.util.DiskUtils
import org.torproject.android.util.Prefs
import org.torproject.android.util.sendIntentToService
import org.torproject.jni.TorService
import org.torproject.jni.TorService.EXTRA_STATUS
import org.torproject.jni.TorService.STATUS_ON
import org.torproject.jni.TorService.getTorrc
import kotlin.getValue

class TestingDialogFragment : DialogFragment() {

    private lateinit var mBinding: FragmentTestingBinding
    val torConnectedViewModel: ConnectViewModel by activityViewModels()

    private var stoppedNormalTorConnection = false
    private var testServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binding: IBinder?) {
            Log.wtf("bim", "onServiceConnected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.wtf("bim", "onServiceDisconnected")
        }
    }

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

        // TODO: Add actual test:
        // - If connected to Tor without a bridge right now, we're fine: dismiss immediately like `btContinue`.
        // - If last test success timestamp is younger than 1 day, dismiss immediately like `btContinue`.
        // - Else, set bridge to `NONE`, DISABLE proxy, start Tor, wait until success or timeout.
        // - Stop Tor again, after test and restore original settings.
        // - Store timestamp of success in `Prefs`.

        if (!Prefs.snowflakeNeedsQualityCheck) {
            Log.wtf("bim", "we dont need a quality check!")
            mBinding.btContinue.callOnClick()
            return
        }

        val torConnectionState = torConnectedViewModel.uiState.value

        if (torConnectionState == ConnectUiState.On && Prefs.transport == Transport.NONE && Prefs.outboundProxy.first == null) {
            Log.wtf("bim", "there's already a direct tor connection so drop things")
            Prefs.snowflakeNeedsQualityCheck = false
            mBinding.btContinue.callOnClick()
            return
        }

        lifecycleScope.launch {
            if (torConnectionState is ConnectUiState.On || torConnectionState is ConnectUiState.Starting) {
                Log.wtf("bim", "tor is running, we need to turn it off")
                stoppedNormalTorConnection = true
                requireActivity().sendIntentToService(TorService.ACTION_STOP)
                delay(250)
            } else {
                Log.wtf("bim", "tor is not running")
            }

            if (torConnectedViewModel.uiState.value != ConnectUiState.Off) {
                stoppedNormalTorConnection = false // it somehow didn't stop!
                Log.wtf("bim", "tor isn't off yet, TODO cleanup, no need to unregister receiver")
            }

            Log.wtf("bim", "current tor state is ${torConnectedViewModel.uiState.value}")

            Log.wtf("bim", "launching tor service")
            launchTorTestService(requireActivity())

            // setupTestPassUi()
        }
    }

    private fun launchTorTestService(orbotActivity: Activity) {
        // first, write the bare minimum to setup a direct connection to tor
        val minimalTorrc = listOf("RunAsDaemon 1", "AvoidDiskWrites 1").joinToString("\n")
        val torrcFile = getTorrc(orbotActivity)
        DiskUtils.flushTextToFile(torrcFile, minimalTorrc, append = false)

        ContextCompat.registerReceiver(
            orbotActivity,
            torStatusReceiver,
            IntentFilter(TorService.ACTION_STATUS),
            Context.RECEIVER_NOT_EXPORTED
        )

        orbotActivity.bindService( // this initiates a connection immediately
            Intent(orbotActivity, SnowflakeTestTorService::class.java),
            testServiceConnection,
            BIND_AUTO_CREATE
        )

    }

    val torStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(EXTRA_STATUS)
            Log.wtf("bim", "got status $status")
            if (status == STATUS_ON) {
                lifecycleScope.launch {
                    Log.wtf("bim", "setting flag")
                    Prefs.snowflakeNeedsQualityCheck = false
                    val orbotActivity = requireActivity()
                    Log.wtf("bim", "unregistering receiver")
                    orbotActivity.unregisterReceiver(torStatusReceiver)
                    Log.wtf("bim", "killing service")
                    orbotActivity.unbindService(testServiceConnection)
                    setupTestPassUi()
                    if (stoppedNormalTorConnection) {
                        val restartDelay = 1000L
                        Log.wtf("bim", "relaunching orbotservice in $restartDelay")
                        delay(restartDelay)
                        Log.wtf("bim", "restarting...")
                        requireActivity().sendIntentToService(TorService.ACTION_START)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO set connection to null, if its not null unbind it here
    }

    fun showDeclinedState() {

    }

    fun setupTestPassUi() {
        Prefs.snowflakeNeedsQualityCheck = false
        mBinding.boxTesting.visibility = View.GONE
        mBinding.boxApproved.visibility = View.VISIBLE
    }

    companion object {
        const val KEY_RESULT = "kindness_test_result"

        fun show(fragmentManager: FragmentManager) {
            TestingDialogFragment().show(fragmentManager, "TestingFragment")
        }

        // TODO make this into a pref, or somewhere better than a static var
        var TEST_UNDERWAY = false
    }
}

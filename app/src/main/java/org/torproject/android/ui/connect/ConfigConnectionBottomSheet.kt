package org.torproject.android.ui.connect

import IPtProxy.IPtProxy
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import org.torproject.android.R
import org.torproject.android.circumvention.Bridges
import org.torproject.android.circumvention.CircumventionApiManager
import org.torproject.android.circumvention.SettingsRequest
import org.torproject.android.databinding.ConfigConnectionBottomSheetBinding
import org.torproject.android.service.OrbotService
import org.torproject.android.service.util.Prefs
import org.torproject.android.ui.OrbotBottomSheetDialogFragment
import java.io.File
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.util.Locale
import androidx.core.net.toUri
import org.torproject.android.Constants

class ConfigConnectionBottomSheet :
    OrbotBottomSheetDialogFragment(), CompoundButton.OnCheckedChangeListener {

    private var callbacks: ConnectionHelperCallbacks? = null

    private lateinit var binding: ConfigConnectionBottomSheetBinding

    private lateinit var radios: List<RadioButton>
    private lateinit var radioSubtitleMap: Map<CompoundButton, View>
    private lateinit var allSubtitles: List<View>

    companion object {
        fun newInstance(callbacks: ConnectionHelperCallbacks): ConfigConnectionBottomSheet {
            return ConfigConnectionBottomSheet().apply {
                this.callbacks = callbacks
            }
        }

        const val TAG = "ConfigConnectionBttmSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ConfigConnectionBottomSheetBinding.inflate(inflater, container, false)

        radios = arrayListOf(
            binding.rbDirect,
            binding.rbSnowflake,
            binding.rbSnowflakeAmp,
            binding.rbSnowflakeSqs,
            binding.rbTelegram,
            binding.rbRequest,
            binding.rbEmail,
            binding.rbMeek,
            binding.rbCustom
        )
        radioSubtitleMap = mapOf<CompoundButton, View>(
            binding.rbDirect to binding.tvDirectSubtitle,
            binding.rbSnowflake to binding.tvSnowflakeSubtitle,
            binding.rbSnowflakeAmp to binding.tvSnowflakeAmpSubtitle,
            binding.rbSnowflakeSqs to binding.tvSnowflakeSqsSubtitle,
            binding.rbTelegram to binding.tvTelegramSubtitle,
            binding.rbRequest to binding.tvRequestSubtitle,
            binding.rbEmail to binding.tvEmailSubtitle,
            binding.rbMeek to binding.tvMeekSubtitle,
            binding.rbCustom to binding.tvCustomSubtitle
        )
        allSubtitles = arrayListOf(
            binding.tvDirectSubtitle,
            binding.tvSnowflakeSubtitle,
            binding.tvSnowflakeAmpSubtitle,
            binding.tvSnowflakeSqsSubtitle,
            binding.tvTelegramSubtitle,
            binding.tvRequestSubtitle,
            binding.tvEmailSubtitle,
            binding.tvMeekSubtitle,
            binding.tvCustomSubtitle
        )

        binding.btnAskTor.setOnClickListener {
            askTor()
        }

        // setup containers so radio buttons can be checked if labels are clicked on
        binding.directContainer.setOnClickListener { binding.rbDirect.isChecked = true }
        binding.snowflakeContainer.setOnClickListener { binding.rbSnowflake.isChecked = true }
        binding.snowflakeAmpContainer.setOnClickListener { binding.rbSnowflakeAmp.isChecked = true }
        binding.snowflakeSqsContainer.setOnClickListener { binding.rbSnowflakeSqs.isChecked = true }
        binding.telegramContainer.setOnClickListener { binding.rbTelegram.isChecked = true }
        binding.requestContainer.setOnClickListener { binding.rbRequest.isChecked = true }
        binding.emailContainer.setOnClickListener { binding.rbEmail.isChecked = true }
        binding.meekContainer.setOnClickListener { binding.rbMeek.isChecked = true }
        binding.customContainer.setOnClickListener { binding.rbCustom.isChecked = true }
        binding.tvCancel.setOnClickListener { dismiss() }

        binding.rbDirect.setOnCheckedChangeListener(this)
        binding.rbSnowflake.setOnCheckedChangeListener(this)
        binding.rbSnowflakeAmp.setOnCheckedChangeListener(this)
        binding.rbSnowflakeSqs.setOnCheckedChangeListener(this)
        binding.rbTelegram.setOnCheckedChangeListener(this)
        binding.rbRequest.setOnCheckedChangeListener(this)
        binding.rbEmail.setOnCheckedChangeListener(this)
        binding.rbMeek.setOnCheckedChangeListener(this)
        binding.rbCustom.setOnCheckedChangeListener(this)

        binding.tvTelegramSubtitle.text = getString(R.string.bridges_via_telegram_subtitle, "start")

        selectRadioButtonFromPreference()

        binding.btnAction.setOnClickListener {
            if (binding.rbRequest.isChecked) {
                MoatBottomSheet(object : ConnectionHelperCallbacks {
                    override fun tryConnecting() {
                        Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_OBFS4)
                        binding.rbCustom.isChecked = true
                        closeAndConnect()
                    }
                }).show(requireActivity().supportFragmentManager, MoatBottomSheet.TAG)
            } else if (binding.rbDirect.isChecked) {
                Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_DIRECT)
                closeAndConnect()
            } else if (binding.rbSnowflake.isChecked) {
                Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_SNOWFLAKE)
                closeAndConnect()
            } else if (binding.rbSnowflakeAmp.isChecked) {
                Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_SNOWFLAKE_AMP)
                closeAndConnect()
            } else if (binding.rbSnowflakeSqs.isChecked) {
                Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_SNOWFLAKE_SQS)
                closeAndConnect()
            } else if (binding.rbTelegram.isChecked) {
                val i = Intent(Intent.ACTION_VIEW, Constants.telegramBot)
                startActivity(i)
            } else if (binding.rbEmail.isChecked) {
                val i = Intent(Intent.ACTION_SENDTO)
                i.setData("mailto:${Constants.emailRecipient}".toUri())
                i.putExtra(Intent.EXTRA_SUBJECT, Constants.emailSubjectAndBody)
                i.putExtra(Intent.EXTRA_TEXT, Constants.emailSubjectAndBody)

                val pm = activity?.packageManager ?: return@setOnClickListener

                if (i.resolveActivity(pm) != null) {
                    startActivity(i)
                }
            }

            // TODO: Finish Meek Azure support.

            if (binding.rbTelegram.isChecked || binding.rbEmail.isChecked || binding.rbCustom.isChecked) {
                CustomBridgeBottomSheet(object : ConnectionHelperCallbacks {
                    override fun tryConnecting() {
                        Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_OBFS4)
                        closeAndConnect()
                    }
                }).show(requireActivity().supportFragmentManager, CustomBridgeBottomSheet.TAG)
            }
        }

        return binding.root
    }

    private fun closeAndConnect() {
        closeAllSheets()
        callbacks?.tryConnecting()
    }

    private fun selectRadioButtonFromPreference() {
        val pref = Prefs.getTorConnectionPathway()
        if (pref.equals(Prefs.CONNECTION_PATHWAY_OBFS4)) binding.rbCustom.isChecked = true
        if (pref.equals(Prefs.CONNECTION_PATHWAY_SNOWFLAKE)) binding.rbSnowflake.isChecked = true
        if (pref.equals(Prefs.CONNECTION_PATHWAY_SNOWFLAKE_AMP)) binding.rbSnowflakeAmp.isChecked = true
        if (pref.equals(Prefs.CONNECTION_PATHWAY_SNOWFLAKE_SQS)) binding.rbSnowflakeSqs.isChecked = true
        if (pref.equals(Prefs.CONNECTION_PATHWAY_DIRECT)) binding.rbDirect.isChecked = true
    }

    private fun askTor() {

        val dLeft = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_faq)
        binding.btnAskTor.text = getString(R.string.asking)
        binding.btnAskTor.setCompoundDrawablesWithIntrinsicBounds(dLeft, null, null, null)

        val fileCacheDir = File(requireActivity().cacheDir, "pt")
        if (!fileCacheDir.exists()) {
            fileCacheDir.mkdir()
        }

        val proxy = OrbotService.getIptProxyController(context)
        proxy.start(IPtProxy.MeekLite, null)

        val moatUrl = OrbotService.getCdnFront("moat-url")
        val front = OrbotService.getCdnFront("moat-front")

        val pUsername = "url=$moatUrl;front=$front"
        val pPassword = "\u0000"

        val authenticator: Authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(pUsername, pPassword.toCharArray())
            }
        }

        Authenticator.setDefault(authenticator)

        val countryCodeValue: String = getDeviceCountryCode(requireContext())
        CircumventionApiManager(proxy.port(IPtProxy.MeekLite)).getSettings(
            SettingsRequest(
                countryCodeValue
            ), {
                it?.let {
                    val circumventionApiBridges = it.settings
                    if (circumventionApiBridges == null) {
                        binding.rbDirect.isChecked = true
                    } else { // got bridges
                        setPreferenceForSmartConnect(circumventionApiBridges)
                    }

                    proxy.stop(IPtProxy.MeekLite)
                }
            }, {
                Log.wtf(TAG, "Couldn't hit circumvention API... $it")
                if (isVisible) {
                    Toast.makeText(
                        requireContext(),
                        R.string.error_asking_tor_for_bridges,
                        Toast.LENGTH_LONG
                    ).show()
                    proxy.stop(IPtProxy.MeekLite)
                }
            })
    }

    private fun getDeviceCountryCode(context: Context): String {
        var countryCode: String?

        // Try to get country code from TelephonyManager service
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Query first getSimCountryIso()
        countryCode = tm.simCountryIso
        if (countryCode != null && countryCode.length == 2) return countryCode.lowercase(Locale.getDefault())

        countryCode = tm.networkCountryIso
        if (countryCode != null && countryCode.length == 2) return countryCode.lowercase(Locale.getDefault())

        countryCode = context.resources.configuration.locales[0].country

        return if (countryCode != null && countryCode.length == 2) countryCode.lowercase(Locale.getDefault()) else "us"
    }

    private fun setPreferenceForSmartConnect(circumventionApiBridges: List<Bridges?>?) {
        if (isVisible) {
            val dLeft = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_green_check)
            binding.btnAskTor.setCompoundDrawablesWithIntrinsicBounds(dLeft, null, null, null)
        }
        circumventionApiBridges?.let {
            if (it.isEmpty()) {
                if (isVisible) {
                    binding.rbDirect.isChecked = true
                    binding.btnAskTor.text = getString(R.string.connection_direct)
                }
                Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_DIRECT)
                return
            }
            val b = it[0]!!.bridges
            when (b.type) {
                CircumventionApiManager.BRIDGE_TYPE_SNOWFLAKE -> {
                    Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_SNOWFLAKE)
                    if (isVisible) {
                        binding.rbSnowflake.isChecked = true
                        binding.btnAskTor.text = getString(R.string.connection_snowflake)
                    }
                }

                CircumventionApiManager.BRIDGE_TYPE_OBFS4 -> {
                    if (isVisible) {
                        binding.rbCustom.isChecked = true
                        binding.btnAskTor.text = getString(R.string.connection_custom)
                    }
                    var bridgeStrings = ""
                    b.bridge_strings!!.forEach { bridgeString ->
                        bridgeStrings += "$bridgeString\n"
                    }
                    Prefs.setBridgesList(bridgeStrings)
                    Prefs.setTorConnectionPathway(Prefs.CONNECTION_PATHWAY_OBFS4)
                }

                else -> {
                    if (isVisible) {
                        binding.rbDirect.isChecked = true
                    }
                }
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            for (radio in radios) {
                if (radio != buttonView) radio.isChecked = false
            }

            radioSubtitleMap[buttonView]?.let {
                for (subtitle in allSubtitles) {
                    subtitle.visibility = if (subtitle == it) View.VISIBLE else View.GONE
                }
            }
        }

        binding.btnAction.text = when (buttonView) {
            binding.rbTelegram, binding.rbRequest, binding.rbEmail, binding.rbCustom -> getString(R.string.next)
            else -> getString(R.string.connect)
        }
    }
}

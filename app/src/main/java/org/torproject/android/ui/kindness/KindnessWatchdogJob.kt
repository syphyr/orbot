package org.torproject.android.ui.kindness

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import org.torproject.android.Regionalization
import org.torproject.android.util.Prefs
import java.util.concurrent.TimeUnit

/**
 * Brings the Snowflake proxy service back when the system has killed it but the
 * user still wants it running (#1799, #1783). START_STICKY alone is not enough:
 * restarts get throttled and eventually abandoned, and some OEMs never deliver
 * them at all, which is how Kindness Mode ends up off until somebody notices.
 */
class KindnessWatchdogJob : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        if (!Prefs.beSnowflakeProxy) {
            cancel(applicationContext)
            return false
        }
        if (shouldRestart(
                wantsProxy = Prefs.beSnowflakeProxy,
                serviceRunning = SnowflakeProxyService.isRunning,
                regionBlocked = Regionalization.isKindnessModeDisabledForCountry(Prefs.bridgeCountry)
            )
        ) {
            try {
                SnowflakeProxyService.startSnowflakeProxyForegroundService(applicationContext)
            } catch (_: IllegalStateException) {
                // Background foreground-service starts can be denied on API 31+
                // when the app holds no exemption. The next boot, app open, or
                // watchdog run after the user grants one will pick it back up.
            }
        }
        return false
    }

    override fun onStopJob(params: JobParameters?) = false

    companion object {
        private const val JOB_ID = 4817

        fun shouldRestart(wantsProxy: Boolean, serviceRunning: Boolean, regionBlocked: Boolean) =
            wantsProxy && !serviceRunning && !regionBlocked

        fun schedule(context: Context) {
            val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfo =
                JobInfo.Builder(JOB_ID, ComponentName(context, KindnessWatchdogJob::class.java))
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15)).setPersisted(true).build()
            jobScheduler.schedule(jobInfo)
        }

        fun cancel(context: Context) {
            (context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler).cancel(JOB_ID)
        }
    }
}

package org.torproject.android.service.circumvention

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.torproject.android.service.util.Prefs

class ResetSnowflakesServedWeeklyWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        Prefs.resetSnowflakesServedWeekly()
        return Result.success()
    }
}
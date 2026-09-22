/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | سرویس پس‌زمینه همگام‌سازی (WorkManager)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  پس از اتصال مجدد به شبکه، فاکتورهای آفلاین و کاتالوگ را خودکار سینک
 *  می‌کند. فقط روی شبکه موجود اجرا می‌شود تا دیتای موبایل هدر نرود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.atiran.vizitor.R
import ir.atiran.vizitor.data.repository.VizitorRepository
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = VizitorRepository(applicationContext)
        return try {
            val report = repo.syncAll()
            val title = if (report.pushedInvoices > 0)
                "✅ فاکتور شما در آتیران ثبت و تأیید شد"
            else "همگام‌سازی آتیران"
            notifyUser(title, report.summary)
            if (report.errors.isEmpty()) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notifyUser(title: String, text: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel("sync", "همگام‌سازی", NotificationManager.IMPORTANCE_LOW)
            )
        }
        nm.notify(
            1001,
            NotificationCompat.Builder(applicationContext, "sync")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        const val WORK_NAME = "vizitor_periodic_sync"

        /** زمان‌بندی سینک دوره‌ای هر ۱۵ دقیقه (حداقل مجاز WorkManager). */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** سینک فوری (دکمه «همگام‌سازی اکنون» در تنظیمات). */
        fun syncNow(context: Context) {
            val oneTime = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(oneTime)
        }
    }
}

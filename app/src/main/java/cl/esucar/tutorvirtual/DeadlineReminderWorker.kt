package cl.esucar.tutorvirtual

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class DeadlineReminderWorker(
    appContext: android.content.Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val notification = NotificationCompat.Builder(applicationContext, "deadlines")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tutor Virtual ESUCAR")
            .setContentText("Revisa tus hitos y continúa avanzando en tu Trabajo de Título.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(2001, notification)
        return Result.success()
    }
}

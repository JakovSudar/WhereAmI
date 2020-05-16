package com.example.whereiam

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import java.io.File

class Notification {
    private val CHANNEL_ID = "imageNotification"
    private var file: File? = null

    fun displayImageNotification() {
        val intent = Intent(Intent.ACTION_VIEW) //
            .setDataAndType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) FileProvider.getUriForFile(
                    WhereIAmApp.ApplicationContext,
                    "com.example.android.fileprovider",
                    this.file!!
                ) else Uri.fromFile(file),
                "image/*"
            ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val pendingIntent = PendingIntent.getActivity(
            WhereIAmApp.ApplicationContext,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        var notification = NotificationCompat.Builder(WhereIAmApp.ApplicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("New Image!")
            .setContentText("Tap to open")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(WhereIAmApp.ApplicationContext)
            .notify(1001, notification)
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "imageNotif"
            val descriptionText = "Displaying images"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                 WhereIAmApp.ApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun setFile(file: File) {
        this.file = file
    }
}
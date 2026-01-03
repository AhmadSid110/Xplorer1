package com.droidexplorer.websim.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.droidexplorer.websim.MainActivity
import com.droidexplorer.websim.R
import com.droidexplorer.websim.core.ops.FileOperation
import com.droidexplorer.websim.core.ops.FileOperationExecutor
import com.droidexplorer.websim.core.ops.OperationCancellationToken
import com.droidexplorer.websim.core.ops.OperationProgress
import com.droidexplorer.websim.core.ops.OperationResult
import com.droidexplorer.websim.file.FileOperator
import com.droidexplorer.websim.storage.DataStoreSafStore
import com.droidexplorer.websim.storage.SafPermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FileOperationService : Service() {

    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var executor: FileOperationExecutor
    private var cancellationToken: OperationCancellationToken = OperationCancellationToken()
    private var currentJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val safStore = DataStoreSafStore(this)
        val safManager = SafPermissionManager(this, safStore)
        executor = FileOperationExecutor(FileOperator(this, safManager))
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            cancellationToken.cancel()
            return START_NOT_STICKY
        }

        val operation = intent?.getSerializableExtra(EXTRA_OPERATION) as? FileOperation
            ?: return START_NOT_STICKY
        if (currentJob != null) {
            _progressFlow.value = OperationProgress.Completed(
                operation.id,
                OperationResult.Failure("Another operation is already running")
            )
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Starting", true, 0, 0))
        cancellationToken = OperationCancellationToken()

        currentJob = serviceScope.launch {
            executor.execute(operation, cancellationToken).collect { progress ->
                _progressFlow.value = progress
                when (progress) {
                    is OperationProgress.Started -> updateNotification("Starting", true, 0, 0)
                    is OperationProgress.Running -> updateNotification(
                        progress.label ?: "Working",
                        false,
                        progress.current,
                        progress.total
                    )

                    is OperationProgress.Completed -> {
                        val message = when (progress.result) {
                            is OperationResult.Success -> progress.result.message ?: "Completed"
                            is OperationResult.Failure -> progress.result.message
                            OperationResult.Cancelled -> "Cancelled"
                        }
                        updateNotification(message, false, 0, 0)
                        currentJob = null
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        currentJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File operations",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(
        content: String,
        indeterminate: Boolean,
        progress: Long,
        total: Long?
    ) {
        val notification = buildNotification(content, indeterminate, progress, total)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        content: String,
        indeterminate: Boolean,
        progress: Long,
        total: Long?
    ): Notification {
        val cancelIntent = Intent(this, FileOperationService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("File operation")
            .setContentText(content)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent
            )

        if (indeterminate || total == null || total <= 0) {
            builder.setProgress(0, 0, true)
        } else {
            val safeTotal = total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val safeProgress = progress.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            builder.setProgress(safeTotal, safeProgress, false)
        }

        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "file_ops"
        private const val NOTIFICATION_ID = 42
        private const val EXTRA_OPERATION = "extra_operation"
        private const val ACTION_CANCEL = "com.droidexplorer.websim.action.CANCEL_OPERATION"

        private val _progressFlow = MutableStateFlow<OperationProgress?>(null)
        val progressFlow: StateFlow<OperationProgress?> = _progressFlow

        fun observe(): StateFlow<OperationProgress?> = progressFlow

        fun enqueue(context: Context, operation: FileOperation) {
            val intent = Intent(context, FileOperationService::class.java).apply {
                putExtra(EXTRA_OPERATION, operation)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

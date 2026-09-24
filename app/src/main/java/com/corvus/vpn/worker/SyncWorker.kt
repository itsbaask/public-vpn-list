package com.corvus.vpn.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.corvus.vpn.data.ServerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val serverRepository: ServerRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val success = serverRepository.syncServers()
        return if (success) Result.success() else Result.retry()
    }
}

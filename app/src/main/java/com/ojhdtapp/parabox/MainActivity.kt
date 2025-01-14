package com.ojhdtapp.parabox

import android.app.NotificationManager
import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.api.services.drive.DriveScopes
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.entityextraction.*
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.smartreply.*
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.data.remote.dto.filterMissing
import com.ojhdtapp.parabox.data.remote.dto.saveLocalResourcesToCloud
import com.ojhdtapp.parabox.data.remote.dto.server.ServerSendMessageDto
import com.ojhdtapp.parabox.data.remote.dto.toFcmMessageContentList
import com.ojhdtapp.parabox.domain.fcm.FcmApiHelper
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.domain.service.PluginListListener
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.domain.use_case.GetContacts
import com.ojhdtapp.parabox.domain.use_case.GetFiles
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateFile
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.parabox.domain.worker.CleanUpFileWorker
import com.ojhdtapp.parabox.domain.worker.DownloadFileWorker
import com.ojhdtapp.parabox.domain.worker.UploadFileWorker
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.NavGraphs
import com.ojhdtapp.parabox.ui.destinations.GuideWelcomePageDestination
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.FixedInsets
import com.ojhdtapp.parabox.ui.util.LocalFixedInsets
import com.ojhdtapp.parabox.ui.util.WorkingMode
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.NestedNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo
import dagger.hilt.android.AndroidEntryPoint
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.roundToInt


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        var inBackground: Boolean = false
    }

    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var updateFile: UpdateFile

    @Inject
    lateinit var getFiles: GetFiles

    @Inject
    lateinit var notificationUtil: NotificationUtil

    @Inject
    lateinit var getContacts: GetContacts

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var fcmApiHelper: FcmApiHelper

    @Inject
    lateinit var updateMessage: UpdateMessage

    @Inject
    lateinit var onedriveUtil: OnedriveUtil

    var pluginService: PluginService? = null
    private lateinit var pluginServiceConnection: ServiceConnection
    private lateinit var userAvatarPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private var recorder: MediaRecorder? = null
    private var recorderJob: Job? = null
    private var recorderStartTime: Long? = null
    private lateinit var recordPath: String
    private var player: MediaPlayer? = null
    private var playerJob: Job? = null
    private var amplituda: Amplituda? = null

    lateinit var vibrator: Vibrator
    lateinit var backup: RoomBackup

    private lateinit var analytics: FirebaseAnalytics

    // Shared ViewModel
    val mainSharedViewModel by viewModels<MainSharedViewModel>()

    // Backup and Restore
    private lateinit var backupLocationSelector: ActivityResultLauncher<String>
    private lateinit var restoreLocationSelector: ActivityResultLauncher<Array<String>>

    // ML
    private var entityExtractor: EntityExtractor? = null
    private var smartReplyGenerator: SmartReplyGenerator? = null

    private fun openFile(file: File) {
        file.downloadPath?.let {
            val path = java.io.File(
                Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOWNLOADS}/Parabox"),
                it
            )
            FileUtil.openFile(this, path, file.extension)
        }
    }

    private fun downloadFile(file: File, cloudFirst: Boolean = false) {
        var resorted = false
        if (!resorted && file.uri != null) {
            try {
                val path = FileUtil.getAvailableFileName(baseContext, file.name)
                FileUtil.saveFileToExternalStorage(baseContext, file.uri, path)
                lifecycleScope.launch(Dispatchers.IO) {
                    updateFile.downloadInfo(path, null, file)
                    updateFile.downloadState(DownloadingState.Done, file)
                }
                Toast.makeText(
                    baseContext,
                    getString(R.string.download_file_success),
                    Toast.LENGTH_SHORT
                ).show()
                resorted = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            if (!resorted && cloudFirst) {
                when (file.cloudType) {
                    GoogleDriveUtil.SERVICE_CODE -> {
                        if (file.cloudId != null) {
                            updateFile.downloadState(DownloadingState.Downloading(0, 0), file)
                            val fileName = FileUtil.getAvailableFileName(baseContext, file.name)
                            GoogleDriveUtil.downloadFile(
                                context = baseContext,
                                fileId = file.cloudId,
                                path = java.io.File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    "Parabox"
                                ),
                                onProgress = { downloadedBytes, allBytes ->
                                    if (allBytes != 0L) {
                                        if (downloadedBytes == allBytes) {
                                            updateFile.downloadInfo(fileName, null, file)
                                            updateFile.downloadState(
                                                DownloadingState.Done,
                                                file
                                            )
                                        } else {
                                            updateFile.downloadState(
                                                DownloadingState.Downloading(
                                                    downloadedBytes = downloadedBytes.toInt(),
                                                    totalBytes = allBytes.toInt()
                                                ), file
                                            )
                                        }

                                    }
                                },
                                fileName = fileName
                            )
                            resorted = true
                        } else {
                            Toast.makeText(
                                baseContext,
                                getString(R.string.invalid_cloud_storage_configuration),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    OnedriveUtil.SERVICE_CODE -> {
                        if (file.cloudId != null && file.url != null) {
                            val url = file.url
                            val path = FileUtil.getAvailableFileName(baseContext, file.name)
                            DownloadManagerUtil.downloadWithManager(
                                baseContext,
                                url,
                                path
                            )?.also {
                                resorted = true
                                updateFile.downloadInfo(path, it, file)
                                repeatOnLifecycle(Lifecycle.State.STARTED) {
                                    DownloadManagerUtil.retrieve(baseContext, it)
                                        .collectLatest {
                                            if (it is DownloadingState.Done) {
                                                updateFile.downloadInfo(path, null, file)
                                            }
                                            updateFile.downloadState(it, file)
                                        }
                                }
                            }
                        }
                    }

                    FcmConstants.CloudStorage.TENCENT_COS.ordinal -> {
                        val secretId =
                            dataStore.data.first()[DataStoreKeys.TENCENT_COS_SECRET_ID]
                        val secretKey =
                            dataStore.data.first()[DataStoreKeys.TENCENT_COS_SECRET_KEY]
                        val bucket =
                            dataStore.data.first()[DataStoreKeys.TENCENT_COS_BUCKET]
                        val region =
                            dataStore.data.first()[DataStoreKeys.TENCENT_COS_REGION]
                        if (secretId != null && secretKey != null && bucket != null && region != null && file.cloudId != null) {
                            val res = TencentCOSUtil.downloadFile(
                                baseContext,
                                secretId,
                                secretKey,
                                region,
                                bucket,
                                file.cloudId,
                                java.io.File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    "Parabox"
                                ).absolutePath,
                                file.name
                            ) { downloadedBytes, allBytes ->
                                if (allBytes != 0L) {
                                    if (downloadedBytes == allBytes) {
                                        updateFile.downloadInfo(file.name, null, file)
                                        updateFile.downloadState(
                                            DownloadingState.Done,
                                            file
                                        )
                                    } else {
                                        updateFile.downloadState(
                                            DownloadingState.Downloading(
                                                downloadedBytes = downloadedBytes.toInt(),
                                                totalBytes = allBytes.toInt()
                                            ), file
                                        )
                                    }
                                }
                            }
                            if (res) {
                                resorted = true
                            }
                        } else {
                            Toast.makeText(
                                baseContext,
                                getString(R.string.invalid_cloud_storage_configuration),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    FcmConstants.CloudStorage.QINIU_KODO.ordinal -> {
                        val accessKey =
                            dataStore.data.first()[DataStoreKeys.QINIU_KODO_ACCESS_KEY]
                        val secretKey =
                            dataStore.data.first()[DataStoreKeys.QINIU_KODO_SECRET_KEY]
                        val bucket =
                            dataStore.data.first()[DataStoreKeys.QINIU_KODO_BUCKET]
                        val domain =
                            dataStore.data.first()[DataStoreKeys.QINIU_KODO_DOMAIN]
                        if (accessKey != null && secretKey != null && bucket != null && domain != null && file.cloudId != null) {
                            val path = FileUtil.getAvailableFileName(baseContext, file.name)
                            QiniuKODOUtil.downloadFile(domain, accessKey, secretKey, file.cloudId)
                                ?.let { newUrl ->
                                    DownloadManagerUtil.downloadWithManager(
                                        baseContext,
                                        newUrl,
                                        path
                                    )?.also {
                                        resorted = true
                                        updateFile.downloadInfo(path, it, file)
                                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                                            DownloadManagerUtil.retrieve(baseContext, it)
                                                .collectLatest {
                                                    if (it is DownloadingState.Done) {
                                                        updateFile.downloadInfo(path, null, file)
                                                    }
                                                    updateFile.downloadState(it, file)
                                                }
                                        }
                                    }
                                }
                        } else {
                            Toast.makeText(
                                baseContext,
                                getString(R.string.invalid_cloud_storage_configuration),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    else -> {

                    }
                }
            }
            if (!resorted) {
                val url = file.url
                if (url != null) {
                    val path = FileUtil.getAvailableFileName(baseContext, file.name)
                    DownloadManagerUtil.downloadWithManager(
                        baseContext,
                        url,
                        path
                    )?.also {
                        resorted = true
                        updateFile.downloadInfo(path, it, file)
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            DownloadManagerUtil.retrieve(baseContext, it)
                                .collectLatest {
                                    if (it is DownloadingState.Done) {
                                        updateFile.downloadInfo(path, null, file)
                                    }
                                    updateFile.downloadState(it, file)
                                }
                        }
                    }
                }
            }
            if (!resorted) {
                updateFile.downloadInfo(null, null, file)
                updateFile.downloadState(DownloadingState.Failure, file)
                Toast.makeText(
                    baseContext,
                    getString(R.string.download_file_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun retrieveDownloadProcess(file: File) {
        file.downloadId?.let { id ->
            lifecycleScope.launch(Dispatchers.IO) {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    DownloadManagerUtil.retrieve(this@MainActivity, id).collectLatest {
                        updateFile.downloadState(it, file)
                    }
                }
            }
        }
    }

    private fun startPlayingLocal(uri: Uri) {
        stopPlaying()
        player = MediaPlayer().apply {
            try {
                setDataSource(baseContext, uri)
                setOnPreparedListener {
                    playerJob = lifecycleScope.launch {
                        while (true) {
                            val progress = (currentPosition.toFloat() / duration)
                            mainSharedViewModel.setAudioPlayerProgressFraction(progress)
                            delay(30)
                        }
                    }
                    amplituda = Amplituda(baseContext).also { amplituda ->
                        amplituda.processAudio(
                            FileUtil.uriToTempFile(baseContext, uri),
                            Compress.withParams(Compress.AVERAGE, 2)
                        ).get(
                            { result ->
                                mainSharedViewModel.insertAllIntoRecordAmplitudeStateList(
                                    result.amplitudesAsList().map { it * 1000 })
                            }, { exception ->
                                exception.printStackTrace()
                            })
                    }
                    mainSharedViewModel.setIsAudioPlaying(true)
                }
                setOnCompletionListener {
                    amplituda?.clearCache()
                    amplituda = null
                    playerJob?.cancel()
                    playerJob = null
                    mainSharedViewModel.clearRecordAmplitudeStateList()
                    mainSharedViewModel.setIsAudioPlaying(false)
                }
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }


//        player?.let {
//            Visualizer(it.audioSessionId).apply {
//                captureSize = Visualizer.getCaptureSizeRange()[1]
//                setDataCaptureListener(object: Visualizer.OnDataCaptureListener{
//                    override fun onWaveFormDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {
//                        val amplitude = p1?.let { it1 -> calculateRMSLevel(it1) } ?: 0
//                        Log.d("parabox", "WaveFromData:$amplitude")
//                    }
//                    override fun onFftDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {
//
//                    }
//                },Visualizer.getMaxCaptureRate() / 2, true, false)
//                enabled = true
//            }
//        }
    }

    private fun startPlayingInternet(url: String) {
        stopPlaying()
        player = MediaPlayer().apply {
            try {
                setDataSource(url)
                setOnPreparedListener {
                    playerJob = lifecycleScope.launch(Dispatchers.IO) {
                        while (true) {
                            val progress = (currentPosition.toFloat() / duration)
                            mainSharedViewModel.setAudioPlayerProgressFraction(progress)
                            delay(30)
                        }
                    }
                    amplituda = Amplituda(baseContext).also { amplituda ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            amplituda.processAudio(
                                url,
                                Compress.withParams(Compress.AVERAGE, 2)
                            ).get(
                                { result ->
                                    mainSharedViewModel.insertAllIntoRecordAmplitudeStateList(
                                        result.amplitudesAsList().map { it * 1000 })
                                }, { exception ->
                                    exception.printStackTrace()
                                })
                        }
                    }
                    mainSharedViewModel.setIsAudioPlaying(true)
                    start()
                }
                setOnCompletionListener {
                    amplituda?.clearCache()
                    amplituda = null
                    playerJob?.cancel()
                    playerJob = null
                    mainSharedViewModel.clearRecordAmplitudeStateList()
                    mainSharedViewModel.setIsAudioPlaying(false)
                }
                prepareAsync()
            } catch (e: IOException) {
                Log.e("parabox", "prepare() failed")
            }
        }

    }

//    fun calculateRMSLevel(audioData: ByteArray): Int {
//        var amplitude = 0.0
//        for (i in audioData.indices) {
//            amplitude += Math.abs((audioData[i] / 32768.0))
//        }
//        amplitude /= audioData.size
//
//        return amplitude.toInt()
//    }

    fun calculateRMSLevel(audioData: ByteArray): Double {
        var amplitude = 0.0
        for (i in 0 until (audioData.size / 2)) {
            val y = (audioData[i * 2].toInt() or (audioData[i * 2 + 1].toInt() shl 8)) / 32768.0
            amplitude += abs(y)
        }
        amplitude = amplitude / audioData.size / 2
        return amplitude
    }

    private fun stopPlaying() {
        playerJob?.cancel()
        playerJob = null
        player?.release()
        player = null
        mainSharedViewModel.setIsAudioPlaying(false)
    }

    private fun pausePlaying() {
        if (player?.isPlaying == true) {
            player?.pause()
            mainSharedViewModel.setIsAudioPlaying(false)
        }
    }

    private fun resumePlaying() {
        if (player?.isPlaying == false) {
            player?.start()
            mainSharedViewModel.setIsAudioPlaying(true)
        }
    }

    private fun setProgress(fraction: Float) {
        player?.run {
            seekTo((duration * fraction).roundToInt())
        }
    }

    private fun startRecording() {
        recorderJob?.cancel()
        if (recorderJob != null)
            recorderJob = null
        mainSharedViewModel.clearRecordAmplitudeStateList()
        recorderStartTime = System.currentTimeMillis()
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(recordPath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e("parabox", "prepare() failed")
            }
            start()
        }
        recorderJob = lifecycleScope.launch {
            while (true) {
                val value = recorder?.maxAmplitude ?: 0
                Log.d("parabox", "$value")
                mainSharedViewModel.insertIntoRecordAmplitudeStateList(value)
                delay(500)
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            if (abs(
                    (recorderStartTime ?: System.currentTimeMillis()) - System.currentTimeMillis()
                ) < 300
            ) {
                delay(300)
            }
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorderJob?.cancel()
            if (recorderJob != null)
                recorderJob = null
            recorder = null
            recorderStartTime = null
        }
    }

    private fun pickUserAvatar() {
        userAvatarPickerLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun setUserAvatar(uri: Uri) {
//        getExternalFilesDir("avatar")?.listFiles()?.filter { it.name == "Avatar_parabox-user.jpg" }?.map {
//            it.delete()
//        }
        val path = getExternalFilesDir("avatar")!!
        val copiedUri = FileUtil.getUriByCopyingFileToPath(
            this,
            path,
            "Avatar_${System.currentTimeMillis().toDateAndTimeString()}.png",
            uri
        )
//        val outputFile =
//            File("${getExternalFilesDir("avatar")}${File.separator}AVATAR_$timeStr.jpg")
//        contentResolver.openInputStream(uri)?.use { inputStream ->
//            FileOutputStream(outputFile).use { outputStream ->
//                inputStream.copyTo(outputStream)
//            }
//        }
//        val copiedUri = FileProvider.getUriForFile(
//            this,
//            BuildConfig.APPLICATION_ID + ".provider", outputFile
//        )
        copiedUri?.let {
            lifecycleScope.launch {
                this@MainActivity.dataStore.edit { settings ->
                    settings[DataStoreKeys.USER_AVATAR] = it.toString()
                }
                Toast.makeText(
                    baseContext,
                    getString(R.string.avatar_updated),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 20))
        }
    }

    private fun refreshMessage() {
        mainSharedViewModel.setIsRefreshing(true)
        lifecycleScope.launch {
            val workingMode = dataStore.data.map { preferences ->
                preferences[DataStoreKeys.SETTINGS_WORKING_MODE] ?: WorkingMode.NORMAL.ordinal
            }.first()
//            val fcmRole = dataStore.data.map { preferences ->
//                preferences[DataStoreKeys.SETTINGS_FCM_ROLE] ?: FcmConstants.Role.SENDER.ordinal
//            }.first()
            when (workingMode) {
                WorkingMode.NORMAL.ordinal -> {
                    if (pluginService?.refreshMessage() == true) {
                        delay(500)
                        mainSharedViewModel.setIsRefreshing(false)
                    } else {
                        delay(500)
                        mainSharedViewModel.setIsRefreshing(false)
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.extension_disconnected),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }

                WorkingMode.RECEIVER.ordinal -> {
                    // only check fcm connection... for lazy
                    fcmApiHelper.getVersion().also {
                        if (it?.isSuccessful != true) {
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.fcm_disconnected),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        mainSharedViewModel.setIsRefreshing(false)
                    }
                }

                WorkingMode.FCM.ordinal -> {
                    // check google server connection
                    delay(500)
                    mainSharedViewModel.setIsRefreshing(false)
                }
            }
        }
    }

    private fun backupDatabase() {
        backup
            .database(appDatabase)
            .enableLogDebug(true)
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_EXTERNAL)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    if (success) {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.backup_text),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        backupLocationSelector.launch(
                            "Backup_${
                                System.currentTimeMillis().toDateAndTimeString()
                            }.sqlite3"
                        )
                    } else {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.backup_failed, exitCode),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .backup()
    }

    private fun restoreDatabase(file: java.io.File) {
        backup
            .database(appDatabase)
            .enableLogDebug(true)
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
            .backupLocationCustomFile(file)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    if (success) {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.restore_success),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        file.delete()
                        lifecycleScope.launch {
                            delay(1000)
                            onEvent(ActivityEvent.RestartApp)
                        }
                    } else {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.restore_failed, exitCode),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .restore()
    }

    private fun startPluginConnection() {
        startPluginConnectionService()
    }

    private fun resetPluginConnection() {
        pluginService?.also {
            it.reset()
            Toast.makeText(
                this,
                getString(R.string.reset_extension_connection_success),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun stopPluginConnection() {
        pluginService?.also {
            it.stop()
            unbindService(pluginServiceConnection)
        }
        stopService(Intent(this, PluginService::class.java))
        mainSharedViewModel.setPluginListStateFlow(emptyList())
    }

    private fun deleteChatFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (dataStore.data.first()[DataStoreKeys.SETTINGS_AUTO_DELETE_LOCAL_RESOURCE] == true) {
                dataStore.data.first()[DataStoreKeys.SETTINGS_AUTO_DELETE_LOCAL_RESOURCE_BEFORE_DAYS]?.let {
                    CacheUtil.deleteChatFilesBeforeTimestamp(
                        baseContext,
                        System.currentTimeMillis() - it.roundToInt() * 24 * 60 * 60 * 1000
                    )
                }
            }
        }
    }

    fun backupFileToCloudService() {
        val workManager = WorkManager.getInstance(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val enableAutoBackup =
                dataStore.data.first()[DataStoreKeys.SETTINGS_AUTO_BACKUP] ?: false
            val defaultBackupService =
                dataStore.data.first()[DataStoreKeys.SETTINGS_CLOUD_SERVICE] ?: 0
            val autoBackupFileMaxSize =
                (dataStore.data.first()[DataStoreKeys.SETTINGS_AUTO_BACKUP_FILE_MAX_SIZE]
                    ?: 10f).let {
                    if (it == 100f) Long.MAX_VALUE
                    else it.toLong() * 1024 * 1024
                }
            if (enableAutoBackup && defaultBackupService != 0) {
                val files = getContacts.shouldBackup().map { it.contactId }.let {
                    getFiles.byContactIdsStatic(it).filter { it.size < autoBackupFileMaxSize && it.cloudType == 0 }
                }
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
//                    .setRequiresDeviceIdle(true)
                    .build()
                files.forEach {
                    val tag = it.fileId.toString()
                    val downloadRequest = OneTimeWorkRequestBuilder<DownloadFileWorker>()
                        .setConstraints(constraints)
                        .addTag(tag)
                        .setInputData(
                            workDataOf(
                                "url" to it.url,
                                "name" to it.name,
                            )
                        )
                        .build()
                    val uploadRequest = OneTimeWorkRequestBuilder<UploadFileWorker>()
                        .setConstraints(constraints)
                        .addTag(tag)
                        .setInputData(
                            workDataOf(
                                "default_backup_service" to defaultBackupService,
                            )
                        )
                        .build()
                    val cleanUpRequest = OneTimeWorkRequestBuilder<CleanUpFileWorker>()
                        .setConstraints(constraints)
                        .addTag(tag)
                        .setInputData(
                            workDataOf(
                                "fileId" to it.fileId,
                            )
                        )
                        .build()
                    val continuation = workManager.beginUniqueWork(
                        tag,
                        ExistingWorkPolicy.KEEP,
                        downloadRequest
                    )
                        .then(uploadRequest)
                        .then(cleanUpRequest)
                    continuation.enqueue()
                    launch(Dispatchers.Main) {
                        files.forEach {
                            workManager.getWorkInfosByTagLiveData(tag)
                                .observe(this@MainActivity) { workInfoList ->
                                    mainSharedViewModel.putWorkInfo(tag, it, workInfoList)
                                }
                        }
//                        continuation.workInfosLiveData.observe(this@MainActivity) { workInfoList ->
//                            mainSharedViewModel.putWorkInfo(it, workInfoList)
//                        }
                    }
                }
            }
        }
    }

    fun backupFileToCloudService(file: File) {
        val workManager = WorkManager.getInstance(this)

        lifecycleScope.launch(Dispatchers.IO) {
            updateFile.cloudInfo(null, null, file.fileId)
            val defaultBackupService =
                dataStore.data.first()[DataStoreKeys.SETTINGS_CLOUD_SERVICE] ?: 0
            if (defaultBackupService != 0) {
                val tag = file.fileId.toString()
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
                val downloadRequest = OneTimeWorkRequestBuilder<DownloadFileWorker>()
                    .setConstraints(constraints)
                    .addTag(tag)
                    .setInputData(
                        workDataOf(
                            "url" to file.url,
                            "name" to file.name,
                        )
                    )
                    .build()
                val uploadRequest = OneTimeWorkRequestBuilder<UploadFileWorker>()
                    .setConstraints(constraints)
                    .addTag(tag)
                    .setInputData(
                        workDataOf(
                            "default_backup_service" to defaultBackupService,
                        )
                    )
                    .build()
                val cleanUpRequest = OneTimeWorkRequestBuilder<CleanUpFileWorker>()
                    .setConstraints(constraints)
                    .addTag(tag)
                    .setInputData(
                        workDataOf(
                            "fileId" to file.fileId,
                        )
                    )
                    .build()
                val continuation = workManager.beginUniqueWork(
                    tag,
                    ExistingWorkPolicy.KEEP,
                    downloadRequest
                )
                    .then(uploadRequest)
                    .then(cleanUpRequest)
                continuation.enqueue()
                launch(Dispatchers.Main) {
                    continuation.workInfosLiveData.observe(this@MainActivity) { workInfoList ->
                        mainSharedViewModel.putWorkInfo(tag, file, workInfoList)
                    }
                }
            }
        }
    }

    fun cancelBackupWorkByTag(tag: String) {
        val workManager = WorkManager.getInstance(this)
        workManager.cancelAllWorkByTag(tag)
    }

    private fun firebaseAppCheck() {
        FirebaseApp.initializeApp(/*context=*/this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }

    private fun queryFCMToken() {
        lifecycleScope.launch {
            if (dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM] == true) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("parabox", "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    Log.d("parabox", "FCM token: $token")
                    lifecycleScope.launch {
                        dataStore.edit { settings ->
                            settings[DataStoreKeys.FCM_TOKEN] = token
                        }
                    }
                })
            }
        }
    }

    private fun queryConfigFromFireStore() {
        val db = Firebase.firestore
        db.collection("config").get().addOnSuccessListener { result ->
            if (result != null) {
                val config = result.documents.firstOrNull()?.data
                val fcm_url = config?.get("fcm_url")?.toString()
                Log.d("parabox", "fcm_url: $fcm_url")
                fcm_url?.let {
                    lifecycleScope.launch {
                        dataStore.edit { settings ->
                            settings[DataStoreKeys.SETTINGS_FCM_OFFICIAL_URL] = it
                        }
                    }
                }
            } else {
                Log.d("parabox", "No such document")
            }
        }.addOnFailureListener { exception ->
            Log.d("parabox", "get failed with ", exception)
        }
    }

    private fun initializeMLKit() {
        lifecycleScope.launch {
            val isEntityExtractionEnabled =
                dataStore.data.first()[DataStoreKeys.SETTINGS_ML_KIT_ENTITY_EXTRACTION] ?: true
            val isSmartReplyEnabled =
                dataStore.data.first()[DataStoreKeys.SETTINGS_ML_KIT_SMART_REPLY] ?: true
            val isTranslationEnabled =
                dataStore.data.first()[DataStoreKeys.SETTINGS_ML_KIT_TRANSLATION] ?: true
            if (isEntityExtractionEnabled) {
                val tempEntityExtractor =
                    EntityExtraction.getClient(
                        EntityExtractorOptions.Builder(
                            AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()?.let {
                                EntityExtractorOptions.fromLanguageTag(it)
                            } ?: EntityExtractorOptions.ENGLISH
                        ).build())
                tempEntityExtractor
                    .downloadModelIfNeeded()
                    .addOnSuccessListener { _ ->
                        entityExtractor = tempEntityExtractor
                        lifecycle.addObserver(entityExtractor!!)
                    }
            }
            if (isSmartReplyEnabled) {
                smartReplyGenerator = SmartReply.getClient()
            }
        }
    }

    private fun refreshCloudStorageFileList(withDelay: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (withDelay) {
                delay(5000)
            }
            val cloudService = mainSharedViewModel.cloudServiceFlow.first()
            when (cloudService) {
                GoogleDriveUtil.SERVICE_CODE -> {
                    dataStore.data.first().get(DataStoreKeys.GOOGLE_WORK_FOLDER_ID)?.let {
                        GoogleDriveUtil.getFileList(baseContext, it)?.map {
                            File(
                                url = it.webContentLink,
                                uri = null,
                                name = it.name,
                                extension = it.fullFileExtension ?: FileUtil.getExtension(it.name),
                                size = it.getSize(),
                                timestamp = it.createdTime.value,
                                profileName = getString(R.string.cloud_service_gd),
                                fileId = "${GoogleDriveUtil.SERVICE_CODE}${
                                    it.id.getAscllString().subSequence(0, 10)
                                }".toLong(),
                                cloudType = GoogleDriveUtil.SERVICE_CODE,
                                cloudId = it.id
                            )
                        }?.also {
                            if (it.isNotEmpty()) {
                                appDatabase.fileDao.insertFiles(
                                    it.map { it.toFileEntity() }
                                )
                            }
                        }
                    }
                }
                OnedriveUtil.SERVICE_CODE -> {
                    Log.d("DRIVE", "Query file from onedrive")
                    onedriveUtil.getFileList()?.map {
                        File(
                            url = it.downloadUrl,
                            uri = null,
                            name = it.name,
                            extension = FileUtil.getExtension(it.name),
                            size = it.size,
                            timestamp = it.createdDateTime.toTimestamp(),
                            profileName = getString(R.string.cloud_service_od),
                            fileId = "${OnedriveUtil.SERVICE_CODE}${
                                it.id.getAscllString().let {
                                    it.subSequence(it.length - 10, it.length)
                                }
                            }".toLong(),
                            cloudType = OnedriveUtil.SERVICE_CODE,
                            cloudId = it.id
                        )
                    }?.also {
                        Log.d("DRIVE", "Query file from onedrive: ${it}")
                        if (it.isNotEmpty()) {
                            appDatabase.fileDao.insertFiles(
                                it.map { it.toFileEntity() }
                            )
                        }
                    }
                }
                else -> {

                }
            }
        }
    }

    private fun collectDarkModeFlow() {
        lifecycleScope.launch {
            dataStore.data.collectLatest {
                val darkMode = it[DataStoreKeys.SETTINGS_DARK_MODE]
                if (BuildConfig.VERSION_CODE >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                    when (darkMode) {
                        DataStoreKeys.DARK_MODE.YES.ordinal -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_YES
                        }
                        DataStoreKeys.DARK_MODE.NO.ordinal -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_NO
                        }
                        else -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_AUTO
                        }
                    }
                } else {
                    when (darkMode) {
                        DataStoreKeys.DARK_MODE.YES.ordinal -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                        DataStoreKeys.DARK_MODE.NO.ordinal -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }
                        else -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    }
                    delegate.applyDayNight()
                }

            }
        }
    }

    fun getGoogleLoginAuth(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(DriveScopes.DRIVE_METADATA),
                Scope(DriveScopes.DRIVE_APPDATA),
                Scope(DriveScopes.DRIVE_FILE),
            )
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    fun getDriveInformation() {
        lifecycleScope.launch {
            try {
                delay(5000)
                val cloudStorage = dataStore.data.first()[DataStoreKeys.SETTINGS_CLOUD_SERVICE] ?: 0
                when (cloudStorage) {
                    GoogleDriveUtil.SERVICE_CODE -> {
                        GoogleDriveUtil.getDriveInformation(baseContext)?.also {
                            baseContext.dataStore.edit { preferences ->
                                preferences[DataStoreKeys.GOOGLE_WORK_FOLDER_ID] = it.workFolderId
                                preferences[DataStoreKeys.CLOUD_TOTAL_SPACE] = it.totalSpace
                                preferences[DataStoreKeys.CLOUD_USED_SPACE] = it.usedSpace
                                preferences[DataStoreKeys.CLOUD_APP_USED_SPACE] = it.appUsedSpace
                            }
                        }
                    }
                    OnedriveUtil.SERVICE_CODE -> {
                        onedriveUtil.getDrive()?.also {
                            onedriveUtil.getAppFolder()?.also {
                                baseContext.dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.CLOUD_APP_USED_SPACE] = it.size
                                }
                            }
                            baseContext.dataStore.edit { preferences ->
                                preferences[DataStoreKeys.CLOUD_TOTAL_SPACE] = it.quota.total
                                preferences[DataStoreKeys.CLOUD_USED_SPACE] = it.quota.used
                            }
                        }
//                        onedriveUtil.getDriveList()?.firstOrNull()?.also {
//                            Log.d("Drive", "driveItem: $it")
//                            baseContext.dataStore.edit { preferences ->
//                                preferences[DataStoreKeys.CLOUD_TOTAL_SPACE] = it.quota.total
//                                preferences[DataStoreKeys.CLOUD_USED_SPACE] = it.quota.used
//                                preferences[DataStoreKeys.CLOUD_APP_USED_SPACE] = 0L
//                            }
//                        }
//                        val response = onedriveUtil.getRootList()
//                        Log.d("Drive", "getRoot: $response")
                    }
                    else -> {

                    }
                }
            } catch (e: Exception) {
                Log.d("Drive", "DriveError: ${e.message}")
            }
        }
    }

    // ML
    suspend fun getEntityAnnotationList(str: String): List<EntityAnnotation> {
        return suspendCoroutine<List<EntityAnnotation>> { cot ->
            Log.d("parabox", "getEntityAnnotationList: $str")
            if (entityExtractor == null) cot.resume(emptyList<EntityAnnotation>())
            else {
                val params = EntityExtractionParams.Builder(str).build()
                entityExtractor!!.annotate(params)
                    .addOnSuccessListener { result ->
                        cot.resume(result)
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        cot.resumeWithException(it)
                    }
            }
        }
    }

    suspend fun getSmartReplyList(contactId: Long): List<SmartReplySuggestion> {
        Log.d("parabox", "getSmartReplyList: $contactId")
        if (smartReplyGenerator == null) return emptyList()
        val conversation = withContext(Dispatchers.IO) {
            appDatabase.messageDao.getMessagesWithLimit(listOf(contactId), 3)
                .sortedBy { it.timestamp }.map {
                    if (it.sentByMe) {
                        TextMessage.createForLocalUser(it.contentString, it.timestamp)
                    } else {
                        TextMessage.createForRemoteUser(
                            it.contentString,
                            it.timestamp,
                            it.profile.name.ifBlank { "name" }
                        )
                    }
                }
        }
        return suspendCoroutine<List<SmartReplySuggestion>> { cot ->
            Log.d("parabox", "getSmartReplyList: ${conversation.last().messageText}")
            smartReplyGenerator!!.suggestReplies(conversation)
                .addOnSuccessListener { result ->
                    if (result.status == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                        cot.resume(emptyList())
                    } else if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                        if (conversation.lastOrNull()?.isLocalUser == true) {
                            cot.resume(emptyList())
                        } else {
                            cot.resume(result.suggestions)
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    cot.resumeWithException(it)
                }
        }
    }

    suspend fun getTranslation(originalText: String): String? {
        return try {
            val languageCode = getLanguageCode(originalText).substringBefore("-")
            val currentLanguageTag =
                AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()?.substringBefore("-")
                    ?: "en"
            Log.d("parabox", "getTranslation: $languageCode -> $currentLanguageTag")
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(languageCode)!!)
                .setTargetLanguage(
                    TranslateLanguage.fromLanguageTag(
                        currentLanguageTag
                    )!!
                )
                .build()
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            return suspendCoroutine { cot ->
                val translator = Translation.getClient(options)
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        Log.d("parabox", "downloadModelIfNeeded: success")
                        translator.translate(originalText)
                            .addOnSuccessListener { translatedText ->
                                Log.d("parabox", "translated: $translatedText")
                                cot.resume(translatedText)
                            }
                            .addOnFailureListener {
                                it.printStackTrace()
                                cot.resumeWithException(it)
                            }.addOnCompleteListener {
                                translator.close()
                            }
                    }
                    .addOnFailureListener {
                        Log.d("parabox", "downloadModelIfNeeded: failed")
                        cot.resumeWithException(it)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getLanguageCode(str: String): String {
        return suspendCoroutine<String> { cot ->
            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(str)
                .addOnSuccessListener { languageCode ->
//                    Log.d("parabox", "getLanguageCode: $languageCode")
//                    Log.d("parabox", "selectedLanguageCode: ${AppCompatDelegate.getApplicationLocales()[0]?.language}")
                    if (languageCode == "und") {
                        cot.resume("en")
                    } else {
                        cot.resume(languageCode)
                    }
                }
                .addOnFailureListener {
                    cot.resume("en")
                }
        }
    }

    suspend fun msSignIn(): Boolean {
        return withContext(Dispatchers.IO) {
            val res = suspendCoroutine<Int> { cot ->
                onedriveUtil.signIn(
                    activity = this@MainActivity,
                ) {
                    cot.resume(it)
                }
            }
            if (res == OnedriveUtil.STATUS_SUCCESS) {
                dataStore.edit { preferences ->
                    preferences[DataStoreKeys.SETTINGS_CLOUD_SERVICE] = OnedriveUtil.SERVICE_CODE
                }
                onedriveUtil.getDrive()?.also {
                    onedriveUtil.getAppFolder()?.also {
                        baseContext.dataStore.edit { preferences ->
                            preferences[DataStoreKeys.CLOUD_APP_USED_SPACE] = it.size
                        }
                    }
                    baseContext.dataStore.edit { preferences ->
                        preferences[DataStoreKeys.CLOUD_TOTAL_SPACE] = it.quota.total
                        preferences[DataStoreKeys.CLOUD_USED_SPACE] = it.quota.used
                    }
                }
                true
            } else false
        }
    }

    suspend fun msSignOut(): Boolean {
        return withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_CLOUD_SERVICE] = 0
            }
            suspendCoroutine<Boolean> { cot ->
                onedriveUtil.signOut() {
                    cot.resume(it == OnedriveUtil.STATUS_SUCCESS)
                }
            }
        }
    }

    // Event
    fun onEvent(event: ActivityEvent) {
        when (event) {
            is ActivityEvent.LaunchIntent -> {
                startActivity(event.intent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }

            is ActivityEvent.LaunchURL -> {
                BrowserUtil.launchURL(this, event.url)
            }

            is ActivityEvent.RestartApp -> {
                val ctx = applicationContext
                val pm = ctx.packageManager
                val intent = pm.getLaunchIntentForPackage(ctx.packageName)
                val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
                ctx.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }

            is ActivityEvent.SendMessage -> {
                Log.d("parabox", "sendMessage at ${System.currentTimeMillis()}")
                lifecycleScope.launch(Dispatchers.IO) {
                    val timestamp = System.currentTimeMillis()
                    handleNewMessage(
                        event.contents,
                        event.pluginConnection,
                        timestamp,
                        event.sendType
                    ).also {
                        val dto = SendMessageDto(
                            contents = event.contents,
                            timestamp = timestamp,
                            pluginConnection = event.pluginConnection,
                            messageId = it
                        )
                        val workingMode =
                            dataStore.data.first()[DataStoreKeys.SETTINGS_WORKING_MODE]
                                ?: WorkingMode.NORMAL.ordinal
                        val enableFcm =
                            dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
//                        val fcmRole = dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_ROLE]
//                            ?: FcmConstants.Role.SENDER.ordinal
                        when (workingMode) {
                            WorkingMode.NORMAL.ordinal -> {
                                pluginService?.sendMessage(dto)
                            }
                            WorkingMode.RECEIVER.ordinal -> {
                                if (enableFcm) {
//                                    val fcmCloudStorage =
//                                        dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_CLOUD_STORAGE]
//                                            ?: FcmConstants.CloudStorage.NONE.ordinal
                                    val dtoWithoutUri = dto.copy(
                                        contents = dto.contents.saveLocalResourcesToCloud(
                                            baseContext
                                        ).filterMissing()
                                    )
                                    if (fcmApiHelper.pushSendDto(
                                            dtoWithoutUri
                                        )?.isSuccessful == true
                                    ) {
                                        updateMessage.verifiedState(it, true)
                                    } else {
                                        updateMessage.verifiedState(it, false)
                                    }
                                } else {
                                    // do nothing
                                }
                            }
                            WorkingMode.FCM.ordinal -> {
                                // to Google server
                                if (enableFcm) {
                                    appDatabase.fcmMappingDao.getFcmMappingById(dto.pluginConnection.id)
                                        ?.also { fcmMapping ->
                                            val contents = dto.contents.saveLocalResourcesToCloud(
                                                baseContext
                                            ).filterMissing().toFcmMessageContentList()
                                            val dto = ServerSendMessageDto(
                                                contents = contents,
                                                slaveOriginUid = fcmMapping.uid,
                                                timestamp = dto.timestamp
                                            )
                                            val json = Gson().toJson(dto)
                                            val fm = Firebase.messaging
                                            Log.d("parabox", "message from: ${fcmMapping.from}")
                                            fm.send(
                                                RemoteMessage.Builder("${fcmMapping.from}@fcm.googleapis.com")
                                                    .setMessageId(it.toString())
                                                    .addData("message", json)
                                                    .addData("session_id", fcmMapping.sessionId)
                                                    .build()
                                            )
                                            delay(500)
                                            updateMessage.verifiedState(it, true)
//                                        if (fcmApiHelper.pushSendDto(
//                                            )?.isSuccessful == true
//                                        ) {
//                                            updateMessage.verifiedState(it, true)
//                                        } else {
//                                            updateMessage.verifiedState(it, false)
//                                        }
                                        }

                                }
                            }
                        }
                    }
                }
            }

            is ActivityEvent.RecallMessage -> {
                pluginService?.recallMessage(event.type, event.messageId)
            }

            is ActivityEvent.SetUserAvatar -> {
                pickUserAvatar()
            }

            is ActivityEvent.StartRecording -> {
                if (player?.isPlaying == true) {
                    stopPlaying()
                    Toast.makeText(
                        baseContext,
                        getString(R.string.audio_interrupted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                startRecording()
            }

            is ActivityEvent.StopRecording -> {
                stopRecording()
            }

            is ActivityEvent.StartAudioPlaying -> {
                if (recorder != null) {
                    Toast.makeText(baseContext, "请先结束录音", Toast.LENGTH_SHORT).show()
                } else {
                    if (event.uri != null) {
                        startPlayingLocal(event.uri)
                    } else if (event.url != null) {
                        startPlayingInternet(event.url)
                    } else {
                        Toast.makeText(baseContext, "音频资源丢失", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            is ActivityEvent.StopAudioPlaying -> {
                stopPlaying()
            }

            is ActivityEvent.PauseAudioPlaying -> {
                pausePlaying()
            }

            is ActivityEvent.ResumeAudioPlaying -> {
                resumePlaying()
            }

            is ActivityEvent.SetAudioProgress -> {
                setProgress(event.fraction)
            }

            is ActivityEvent.DownloadFile -> {
                downloadFile(event.file)
            }

            is ActivityEvent.DownloadCloudFile -> {
                downloadFile(event.file, true)
            }

            is ActivityEvent.OpenFile -> {
                openFile(event.file)
            }

            is ActivityEvent.Vibrate -> {
                vibrate()
            }

            is ActivityEvent.RefreshMessage -> {
                refreshMessage()
            }

            is ActivityEvent.ShowInBubble -> {
                lifecycleScope.launch {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && notificationUtil.canBubble(
                            event.contact,
                            event.channelId
                        )
                    ) {
                        notificationUtil.sendNewMessageNotification(
                            event.message,
                            event.contact,
                            event.channelId,
                            true,
                            true
                        )
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "当前会话未启用对话泡",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            is ActivityEvent.Backup -> {
                backupDatabase()
            }

            is ActivityEvent.Restore -> {
                restoreLocationSelector.launch(
                    arrayOf(
                        "application/vnd.sqlite3",
                        "application/x-sqlite3",
                        "application/octet-stream",
                        "application/x-trash",
                    )
                )
            }

            is ActivityEvent.StartExtension -> {
                startPluginConnection()
            }

            is ActivityEvent.ResetExtension -> {
                resetPluginConnection()
            }

            is ActivityEvent.StopExtension -> {
                stopPluginConnection()
            }

            is ActivityEvent.SaveToCloud -> {
                backupFileToCloudService(event.file)
            }

            is ActivityEvent.CancelBackupWork -> {
                cancelBackupWorkByTag(event.tag)
                mainSharedViewModel.workInfoMap.remove(event.tag)
                lifecycleScope.launch(Dispatchers.IO) {
                    updateFile.cloudInfo(0, null, event.fileId)
                }
            }
            is ActivityEvent.QueryFCMToken -> {
                queryFCMToken()
            }

            is ActivityEvent.LaunchApp -> {

            }

            is ActivityEvent.RefreshCloudStorageFileList -> {
                refreshCloudStorageFileList()
            }
        }
    }


    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            // Invoked when a dynamic shortcut is clicked.
            Intent.ACTION_VIEW -> {
                val id = intent.data?.lastPathSegment?.toLongOrNull()
                if (id != null) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            getContacts.queryById(id)
                        }.also {
                            if (it != null) {
                                mainSharedViewModel.navigateToChatPage(it)
                            }
                        }
                    }
                }
            }
            // Invoked when a text is shared through Direct Share.
            Intent.ACTION_SEND -> {
                val shortcutId = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                shortcutId?.toLong()?.let { id ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            getContacts.queryById(id)
                        }.also {
                            if (it != null) {
                                mainSharedViewModel.navigateToChatPage(it)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply Darkmode
        collectDarkModeFlow()

        // Navigate to Page
        if (savedInstanceState == null) {
            intent?.let(::handleIntent)
        }

        // Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Record
        recordPath = "${externalCacheDir!!.absoluteFile}/audio_record.mp3"
//        recordPath = "${getExternalFilesDir("chat")!!.absoluteFile}/audio_record.mp3"

        // Activity Result Api
        userAvatarPickerLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                if (it != null) {
                    setUserAvatar(it)
                }
            }


        // Request Permission Launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

        backupLocationSelector =
            registerForActivityResult(CreateDocument("application/vnd.sqlite3")) { uri ->
                if (uri != null) {
                    getExternalFilesDir("backup")?.also { dir ->
                        dir.listFiles()?.firstOrNull()?.also { file ->
                            contentResolver.openOutputStream(uri)?.use { output ->
                                file.inputStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            file.delete()
                        }
                        Toast.makeText(this, "备份已完成", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "操作取消", Toast.LENGTH_SHORT).show()
                }

            }

        restoreLocationSelector =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    contentResolver.openInputStream(uri)?.use { input ->
                        getExternalFilesDir("backup")?.also { dir ->
                            dir.listFiles()?.forEach { it.delete() }
                            val file = java.io.File(dir, "chat.db")
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                            restoreDatabase(file = file)
                        }
                    }
                }
            }

        // File Download Process
        lifecycleScope.launch(Dispatchers.IO) {
            getFiles.allStatic().forEach {
                if (it.downloadPath == null) {
                    updateFile.downloadState(DownloadingState.None, it)
                    updateFile.downloadInfo(null, null, it)
                } else {
                    val path = java.io.File(
                        Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOWNLOADS}/Parabox"),
                        it.downloadPath
                    )
                    if (!path.exists()) {
                        updateFile.downloadState(DownloadingState.None, it)
                        updateFile.downloadInfo(null, null, it)
                    } else {
                        if (it.downloadingState is DownloadingState.Downloading
                            && ((it.downloadingState as DownloadingState.Downloading).downloadedBytes == 0)
                        ) {
                            updateFile.downloadState(DownloadingState.Failure, it)
                            updateFile.downloadInfo(null, null, it)
                        }
                        retrieveDownloadProcess(it)
                    }
                }
            }
        }

        // Drive
        getDriveInformation()

        // Backup
        backup = RoomBackup(this)

        // Cloud Backup
        backupFileToCloudService()

        // Firebase AppCheck
        firebaseAppCheck()

        // Obtain the FirebaseAnalytics instance.
        analytics = Firebase.analytics

        // Query FCM Token
        queryFCMToken()

        // Query FireStore
        queryConfigFromFireStore()

        // ML-Kit
        initializeMLKit()

        // FCM Notification Channel
        notificationUtil.createNotificationChannel(
            "9999",
            "FCM",
            "FCM 推送的消息",
            NotificationManager.IMPORTANCE_HIGH
        )

        // Auto Delete Chat Resource
        deleteChatFiles()

        // CloudStorage Files
        refreshCloudStorageFileList(withDelay = true)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            // System Ui
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = isSystemInDarkTheme()
            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !useDarkIcons
                )
            }

            // System Bars
            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            val fixedInsets = remember {
                FixedInsets(
                    statusBarHeight = systemBarsPadding.calculateTopPadding(),
                    navigationBarHeight = systemBarsPadding.calculateBottomPadding()
                )
            }

            val mainNavController = rememberAnimatedNavController()
            val mainNavHostEngine = rememberAnimatedNavHostEngine(
                navHostContentAlignment = Alignment.TopCenter,
                rootDefaultAnimations = RootNavGraphDefaultAnimations(
//                    enterTransition = { slideInHorizontally { it }},
//                    exitTransition = { slideOutHorizontally { -it }},
//                    popEnterTransition = { slideInHorizontally { -it }},
//                    popExitTransition = { slideOutHorizontally { it }},
//                    enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 0.9f) },
//                    exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 1.1f) },
//                    popEnterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 1.1f) },
//                    popExitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 0.9f) }
                    enterTransition = { slideInHorizontally { 100 } + fadeIn() },
                    exitTransition = { slideOutHorizontally { -100 } + fadeOut() },
                    popEnterTransition = { slideInHorizontally { -100 } + fadeIn() },
                    popExitTransition = { slideOutHorizontally { 100 } + fadeOut() }
                ),
                defaultAnimationsForNestedNavGraph = mapOf(
                    NavGraphs.guide to NestedNavGraphDefaultAnimations(
                        enterTransition = { slideInHorizontally { it } },
                        exitTransition = { slideOutHorizontally { -it } },
                        popEnterTransition = { slideInHorizontally { -it } },
                        popExitTransition = { slideOutHorizontally { it } },
//                        enterTransition = { slideInHorizontally { 100 } + fadeIn() },
//                        exitTransition = { slideOutHorizontally { -100 } + fadeOut() },
//                        popEnterTransition = { slideInHorizontally { -100 } + fadeIn() },
//                        popExitTransition = { slideOutHorizontally { 100 } + fadeOut() }
                    )
                )
            )
            // Shared ViewModel
//            val mainSharedViewModel = hiltViewModel<MainSharedViewModel>(this)

            // Screen Sizes
            val sizeClass = calculateWindowSizeClass(activity = this)
//            val shouldShowNav = menuNavController.appCurrentDestinationAsState().value in listOf(
//                MessagePageDestination,
//                FilePageDestination,
//                SettingPageDestination
//            )

            // Navigate to guide
            LaunchedEffect(Unit) {
                // read from datastore
                val isFirstLaunch = !mainSharedViewModel.guideLaunchedStateFlow.value
                        && dataStore.data.first()[DataStoreKeys.IS_FIRST_LAUNCH] ?: true
                if (isFirstLaunch) {
                    mainSharedViewModel.launchedGuide()
                    mainNavController.navigate(GuideWelcomePageDestination) {
                        popUpTo(NavGraphs.root) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            AppTheme {
                CompositionLocalProvider(values = arrayOf(LocalFixedInsets provides fixedInsets)) {
                    DestinationsNavHost(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        navGraph = NavGraphs.root,
                        engine = mainNavHostEngine,
                        navController = mainNavController,
                        dependenciesContainerBuilder = {
                            dependency(mainSharedViewModel)
                            dependency(sizeClass)
                            dependency { event: ActivityEvent -> onEvent(event) }
                        })

                }

//                MessagePage(
//                    onConnectBtnClicked = {
//                        pluginConn.connect()
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.STARTED){
//                                pluginConn.connectionStateFlow.collect {
//                                    Log.d("parabox", "connection state received")
//                                    viewModel.setSendAvailableState(it)
//                                }
//                            }
//                        }
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.STARTED){
//                                repeatOnLifecycle(Lifecycle.State.STARTED) {
//                                    pluginConn.messageResFlow.collect {
//                                        Log.d("parabox", "message received")
//                                        viewModel.setMessage(it)
//                                    }
//                                }
//                            }
//                        }
//                    },
//                    onSendBtnClicked = {
//                        pluginConn.send(
//                            (0..10).random().toString()
//                        )
//                    }
//                )
            }
        }
    }

    override fun onStart() {
        inBackground = false
        startPluginConnectionService()
        super.onStart()
    }

    override fun onStop() {
        inBackground = true
        lifecycleScope.launch(Dispatchers.Main) {
            val workingMode = dataStore.data.first()[DataStoreKeys.SETTINGS_WORKING_MODE]
                ?: WorkingMode.NORMAL.ordinal
            if (workingMode == WorkingMode.NORMAL.ordinal) {
                unbindService(pluginServiceConnection)
                pluginService = null
            }
        }
        super.onStop()
    }

    private fun startPluginConnectionService() {
        lifecycleScope.launch(Dispatchers.Main) {
            val workingMode = dataStore.data.first()[DataStoreKeys.SETTINGS_WORKING_MODE]
                ?: WorkingMode.NORMAL.ordinal
            if (workingMode == WorkingMode.NORMAL.ordinal) {
                val pluginServiceBinderIntent = Intent(this@MainActivity, PluginService::class.java)
                pluginServiceConnection = object : ServiceConnection {
                    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                        Log.d("parabox", "mainActivity - service connected")
                        pluginService =
                            (p1 as PluginService.PluginServiceBinder).getService().also {
                                mainSharedViewModel.setPluginListStateFlow(it.getAppModelList())
                                it.setPluginListListener(object : PluginListListener {
                                    override fun onPluginListChange(pluginList: List<AppModel>) {
                                        mainSharedViewModel.setPluginListStateFlow(pluginList)
                                    }
                                })
                            }
                    }

                    override fun onServiceDisconnected(p0: ComponentName?) {
                        Log.d("parabox", "mainActivity - service disconnected")
//                        Toast.makeText(
//                            baseContext,
//                            getString(R.string.stop_extension_connection_success),
//                            Toast.LENGTH_SHORT
//                        ).show()
                        pluginService = null
                    }

                }
                startService(pluginServiceBinderIntent)
                bindService(pluginServiceBinderIntent, pluginServiceConnection, BIND_AUTO_CREATE)

                pluginService?.also {
                    it.reset()
                }
            }
        }
    }
}
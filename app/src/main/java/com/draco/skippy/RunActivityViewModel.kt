package com.draco.skippy

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RunActivityViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val BUFFER_SIZE = 1024 * 8
        const val WAKELOCK_TAG = "Skippy::Wakelock"
    }

    private var process: Process? = null

    private val _commandOutput = MutableLiveData<String>()
    val commandOutput: LiveData<String> = _commandOutput

    var handled = false

    private var powerManager = application.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)

    private val workingDir = with (application.applicationContext) {
        getExternalFilesDir(null) ?: externalCacheDir ?: filesDir
    }

    fun runScript(contentResolver: ContentResolver, uri: Uri) {
        contentResolver
            .openInputStream(uri)
            ?.bufferedReader()
            .use { reader ->
                reader?.readText()?.let {
                    runScript(it)
                }
            }
    }

    @SuppressLint("WakelockTimeout")
    fun runScript(contents: String) {
        handled = true
        viewModelScope.launch(Dispatchers.IO) {
            wakelock.acquire()

            process = ProcessBuilder("sh", "-c", contents)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val buffer = OutputCharBuffer(BUFFER_SIZE)
            process?.inputStream?.bufferedReader().use {
                while (true) {
                    try {
                        val c = it?.read() ?: break
                        if (c == -1)
                            break

                        buffer.add(c.toChar())

                        val out = buffer.get()
                        _commandOutput.postValue(out)
                    } catch (_: Exception) {
                        break
                    }
                }
            }

            wakelock.release()
        }
    }
}
package com.draco.skippy

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.util.concurrent.Executors

class RunActivity : Activity() {
    private lateinit var scrollView: ScrollView
    private lateinit var output: TextView

    private val executorService = Executors.newFixedThreadPool(1)

    private var process: Process? = null

    private lateinit var workingDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run)

        scrollView = findViewById(R.id.scroll_view)
        output = findViewById(R.id.output)

        workingDir = getExternalFilesDir(null) ?: externalCacheDir ?: filesDir
            .also { it.deleteOnExit() }

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                intent?.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    runScript(it)
                }

                intent?.extras?.get(Intent.EXTRA_STREAM)?.let {
                    runScript(it as Uri)
                }
            }

            Intent.ACTION_VIEW -> {
                intent?.data?.let {
                    runScript(it)
                }
            }
        }
    }

    private fun runScript(uri: Uri) {
        contentResolver
            .openInputStream(uri)
            ?.bufferedReader()
            .use { reader ->
                reader?.readText()?.let {
                    runScript(it)
                }
            }
    }

    private fun runScript(contents: String) {
        executorService.execute {
            process = ProcessBuilder("sh", "-c", contents)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val buffer = OutputBuffer(128)
            process?.inputStream?.bufferedReader().use {
                while (true) {
                    try {
                        val line = it?.readLine() ?: break
                        buffer.add(line)

                        val out = buffer.get()
                        runOnUiThread {
                            output.text = out
                            output.setTextIsSelectable(false)
                            scrollView.fullScroll(View.FOCUS_DOWN)
                            output.setTextIsSelectable(true)
                        }
                    } catch (_: Exception) {
                        return@execute
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        process?.destroy()
        super.onDestroy()
    }
}
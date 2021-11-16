package com.draco.skippy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class RunActivity : AppCompatActivity() {
    private val viewModel: RunActivityViewModel by viewModels()

    private lateinit var scrollView: ScrollView
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run)

        scrollView = findViewById(R.id.scroll_view)
        output = findViewById(R.id.output)

        viewModel.commandOutput.observe(this) {
            output.text = it
            output.setTextIsSelectable(false)
            scrollView.fullScroll(View.FOCUS_DOWN)
            output.setTextIsSelectable(true)
        }

        /* Execute the script only once */
        if (viewModel.handled)
            return

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                intent?.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    viewModel.runScript(it)
                }

                intent?.extras?.get(Intent.EXTRA_STREAM)?.let {
                    viewModel.runScript(contentResolver, it as Uri)
                }
            }

            Intent.ACTION_VIEW -> {
                intent?.data?.let {
                    viewModel.runScript(contentResolver, it)
                }
            }
        }
    }

}
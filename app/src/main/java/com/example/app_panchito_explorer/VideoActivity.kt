package com.example.app_panchito_explorer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VideoActivity : AppCompatActivity() {

    private lateinit var webVideo: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val videoId = "LcApcwU06mM"
        webVideo = findViewById(R.id.webVideo)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnAbrirYoutube).setOnClickListener {
            abrirYoutube(videoId)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (webVideo.canGoBack()) {
                webVideo.goBack()
            } else {
                finish()
            }
        }

        webVideo.apply {
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadDataWithBaseURL(
                "https://www.youtube.com",
                """
                <html>
                    <body style="margin:0;background:#000;">
                        <iframe
                            width="100%"
                            height="100%"
                            src="https://www.youtube.com/embed/$videoId"
                            title="Video Tutorial"
                            frameborder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowfullscreen>
                        </iframe>
                    </body>
                </html>
                """.trimIndent(),
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    private fun abrirYoutube(videoId: String) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/$videoId"))

        try {
            startActivity(appIntent)
        } catch (_: Exception) {
            startActivity(webIntent)
        }
    }

    override fun onDestroy() {
        if (::webVideo.isInitialized) {
            webVideo.destroy()
        }
        super.onDestroy()
    }
}

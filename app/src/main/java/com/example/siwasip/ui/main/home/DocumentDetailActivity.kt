package com.example.siwasip.ui.main.home

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.siwasip.R
import android.net.Uri

class DocumentDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_URL = "extra_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_detail)

        val txtTitle = findViewById<TextView>(R.id.txtDetailTitle)
        val txtDate = findViewById<TextView>(R.id.txtDetailDate)
        val webView = findViewById<WebView>(R.id.webViewDocument)
        val btnBack = findViewById<Button>(R.id.btnBackToHome)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "-"
        val date = intent.getStringExtra(EXTRA_DATE) ?: "-"
        val url = intent.getStringExtra(EXTRA_URL) ?: ""

        txtTitle.text = "Judul : $title"
        txtDate.text = "Tanggal Unggah : $date"

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // Pakai Google Docs viewer supaya PDF aman di WebView
        val googleUrl = "https://docs.google.com/gview?embedded=1&url=${Uri.encode(url)}"
        webView.loadUrl(googleUrl)

        btnBack.setOnClickListener {
            finish()
        }
    }
}
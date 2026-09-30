package com.example.siptvuploader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etMac: EditText
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var etPin: EditText
    private lateinit var tvStatus: TextView
    private lateinit var historyList: ListView

    private lateinit var prefs: android.content.SharedPreferences
    private val historyEntries = mutableListOf<JSONObject>()
    private lateinit var historyAdapter: ArrayAdapter<String>

    companion object {
        private const val TARGET_URL = "https://siptv.app/mylist/"
        private const val PREFS_NAME = "tvabc_uploader_prefs"
        private const val KEY_HISTORY = "history"
        private const val DEFAULT_PASSWORD = "parola"
        private const val MAX_HISTORY = 30
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        webView = findViewById(R.id.webView)
        etMac = findViewById(R.id.etMac)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        etPin = findViewById(R.id.etPin)
        tvStatus = findViewById(R.id.tvStatus)
        historyList = findViewById(R.id.historyList)
        val btnSend: Button = findViewById(R.id.btnSend)

        // Parola e presetată automat de fiecare dată când deschizi aplicația.
        etPass.setText(DEFAULT_PASSWORD)

        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        historyList.adapter = historyAdapter
        loadHistory()

        historyList.setOnItemClickListener { _, _, position, _ ->
            val entry = historyEntries.getOrNull(position) ?: return@setOnItemClickListener
            etMac.setText(entry.optString("mac"))
            etUser.setText(entry.optString("user"))
            etPass.setText(entry.optString("pass", DEFAULT_PASSWORD))
            etPin.setText(entry.optString("pin"))
            Toast.makeText(this, "Câmpuri completate din istoric", Toast.LENGTH_SHORT).show()
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Pagina s-a încărcat; dacă utilizatorul a apăsat deja "Trimite",
                // completăm formularul acum.
                if (pendingSubmit) {
                    pendingSubmit = false
                    runFillScript()
                }
            }
        }

        btnSend.setOnClickListener {
            val mac = etMac.text.toString().trim()
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (mac.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completează MAC, Username și Parolă", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveToHistory(mac, user, pass, etPin.text.toString().trim())

            tvStatus.text = "Se încarcă siptv.app ..."
            pendingSubmit = true
            webView.loadUrl(TARGET_URL)
        }
    }

    private var pendingSubmit = false

    // ---------- Istoric liste adăugate (salvat local pe telefon, persistent) ----------

    private fun loadHistory() {
        historyEntries.clear()
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            historyEntries.add(arr.getJSONObject(i))
        }
        refreshHistoryUi()
    }

    private fun saveToHistory(mac: String, user: String, pass: String, pin: String) {
        // Evită duplicatele: dacă MAC + user există deja, îl mutăm în capul listei.
        historyEntries.removeAll { it.optString("mac") == mac && it.optString("user") == user }

        val entry = JSONObject()
        entry.put("mac", mac)
        entry.put("user", user)
        entry.put("pass", pass)
        entry.put("pin", pin)
        entry.put("date", SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date()))

        historyEntries.add(0, entry)
        while (historyEntries.size > MAX_HISTORY) {
            historyEntries.removeAt(historyEntries.size - 1)
        }

        val arr = JSONArray()
        historyEntries.forEach { arr.put(it) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()

        refreshHistoryUi()
    }

    private fun refreshHistoryUi() {
        historyAdapter.clear()
        historyEntries.forEach { entry ->
            val mac = entry.optString("mac")
            val user = entry.optString("user")
            val date = entry.optString("date")
            historyAdapter.add("$mac  —  $user   ($date)")
        }
        historyAdapter.notifyDataSetChanged()
    }

    /**
     * Injectează JavaScript care caută câmpurile MAC / URL / PIN pe pagina
     * reală siptv.app/mylist și completează + trimite formularul, exact cum ai
     * face-o tu manual din browser.
     *
     * IMPORTANT: siptv.app își poate schimba structura paginii oricând. Dacă
     * scriptul nu găsește un câmp, verifică în tvStatus mesajul de eroare și
     * ajustează selectorii de mai jos (vezi comentariile) după ce inspectezi
     * pagina în Chrome (click-dreapta -> Inspect pe fiecare câmp).
     */
    private fun buildPlaylistUrl(): String {
        val user = etUser.text.toString().trim()
        val pass = etPass.text.toString().trim()
        return "http://rezerva.tvabc.eu:80/get.php?username=$user&password=$pass&type=m3u_plus&output=ts"
    }

    private fun runFillScript() {
        val mac = JSONObject.quote(etMac.text.toString().trim())
        val playlistUrl = JSONObject.quote(buildPlaylistUrl())
        val pin = JSONObject.quote(etPin.text.toString().trim())

        val js = """
            (function() {
                function findInput(keywords) {
                    var inputs = document.querySelectorAll('input');
                    for (var i = 0; i < inputs.length; i++) {
                        var el = inputs[i];
                        var hay = ((el.id || '') + ' ' + (el.name || '') + ' ' +
                                   (el.placeholder || '') + ' ' + (el.className || '')).toLowerCase();
                        for (var k = 0; k < keywords.length; k++) {
                            if (hay.indexOf(keywords[k]) !== -1) return el;
                        }
                    }
                    return null;
                }

                function setValue(el, value) {
                    if (!el || !value) return false;
                    el.focus();
                    el.value = value;
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return true;
                }

                function clickByText(keywords) {
                    var clickable = document.querySelectorAll('button, a, input[type=button], input[type=submit], [role=button]');
                    for (var i = 0; i < clickable.length; i++) {
                        var el = clickable[i];
                        var text = (el.innerText || el.value || '').toLowerCase();
                        for (var k = 0; k < keywords.length; k++) {
                            if (text.indexOf(keywords[k]) !== -1) {
                                el.click();
                                return true;
                            }
                        }
                    }
                    return false;
                }

                var result = { mac: false, urlTab: false, url: false, pin: false, submit: false };

                var macEl = findInput(['mac']);
                result.mac = setValue(macEl, ${mac});

                // Comută pe modul "URL" (link extern), nu "File"
                result.urlTab = clickByText(['url']);

                var urlEl = findInput(['url', 'link', 'playlist']);
                result.url = setValue(urlEl, ${playlistUrl});

                var pinValue = ${pin};
                if (pinValue) {
                    var pinEl = findInput(['pin']);
                    result.pin = setValue(pinEl, pinValue);
                }

                // Trimite formularul
                result.submit = clickByText(['send', 'trimite', 'upload', 'submit', 'save']);

                return JSON.stringify(result);
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { rawResult ->
            runOnUiThread {
                tvStatus.text = "Rezultat completare: $rawResult"
            }
        }
    }
}

package com.example.wormhole.engine

import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

object DomExtractor {

    // JavaScript to extract clean readable text from page
    private val TEXT_EXTRACTION_SCRIPT = """
        (function() {
            try {
                var clone = document.body.cloneNode(true);
                var toRemove = clone.querySelectorAll('script, style, noscript, svg, iframe, nav, footer, header');
                toRemove.forEach(function(el) { el.remove(); });
                var text = clone.innerText || clone.textContent || '';
                text = text.replace(/\s+/g, ' ').trim();
                return JSON.stringify({
                    title: document.title || '',
                    url: window.location.href || '',
                    text: text.substring(0, 12000)
                });
            } catch(e) {
                return JSON.stringify({ title: document.title || '', url: window.location.href || '', text: '' });
            }
        })();
    """.trimIndent()

    // JavaScript to extract interactive actionable elements (for Agent Mode)
    private val INTERACTIVE_ELEMENTS_SCRIPT = """
        (function() {
            try {
                var items = [];
                var inputs = document.querySelectorAll('input:not([type="hidden"]), textarea, select');
                inputs.forEach(function(el, idx) {
                    var rect = el.getBoundingClientRect();
                    if (rect.width > 0 && rect.height > 0) {
                        items.push({
                            type: 'input',
                            tag: el.tagName.toLowerCase(),
                            id: el.id || '',
                            name: el.name || '',
                            placeholder: el.placeholder || '',
                            selector: el.id ? '#' + el.id : (el.name ? '[name="' + el.name + '"]' : el.tagName.toLowerCase() + ':nth-of-type(' + (idx+1) + ')'),
                            currentValue: el.value || ''
                        });
                    }
                });
                var buttons = document.querySelectorAll('button, input[type="submit"], [role="button"], a.btn, a[href^="http"]');
                buttons.forEach(function(el, idx) {
                    var rect = el.getBoundingClientRect();
                    if (rect.width > 0 && rect.height > 0 && idx < 25) {
                        var text = (el.innerText || el.value || el.getAttribute('aria-label') || '').trim();
                        if (text.length > 0 && text.length < 50) {
                            items.push({
                                type: 'button',
                                text: text,
                                selector: el.id ? '#' + el.id : (el.className ? '.' + el.className.split(' ')[0] : el.tagName.toLowerCase())
                            });
                        }
                    }
                });
                return JSON.stringify(items);
            } catch(e) {
                return '[]';
            }
        })();
    """.trimIndent()

    fun extractCleanContent(webView: WebView, onResult: (title: String, url: String, text: String) -> Unit) {
        webView.evaluateJavascript(TEXT_EXTRACTION_SCRIPT) { rawResult ->
            if (rawResult == null || rawResult == "null") {
                onResult(webView.title ?: "", webView.url ?: "", "")
                return@evaluateJavascript
            }
            try {
                // String returned by evaluateJavascript is JSON encoded
                val unescaped = if (rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                    JSONObject("{ \"res\": $rawResult }").getString("res")
                } else {
                    rawResult
                }
                val obj = JSONObject(unescaped)
                val title = obj.optString("title", webView.title ?: "")
                val url = obj.optString("url", webView.url ?: "")
                val text = obj.optString("text", "")
                onResult(title, url, text)
            } catch (e: Exception) {
                onResult(webView.title ?: "", webView.url ?: "", "")
            }
        }
    }

    fun extractInteractiveElements(webView: WebView, onResult: (List<Map<String, String>>) -> Unit) {
        webView.evaluateJavascript(INTERACTIVE_ELEMENTS_SCRIPT) { rawResult ->
            val list = mutableListOf<Map<String, String>>()
            if (rawResult != null && rawResult != "null") {
                try {
                    val unescaped = if (rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                        JSONObject("{ \"res\": $rawResult }").getString("res")
                    } else {
                        rawResult
                    }
                    val arr = JSONArray(unescaped)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val map = mutableMapOf<String, String>()
                        obj.keys().forEach { key ->
                            map[key] = obj.optString(key)
                        }
                        list.add(map)
                    }
                } catch (_: Exception) {}
            }
            onResult(list)
        }
    }
}

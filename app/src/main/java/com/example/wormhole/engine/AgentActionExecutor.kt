package com.example.wormhole.engine

import android.webkit.WebView
import com.example.wormhole.data.ActionType
import com.example.wormhole.data.AgentAction

object AgentActionExecutor {

    fun executeAction(webView: WebView, action: AgentAction, onComplete: (success: Boolean, message: String) -> Unit) {
        when (action.type) {
            ActionType.CLICK -> {
                val script = """
                    (function() {
                        var el = document.querySelector('${escapeJs(action.selector)}');
                        if (!el) {
                            var buttons = Array.from(document.querySelectorAll('button, a, input[type="submit"]'));
                            el = buttons.find(b => (b.innerText || b.value || '').toLowerCase().includes('${escapeJs(action.value.lowercase())}'));
                        }
                        if (el) {
                            el.scrollIntoView({behavior: 'smooth', block: 'center'});
                            // Highlight element for user visual confirmation
                            var origBorder = el.style.outline;
                            el.style.outline = '3px solid #FFD54F';
                            setTimeout(function() { el.style.outline = origBorder; }, 1200);
                            el.click();
                            return "Clicked element: " + (el.id || el.tagName);
                        }
                        return "Element not found: ${escapeJs(action.selector)}";
                    })();
                """.trimIndent()
                runScript(webView, script, onComplete)
            }
            ActionType.INPUT_TEXT -> {
                val script = """
                    (function() {
                        var el = document.querySelector('${escapeJs(action.selector)}');
                        if (!el) {
                            var inputs = Array.from(document.querySelectorAll('input, textarea'));
                            el = inputs.find(i => (i.placeholder || i.name || i.id || '').toLowerCase().includes('${escapeJs(action.description.lowercase())}'));
                        }
                        if (el) {
                            el.scrollIntoView({behavior: 'smooth', block: 'center'});
                            el.focus();
                            el.value = '${escapeJs(action.value)}';
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            return "Entered text into: " + (el.name || el.id || el.tagName);
                        }
                        return "Input field not found: ${escapeJs(action.selector)}";
                    })();
                """.trimIndent()
                runScript(webView, script, onComplete)
            }
            ActionType.SCROLL -> {
                val scrollAmount = if (action.value.lowercase().contains("up")) -600 else 600
                val script = "window.scrollBy({ top: $scrollAmount, behavior: 'smooth' }); 'Scrolled page';"
                runScript(webView, script, onComplete)
            }
            ActionType.SUBMIT -> {
                val script = """
                    (function() {
                        var form = document.querySelector('${escapeJs(action.selector)}');
                        if (!form) form = document.querySelector('form');
                        if (form) {
                            form.submit();
                            return "Form submitted";
                        }
                        return "No form found to submit";
                    })();
                """.trimIndent()
                runScript(webView, script, onComplete)
            }
            ActionType.NAVIGATE -> {
                webView.post {
                    webView.loadUrl(action.value)
                    onComplete(true, "Navigating to: ${action.value}")
                }
            }
            ActionType.EXTRACT -> {
                onComplete(true, "Content extracted into context")
            }
        }
    }

    private fun runScript(webView: WebView, script: String, onComplete: (Boolean, String) -> Unit) {
        webView.post {
            webView.evaluateJavascript(script) { result ->
                val cleaned = result?.replace("\"", "") ?: "No response"
                val success = !cleaned.lowercase().contains("not found") && !cleaned.lowercase().contains("error")
                onComplete(success, cleaned)
            }
        }
    }

    private fun escapeJs(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ")
            .replace("\r", "")
    }
}

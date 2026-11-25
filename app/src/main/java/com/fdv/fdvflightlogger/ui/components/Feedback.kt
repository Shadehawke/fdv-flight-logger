package com.fdv.fdvflightlogger.ui.components

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fdv.fdvflightlogger.data.feedback.FeedbackForm

/** Opens the feedback form in a browser tab, with the app version and device filled in. */
fun openFeedback(context: Context, kind: FeedbackForm.Kind) {
    if (!FeedbackForm.isConfigured) return
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull().orEmpty()
    val device = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    val uri = Uri.parse(FeedbackForm.url(kind, version, Build.VERSION.RELEASE, device))
    if (uri.scheme != "https") return
    CustomTabsIntent.Builder().build().launchUrl(context, uri)
}

/** "Suggest an idea" and "Report a problem", wrapping onto two lines on narrow screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackButtons(modifier: Modifier = Modifier) {
    if (!FeedbackForm.isConfigured) return
    val context = LocalContext.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextButton(onClick = { openFeedback(context, FeedbackForm.Kind.SUGGESTION) }) { Text("Suggest an idea") }
        OutlinedButton(onClick = { openFeedback(context, FeedbackForm.Kind.PROBLEM) }) { Text("Report a problem") }
    }
}
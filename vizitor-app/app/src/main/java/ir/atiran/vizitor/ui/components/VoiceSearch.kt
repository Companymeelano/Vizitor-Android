/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | جستجوی صوتی فارسی (Speech → Text)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  با RecognizerIntent بومی اندروید — بدون وابستگی خارجی.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonPurple

/**
 * لانچر تشخیص گفتار فارسی؛ با موفقیت → [onResult]، در دسترس نبودن → [onUnavailable].
 */
@Composable
fun rememberVoiceSearch(
    onResult: (String) -> Unit,
    onUnavailable: () -> Unit
): () -> Unit {
    val launcher: ManagedActivityResultLauncher<Intent, ActivityResult> =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == android.app.Activity.RESULT_OK) {
                res.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                    ?.let(onResult)
            } else {
                onUnavailable()
            }
        }
    return remember(launcher) {
        {
            try {
                launcher.launch(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "نام کالا یا مشتری را بگویید…")
                    }
                )
            } catch (e: ActivityNotFoundException) {
                onUnavailable()
            }
        }
    }
}

/** دکمه میکروفون طلایی برای جستجوی صوتی. */
@Composable
fun MicButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .background(NeonPurple.copy(alpha = 0.22f), CircleShape),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Gold)
    ) {
        Icon(Icons.Filled.Mic, contentDescription = "جستجوی صوتی", modifier = Modifier.size(22.dp))
    }
}

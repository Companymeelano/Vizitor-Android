// ═══════════════════════════════════════════════════════════════════════════
//  اجزای مشترک رابط کاربری — همان ظاهر کارت/دکمه/کادر پنل مدیریت
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.direct.ui.theme.Band
import ir.atiran.vizitor.direct.ui.theme.Err
import ir.atiran.vizitor.direct.ui.theme.Gold
import ir.atiran.vizitor.direct.ui.theme.Line
import ir.atiran.vizitor.direct.ui.theme.Navy
import ir.atiran.vizitor.direct.ui.theme.Ok
import ir.atiran.vizitor.direct.ui.theme.Raised
import ir.atiran.vizitor.direct.ui.theme.TextMain
import ir.atiran.vizitor.direct.ui.theme.TextMuted
import ir.atiran.vizitor.direct.ui.theme.Warn

/** کارت اصلی با سرتیتر طلایی‌شده و نشان مسیر. */
@Composable
fun VizCard(
    title: String,
    badge: String = "",
    icon: String = "",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Band, RoundedCornerShape(18.dp))
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon.isNotBlank()) {
                Text(icon, fontSize = 17.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(title, color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
            Spacer(Modifier.weight(1f))
            if (badge.isNotBlank()) {
                Text(
                    badge,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(46.dp)
                .height(2.dp)
                .background(Gold, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** کادر ورودی با ظاهر یکدست سرمه‌ای. */
@Composable
fun VizField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String = "",
    password: Boolean = false,
    numeric: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { if (hint.isNotBlank()) Text(hint, color = TextMuted, fontSize = 12.5.sp) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Raised,
            unfocusedContainerColor = Raised,
            disabledContainerColor = Raised,
            focusedBorderColor = Gold,
            unfocusedBorderColor = Line,
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            focusedLabelColor = Gold,
            unfocusedLabelColor = TextMuted,
            cursorColor = Gold,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** دکمهٔ طلایی (کنش اصلی). */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Navy,
            disabledContainerColor = Raised,
            disabledContentColor = TextMuted,
        ),
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Navy,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** دکمهٔ کم‌رنگ (کنش فرعی). */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Raised,
            contentColor = TextMain,
            disabledContainerColor = Raised,
            disabledContentColor = TextMuted,
        ),
        modifier = modifier.height(46.dp),
    ) {
        Text(text, fontSize = 14.sp)
    }
}

/** چیپ انتخاب دیتابیس/فیلتر. */
@Composable
fun VizChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (selected) Gold else Raised
    val fg = if (selected) Navy else TextMain
    Box(
        Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) Gold else Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

/** ردیف وضعیت رنگی. */
@Composable
fun StatusRow(kind: StatusKind, text: String) {
    val color = when (kind) {
        StatusKind.Ok -> Ok
        StatusKind.Warn -> Warn
        StatusKind.Error -> Err
        StatusKind.Idle -> TextMuted
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Raised, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextMain, fontSize = 13.sp)
    }
}

enum class StatusKind { Ok, Warn, Error, Idle }

/** کاشی آماری کوچک. */
@Composable
fun KpiTile(value: String, label: String, hint: String = "", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Raised, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(value, color = Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = TextMain, fontSize = 12.5.sp)
        if (hint.isNotBlank()) Text(hint, color = TextMuted, fontSize = 11.sp)
    }
}

/** نوار پیام/خطا. */
@Composable
fun MessageBar(text: String, isError: Boolean) {
    if (text.isBlank()) return
    val color = if (isError) Err else Ok
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text, color = TextMain, fontSize = 13.sp)
    }
}

/** خط جداکنندهٔ نازک. */
@Composable
fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

/** پانوشت امضای سازنده (همان متن نصب‌کننده و پنل). */
@Composable
fun CreditBar() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "طراحی و برنامه‌نویسی: میلاد یقوبی (Milad Yaghoobi)",
            color = Gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "گروه نرم‌افزاری: Meelano Studio Design",
            color = TextMuted,
            fontSize = 11.5.sp,
        )
    }
}

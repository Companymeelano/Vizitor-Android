/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب «ثبت ویزیت» (نسخه ۲٫۱۴٫۰)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  بعد از ورود ویزیتور (dbo.sys_users) این بخش فعال می‌شود:
 *    ۱) موقعیت GPS مشتری (اختیاری؛ با اجازهٔ کاربر)
 *    ۲) انتخاب مشتری مجاز از dbo.sys_cus (جست‌وجو + فهرست)
 *    ۳) مدت ویزیت (دقیقه) + توضیح
 *    ۴) ثبت در جدول واقعی dbo.Visit (تاریخ شمسی + ساعت + مختصات)
 *    ۵) فهرست ویزیت‌های اخیر + شمارش امروز
 *  همهٔ عملیات داده روی Dispatchers.IO است؛ هیچ کوئری روی ترد UI اجرا نمی‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.atiran.vizitor.sqldirect.VisitCustomerOption
import ir.atiran.vizitor.sqldirect.VisitLocationHelper
import ir.atiran.vizitor.sqldirect.VisitRow
import ir.atiran.vizitor.sqldirect.VisitViewModel
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.TextPrimary
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaNumber

@Composable
fun VisitScreen(
    viewModel: VisitViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val helper = remember(context) { VisitLocationHelper(context) }

    // ── اجازهٔ موقعیت (در سطح سیستم؛ یک‌بار) ─────────────────────────────
    val hasLocationPermission: Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    val locateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startLocating()
            helper.requestSingle { loc -> viewModel.onLocation(loc) }
        } else {
            viewModel.onLocation(null)
        }
    }

    val doLocate: () -> Unit = {
        viewModel.startLocating()
        helper.requestSingle { loc -> viewModel.onLocation(loc) }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            helper.lastKnown()?.let { viewModel.onLastKnown(it) }
        }
    }

    val p = vizitorPalette

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── سرصفحه ──────────────────────────────────────────────────────────
        item {
            VisitHeaderCard(state.visitorName, state.connected, state.todayCount, state.busy, onRefresh = { viewModel.refreshAll() })
        }

        // ── پیام وضعیت ─────────────────────────────────────────────────────
        if (state.status.isNotBlank()) {
            item {
                VisitStatusCard(state.status, state.statusKind, onClear = { viewModel.clearStatus() })
            }
        }

        if (!state.ready) {
            item {
                VisitLockedCard(state.visitorName)
            }
            return@LazyColumn
        }

        // ── موقعیت ─────────────────────────────────────────────────────────
        item {
            VisitLocationCard(
                latitude = state.latitude,
                longitude = state.longitude,
                accuracyM = state.accuracyM,
                locating = state.locating,
                hasPermission = hasLocationPermission,
                onRequestPermission = {
                    locateLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onRefresh = { doLocate() },
                onClear = {
                    viewModel.onLocationCleared()
                },
            )
        }

        // ── مشتری ──────────────────────────────────────────────────────────
        item {
            VisitCustomerCard(
                search = state.search,
                selected = state.selectedCustomer,
                customers = state.customers,
                listExpanded = state.listExpanded,
                onSearch = { viewModel.onSearch(it) },
                onSelect = { viewModel.selectCustomer(it) },
                onClear = { viewModel.clearCustomer() },
            )
        }

        // ── مدت ────────────────────────────────────────────────────────────
        item {
            VisitDurationCard(state.duration, onChange = { viewModel.changeDuration(it) })
        }

        // ── توضیح ──────────────────────────────────────────────────────────
        item {
            VisitDescriptionCard(state.description, onText = { viewModel.onDescription(it) })
        }

        // ── دکمهٔ ثبت ───────────────────────────────────────────────────────
        item {
            VisitSaveButton(
                enabled = state.connected && !state.busy,
                busy = state.busy,
                onClick = { viewModel.record() }
            )
        }

        // ── ویزیت‌های اخیر ─────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ویزیت‌های اخیر",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${state.visits.size.toFaNumber()} رکورد",
                    fontSize = 10.5.sp,
                    color = TextSecondary
                )
            }
        }

        if (state.visits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, p.glassBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "هنوز ویزیتی ثبت نشده است — اولین ویزیت شما با دکمهٔ بالا ثبت می‌شود.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(state.visits) { v ->
                VisitRowItem(v)
            }
        }
    }
}

// ═══════════════════════════════ کارت‌ها ═══════════════════════════════

@Composable
private fun VisitCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val p = vizitorPalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.surface.copy(alpha = 0.85f))
            .border(1.dp, p.glassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun VisitCardTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
    }
}

@Composable
private fun VisitHeaderCard(
    visitorName: String,
    connected: Boolean,
    todayCount: Int,
    busy: Boolean,
    onRefresh: () -> Unit
) {
    val p = vizitorPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(p.surface.copy(alpha = 0.95f), p.surfaceDeep.copy(alpha = 0.9f))
                )
            )
            .border(1.dp, p.gold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(p.btnPrimaryTop, p.btnPrimaryBottom))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Route, contentDescription = "ثبت ویزیت", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ثبت ویزیت",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Text(
                    "ثبت مراجعه به مشتری در جدول dbo.Visit سامانهٔ آتیران",
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRefresh, enabled = !busy) {
                Icon(Icons.Filled.Refresh, contentDescription = "تازه‌سازی", tint = Gold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (connected) NeonGreen else DangerRed)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (connected) "متصل به سرور" else "اتصال قطع است",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (connected) NeonGreen else DangerRed
            )
            Spacer(Modifier.width(14.dp))
            Text(
                "ویزیتور: " + (visitorName.ifBlank { "—"}),
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(p.gold.copy(alpha = 0.15f))
                    .border(0.8.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    "امروز: ${todayCount.toFaNumber()} ویزیت",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }
        }
    }
}

@Composable
private fun VisitStatusCard(status: String, kind: Int, onClear: () -> Unit) {
    val accent = when (kind) {
        1 -> NeonGreen
        2 -> DangerRed
        else -> Gold
    }
    val glyph = when (kind) {
        1 -> Icons.Filled.CheckCircle
        2 -> Icons.Filled.Warning
        else -> Icons.Filled.Info
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(glyph, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            status,
            fontSize = 11.5.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClear, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "بستن", tint = TextSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun VisitLockedCard(visitorName: String) {
    VisitCardContainer {
        Icon(Icons.Filled.Info, contentDescription = null, tint = Gold, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            "برای ثبت ویزیت باید وارد سامانه باشید",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (visitorName.isBlank())
                "از صفحهٔ «تنظیمات» بخش «اتصال به سرور» را کامل کنید (کارت اتصال ← سرور و دیتابیس ← ورود ویزیتور). بعد از ورود، همهٔ بخش‌ها فعال می‌شوند."
            else "نشست شما به‌روز نیست — از صفحهٔ تنظیمات دوباره «اتصال سریع» را بزنید.",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun VisitLocationCard(
    latitude: Double?,
    longitude: Double?,
    accuracyM: Double?,
    locating: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onClear: () -> Unit
) {
    VisitCardContainer {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VisitCardTitle(Icons.Filled.Place, "موقعیت GPS")
            Spacer(Modifier.weight(1f))
            if (locating) {
                CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
            } else if (latitude != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "پاک کردن موقعیت", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            locating -> Text("در حال دریافت موقعیت فعلی…", fontSize = 12.sp, color = TextSecondary)
            latitude != null && longitude != null -> {
                Text(
                    "${latitude.toFaNumber()}، ${longitude.toFaNumber()}" +
                        (accuracyM?.let { "  (دقت ≈ ${it.toFaNumber()} متر)" } ?: ""),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
            else -> Text(
                "موقعیتی ثبت نشده است. برای ثبت دقیق با جغرافیا، دکمهٔ «دریافت موقعیت» را بزنید (یا ویزیت را بدون مختصات ثبت کنید).",
                fontSize = 11.5.sp,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = if (hasPermission) onRefresh else onRequestPermission,
            enabled = !locating,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = vizitorPalette.btnPrimaryBottom,
                contentColor = Color.White
            )
        ) {
            Icon(
                if (hasPermission) Icons.Filled.Refresh else Icons.Filled.LocationSearching,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                if (hasPermission) "دریافت موقعیت فعلی" else "اجازهٔ موقعیت + دریافت",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VisitCustomerCard(
    search: String,
    selected: VisitCustomerOption?,
    customers: List<VisitCustomerOption>,
    listExpanded: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (VisitCustomerOption) -> Unit,
    onClear: () -> Unit
) {
    val p = vizitorPalette
    val matches = remember(search, customers) {
        if (search.isBlank()) emptyList()
        else customers.filter { it.name.contains(search, true) || it.phone.contains(search) }.take(8)
    }
    VisitCardContainer {
        VisitCardTitle(Icons.Filled.Person, "مشتری ویزیت")
        Spacer(Modifier.height(10.dp))
        if (selected != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(p.gold.copy(alpha = 0.12f))
                    .border(1.dp, p.gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        selected.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected.phone.isNotBlank()) {
                        Text(
                            selected.phone.toFaDigits(),
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                IconButton(onClick = onClear, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "تغییر مشتری", tint = TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
        } else {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                placeholder = { Text("جست‌وجو با نام یا شمارهٔ موبایل…", fontSize = 12.5.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(19.dp))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.gold,
                    unfocusedBorderColor = p.glassBorder
                )
            )
        }
        if (listExpanded && search.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            if (matches.isEmpty()) {
                Text("مشتری مجازی با این مشخصات پیدا نشد.", fontSize = 11.sp, color = TextSecondary)
            } else {
                matches.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(c) }
                            .padding(vertical = 7.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (c.phone.isNotBlank()) {
                                Text(c.phone.toFaDigits(), fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitDurationCard(duration: Int, onChange: (Int) -> Unit) {
    val p = vizitorPalette
    VisitCardContainer {
        VisitCardTitle(Icons.Filled.AccessTime, "مدت ویزیت (دقیقه)")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange(-5) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.dp, p.glassBorder, CircleShape)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "کمتر", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                duration.toFaNumber(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Gold
            )
            Spacer(Modifier.width(14.dp))
            IconButton(
                onClick = { onChange(5) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.dp, p.glassBorder, CircleShape)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "بیشتر", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("≈ ساعت: " + (duration / 60).toFaNumber() + " و " + (duration % 60).toFaNumber() + " دقیقه", fontSize = 10.5.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun VisitDescriptionCard(description: String, onText: (String) -> Unit) {
    val p = vizitorPalette
    VisitCardContainer {
        VisitCardTitle(Icons.Filled.Notes, "توضیح ویزیت (اختیاری)")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onText,
            placeholder = { Text("مثلاً: مذاکره برای تمدید سفارش، موجودی انبار مشتری بررسی شد…", fontSize = 12.sp) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.gold,
                unfocusedBorderColor = p.glassBorder
            )
        )
    }
}

@Composable
private fun VisitSaveButton(enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    val p = vizitorPalette
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) p.btnPrimaryBottom else p.surfaceDeep,
            contentColor = if (enabled) Color.White else TextSecondary
        )
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(10.dp))
            Text("در حال ثبت…", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Filled.Route, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("ثبت ویزیت در سامانه", fontSize = 14.5.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun VisitRowItem(v: VisitRow) {
    val p = vizitorPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.surface.copy(alpha = 0.7f))
            .border(1.dp, p.glassBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.gold.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "${v.dateCreated.toFaDigits()} ${v.timeCreated.toFaDigits()}".trim(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${v.durationMin.toFaNumber()} دقیقه",
                fontSize = 10.5.sp,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            if (v.customerName.isBlank()) "مشتری شمارهٔ ${v.shmo.toFaNumber()}" else v.customerName,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (v.description.isNotBlank()) {
            Text(
                v.description,
                fontSize = 11.5.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (v.lat != null && v.lng != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "📍 ${v.lat!!.toFaNumber()}، ${v.lng!!.toFaNumber()}",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

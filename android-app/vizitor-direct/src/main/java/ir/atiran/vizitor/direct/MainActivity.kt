// ═══════════════════════════════════════════════════════════════════════════
//  نقطهٔ ورود برنامه — چیدمان راست‌به‌چپ، تم سرمه‌ای، و چهار صفحه:
//  راه‌اندازی اتصال / ورود / خانه / شناسنامه
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import ir.atiran.vizitor.data.sql.ConnectionState
import ir.atiran.vizitor.direct.ui.components.ThinDivider
import ir.atiran.vizitor.direct.ui.components.VizChip
import ir.atiran.vizitor.direct.ui.screens.AboutScreen
import ir.atiran.vizitor.direct.ui.screens.HomeScreen
import ir.atiran.vizitor.direct.ui.screens.LoginScreen
import ir.atiran.vizitor.direct.ui.screens.SetupScreen
import ir.atiran.vizitor.direct.ui.theme.Gold
import ir.atiran.vizitor.direct.ui.theme.Line
import ir.atiran.vizitor.direct.ui.theme.Navy
import ir.atiran.vizitor.direct.ui.theme.NavyDark
import ir.atiran.vizitor.direct.ui.theme.Ok
import ir.atiran.vizitor.direct.ui.theme.Err
import ir.atiran.vizitor.direct.ui.theme.TextMain
import ir.atiran.vizitor.direct.ui.theme.TextMuted
import ir.atiran.vizitor.direct.ui.theme.VizitorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            VizitorTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(color = Navy, modifier = Modifier.fillMaxSize()) {
                        DirectRoot()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** کارت اتصال (vizitor://…) می‌تواند از مرورگر/فایل روی برنامه باز شود. */
    private fun handleIntent(intent: Intent?) {
        val data = intent?.dataString ?: return
        if (data.startsWith("vizitor://", ignoreCase = true)) incoming.value = data
    }

    companion object {
        /** آخرین کارت اتصالِ رسیده از بیرون برنامه (توسط رابط کاربری خوانده می‌شود). */
        val incoming = MutableStateFlow<String?>(null)
    }
}

private enum class Screen { SETUP, LOGIN, HOME, ABOUT }

@Composable
private fun DirectRoot(vm: DirectViewModel = viewModel()) {

    val form by vm.form.collectAsState()
    val dbList by vm.dbList.collectAsState()
    val session by vm.session.collectAsState()
    val data by vm.data.collectAsState()
    val health by vm.health.collectAsState()
    val dbInfo by vm.dbInfo.collectAsState()
    val connection by vm.connection.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()

    var screen by remember { mutableStateOf(if (form.database.isBlank()) Screen.SETUP else Screen.LOGIN) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ورود خودکار به صفحهٔ خانه پس از موفقیت
    LaunchedEffect(session) {
        if (session is SessionState.LoggedIn) screen = Screen.HOME
    }

    // کارت اتصالِ رسیده از بیرون (QR/لینک)
    val incomingUri by MainActivity.incoming.collectAsState()
    LaunchedEffect(incomingUri) {
        incomingUri?.let {
            vm.applyCardText(it)
            screen = Screen.SETUP
            MainActivity.incoming.value = null
        }
    }

    // اسکنر QR (همان کد نصب‌کننده)
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { vm.applyCardText(it) }
    }

    // خواندن فایل android-connect.json از حافظهٔ گوشی
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (text.isNullOrBlank()) vm.setError("خواندن فایل ممکن نشد.") else vm.applyCardText(text)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Navy)) {
        TopBar(
            connection = connection,
            loggedIn = session is SessionState.LoggedIn,
            onAbout = { screen = Screen.ABOUT },
            onBack = {
                screen = when {
                    session is SessionState.LoggedIn -> Screen.HOME
                    form.database.isNotBlank() -> Screen.LOGIN
                    else -> Screen.SETUP
                }
            },
        )
        ThinDivider()

        Box(Modifier.weight(1f)) {
            when (screen) {
                Screen.SETUP -> SetupScreen(
                    form = form,
                    connection = connection,
                    dbList = dbList,
                    dbInfo = dbInfo,
                    health = health,
                    driverName = vm.driverName,
                    message = message,
                    error = error,
                    onForm = { vm.updateForm(it) },
                    onFetchDatabases = { vm.fetchDatabases() },
                    onConnect = { vm.connectToDatabase(it) },
                    onRefreshHealth = { vm.refreshHealth() },
                    onScanQr = {
                        scanner.launch(
                            ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt("کارت اتصال ویزیتور را در کادر بگیرید")
                                .setBeepEnabled(false)
                                .setOrientationLocked(false)
                                .setBarcodeImageEnabled(false),
                        )
                    },
                    onPickJson = { picker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    onApplyCardText = { vm.applyCardText(it) },
                    onForget = { vm.forgetEverything() },
                )

                Screen.LOGIN -> LoginScreen(
                    maskedTarget = form.user.ifBlank { "—" } + "@" + form.activeHost.ifBlank { "—" } + ":" +
                        form.port + "/" + form.database.ifBlank { "—" },
                    connection = connection,
                    driverName = vm.driverName,
                    loggingIn = session is SessionState.LoggingIn,
                    message = message,
                    error = error,
                    onLogin = { u, p -> vm.login(u, p) },
                    onBackToSetup = {
                        vm.logout()
                        screen = Screen.SETUP
                    },
                )

                Screen.HOME -> {
                    val s = session
                    if (s is SessionState.LoggedIn) {
                        HomeScreen(
                            user = s.user,
                            identity = s.identity,
                            connection = connection,
                            data = data,
                            health = health,
                            message = message,
                            error = error,
                            onLoadCustomers = { vm.loadCustomers() },
                            onLoadProducts = { q -> vm.loadProducts(q) },
                            onRefreshHealth = { vm.refreshHealth() },
                            onLogout = {
                                vm.logout()
                                screen = Screen.LOGIN
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) { screen = Screen.LOGIN }
                    }
                }

                Screen.ABOUT -> AboutScreen(
                    driverName = vm.driverName,
                    appVersion = BuildConfig.VERSION_NAME,
                    maskedTarget = form.user.ifBlank { "—" } + "@" + form.activeHost.ifBlank { "—" } + ":" +
                        form.port + "/" + form.database.ifBlank { "—" },
                    panelUrl = "",
                )
            }
        }

        ThinDivider()
        FooterBar(
            onSettings = { screen = if (session is SessionState.LoggedIn) Screen.HOME else Screen.SETUP },
            onAbout = { screen = Screen.ABOUT },
        )
    }
}

@Composable
private fun TopBar(
    connection: ConnectionState,
    loggedIn: Boolean,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(Gold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("و", color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("سامانهٔ ویزیتور — اتصال مستقیم", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
            Text("SQL Server • پورت ۱۴۳۳", color = TextMuted, fontSize = 11.5.sp)
        }
        val (dot, label) = when (connection) {
            is ConnectionState.Ready -> Ok to "متصل"
            is ConnectionState.Connecting -> Gold to "در حال اتصال"
            is ConnectionState.Disconnected -> TextMuted to "قطع"
            is ConnectionState.Error -> Err to "خطا"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Navy, RoundedCornerShape(12.dp))
                .border(1.dp, Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(Modifier.size(8.dp).background(dot, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextMain, fontSize = 11.5.sp)
        }
        Spacer(Modifier.width(8.dp))
        VizChip(if (loggedIn) "خانه" else "بازگشت", onClick = onBack)
        Spacer(Modifier.width(6.dp))
        VizChip("شناسنامه", onClick = onAbout)
    }
}

@Composable
private fun FooterBar(onSettings: () -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(NavyDark).padding(vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            VizChip("تنظیمات اتصال", onClick = onSettings)
            Spacer(Modifier.width(8.dp))
            VizChip("درباره", onClick = onAbout)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "طراحی و برنامه‌نویسی: میلاد یقوبی (Milad Yaghoobi) • گروه نرم‌افزاری: Meelano Studio Design",
            color = TextMuted,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ناوبری اصلی + نوار پایین ۵ تایی با FAB مرکزی
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  تب ۱: پیشخوان من | تب ۲: ویترین کالا | تب ۳: گفتگو | تب ۴: سبد سفارش (FAB مرکزی)
 *  تب ۵: مشتری | تب ۶: گزارشات و تنظیمات
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.atiran.vizitor.R
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.components.AutoFitText
import ir.atiran.vizitor.ui.components.dashboardBackdrop
import ir.atiran.vizitor.ui.screens.CartScreen
import ir.atiran.vizitor.ui.screens.CatalogScreen
import ir.atiran.vizitor.ui.screens.ChatScreen
import ir.atiran.vizitor.ui.screens.CustomersScreen
import ir.atiran.vizitor.ui.screens.DashboardScreen
import ir.atiran.vizitor.ui.screens.DirectSqlScreen
import ir.atiran.vizitor.ui.screens.ManagerScreen
import ir.atiran.vizitor.ui.screens.ReportsScreen
import ir.atiran.vizitor.ui.screens.ScannerScreen
import ir.atiran.vizitor.ui.screens.SettingsScreen
import ir.atiran.vizitor.ui.screens.SplashScreen
import ir.atiran.vizitor.ui.screens.VisitScreen
import ir.atiran.vizitor.ui.theme.DarkSlateElevated
import ir.atiran.vizitor.ui.theme.GlassBorder
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import ir.atiran.vizitor.ui.theme.NeonPurpleGlow
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

object Routes {
    const val DASHBOARD = "dashboard"
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val CUSTOMERS = "customers"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val CHAT = "chat"
    const val SPLASH = "splash"
    const val SCANNER = "scanner"
    // تب «ثبت ویزیت» — ثبت مراجعه به مشتری در جدول واقعی dbo.Visit (v2.14.0)
    const val VISITS = "visits"
    // صفحهٔ اتصال مستقیم به SQL Server (پورت ۱۴۳۳) — همان چیزی که نصب‌کننده آماده می‌کند
    const val DIRECT_SQL = "directsql"
    // صفحهٔ «تنظیمات ورود» — همان صفحهٔ اتصال در حالت تمام‌صفحه با بازگشت هوشمند
    // (از صفحهٔ اول ← برمی‌گردد به صفحهٔ اول؛ از تنظیمات ← برمی‌گردد به تنظیمات)
    const val WELCOME = "welcome"
    /** پنل مدیریت — فقط با نام کاربری و رمز شخصی مدیر در آتیران (v2.16.0) */
    const val MANAGER = "manager"
    /** «اطلاع‌رسانی اولیه به ویزیتور» — پیش از شروع کار (v2.18.0) */
    const val BRIEFING = "briefing"
    /** مرکز «همهٔ فعالیت‌های ویزیتور» — منوی سه‌بعدی کارها (v2.18.0) */
    const val ACTIVITIES = "activities"
}

data class TabItem(val route: String, val label: String, val icon: ImageVector, val iconRes: Int)

// v2.16.0 — نوار پایین خواناتر: آیکن‌های بزرگ‌تر، برچسب‌های درشت‌تر، ارتفاع بیشتر
private val BAR_HEIGHT = 86.dp
private val TAB_ICON_BOX = 50.dp
private val TAB_IMAGE = 46.dp

private val rightTabs = listOf(
    TabItem(Routes.DASHBOARD, "خانه", Icons.Filled.Dashboard, R.drawable.tab_dashboard),
    TabItem(Routes.CATALOG, "ویترین", Icons.Filled.Storefront, R.drawable.tab_showcase),
)

private val leftTabs = listOf(
    TabItem(Routes.CUSTOMERS, "مشتری", Icons.Filled.Person, R.drawable.tab_customer),
    TabItem(Routes.REPORTS, "گزارشات", Icons.Filled.Receipt, R.drawable.tab_reports)
)

// v2.16.0 — منو ساده شد: ۴ تب + دکمهٔ مرکزی سبد.
// «ویزیت» و «تنظیمات» از کارت‌های دسترسی سریع پیشخوان و نوار بالای صفحه‌ها باز می‌شوند
// (دیگر شش آیکن شلوغ در نوار پایین وجود ندارد و ویزیتور سردرگم نمی‌شود).

@Composable
fun VizitorRoot(
    viewModel: VizitorViewModel = viewModel(),
    // یک نمونهٔ واحد برای کل برنامه: صفحهٔ اول (انتخاب نقش) و صفحهٔ تنظیمات و
    // صفحهٔ اتصال، همه همین وضعیت را می‌بینند تا دو روایت متناقض نداشته باشیم.
    sqlViewModel: ir.atiran.vizitor.sqldirect.DirectSqlViewModel = viewModel(),
    // نمونهٔ واحد تب «ثبت ویزیت» — وضعیت فرم/ویزیت‌های اخیر بین جابه‌جایی تب‌ها حفظ می‌شود
    visitViewModel: ir.atiran.vizitor.sqldirect.VisitViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // دوربین برای اسکنر بارکد
    val cameraPermissionGranted = remember {
        androidx.compose.runtime.mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted.value = granted }

    // نمایش پیام‌های سراسری
    val toast by viewModel.toast.collectAsState()
    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeToast()
        }
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val isSplash = currentRoute == Routes.SPLASH
    // صفحه‌های تمام‌صفحه (تنظیم اتصال/ورود) نوار پایین ندارند و حاشیهٔ نوار
    // سیستم را خودشان (screenSafePadding) مدیریت می‌کنند؛ پس Scaffold نباید
    // دوباره حاشیه بدهد تا هیچ صفحه‌ای فاصلهٔ اضافه/بریدگی نداشته باشد.
    val isSetupScreen = currentRoute == Routes.WELCOME || currentRoute == Routes.DIRECT_SQL
    val isFullScreen = isSplash || isSetupScreen
    // مدت انیمیشن جابه‌جایی صفحه‌ها — روی گوشی ضعیف صفر می‌شود (ضد لگ)
    val routeAnim = if (VizitorPerf.entranceFx) 260 else 0

    // وضعیت اتصال/ورود (منبع واحد) — صفحهٔ اول و تنظیمات از همین می‌خوانند
    val serverSession by ir.atiran.vizitor.sqldirect.VizitorSession.state.collectAsState()

    // ورود به پنل: از هر جای برنامه (صفحهٔ اول، تنظیمات، …)
    val enterPanel: () -> Unit = {
        navController.navigate(Routes.DASHBOARD) {
            popUpTo(Routes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    // ورود به «پنل مدیریت» (نمودارها + جدول‌های گزارش کامل)
    val enterManager: () -> Unit = {
        navController.navigate(Routes.MANAGER) {
            popUpTo(Routes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    // آیا ویزیتور «اطلاع‌رسانی اولیه» این نسخه را دیده است؟ (v2.18.0)
    val briefingSeen by viewModel.briefingSeen.collectAsState()

    // مقصد پس از ورود:
    //   اول بار ⇒ «اطلاع‌رسانی اولیه» (وضعیت اتصال، اختیارات، اهداف و مسیرها)
    //   بارهای بعد ⇒ اگر نقش «مدیریت» انتخاب شده باشد پنل مدیریت، وگرنه پیشخوان
    val enterAfterLogin: () -> Unit = {
        if (!briefingSeen) {
            navController.navigate(Routes.BRIEFING) {
                popUpTo(Routes.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
        } else if (ir.atiran.vizitor.sqldirect.VizitorRoleIntent.wantsManagerPanel()) enterManager()
        else enterPanel()
    }

    // بازگشت هوشمند: اگر جایی برای بازگشت هست برگرد، وگرنه به پنل
    // (صفحهٔ اول ← برمی‌گردد به صفحهٔ اول؛ تنظیمات ← برمی‌گردد به تنظیمات)
    val settingsBack: () -> Unit = {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
        else enterPanel()
    }

    // اگر «ورود سریع» از صفحهٔ اول انجام شد و ورود کامل شد → خودکار به پنل
    var pendingQuickEnter by remember { mutableStateOf(false) }
    LaunchedEffect(serverSession.loggedIn, pendingQuickEnter) {
        if (pendingQuickEnter && serverSession.loggedIn) {
            pendingQuickEnter = false
            enterAfterLogin()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (isFullScreen) WindowInsets(0, 0, 0, 0)
        else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (isFullScreen) return@Scaffold
            VizitorBottomBar(
                navController = navController,
                onFabClick = {
                    navController.navigate(Routes.CART) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                },
                onScanClick = {
                    if (cameraPermissionGranted.value) {
                        navController.navigate(Routes.SCANNER)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .dashboardBackdrop(),
            // جابه‌جایی نرم صفحه‌ها (روی گوشی ضعیف بدون انیمیشن)
            enterTransition = {
                fadeIn(tween(routeAnim)) + slideInHorizontally(tween(routeAnim)) { it / 14 }
            },
            exitTransition = { fadeOut(tween(if (routeAnim > 0) 150 else 0)) },
            popEnterTransition = {
                fadeIn(tween(routeAnim)) + slideInHorizontally(tween(routeAnim)) { -it / 14 }
            },
            popExitTransition = { fadeOut(tween(if (routeAnim > 0) 150 else 0)) }
        ) {
            // صفحه ورود لوکس — نام پخش + مدیریت + دکمه ورود تم‌دار (v2.5.0)
            composable(Routes.SPLASH) {
                SplashScreen(
                    onEnter = {
                        ir.atiran.vizitor.sqldirect.VizitorRoleIntent.set(
                            ir.atiran.vizitor.sqldirect.VizitorRoleIntent.Role.VISITOR
                        )
                        // اگر اتصال تنظیم شده و اعتبارنامه ذخیره است: ورود سریع،
                        // بعد از ورود خودکار به پنل می‌رویم. در غیر این صورت،
                        // کاربر را مستقیم به صفحهٔ تنظیم اتصال می‌بریم.
                        if (serverSession.configured && serverSession.credentialsSaved) {
                            pendingQuickEnter = true
                            sqlViewModel.quickEnter()
                        } else if (!serverSession.configured) {
                            viewModel.showToast("اول اتصال به سرور آتیران را تنظیم کنید ⚙️")
                            navController.navigate(Routes.WELCOME)
                        } else {
                            enterPanel()
                        }
                    },
                    onSoon = { viewModel.showToast(it) },
                    serverSession = serverSession,
                    onManager = {
                        // نقش مدیریت: نام کاربری و رمز شخصی مدیر در آتیران
                        ir.atiran.vizitor.sqldirect.VizitorRoleIntent.set(
                            ir.atiran.vizitor.sqldirect.VizitorRoleIntent.Role.MANAGER
                        )
                        if (serverSession.loggedIn) {
                            enterManager()
                        } else if (serverSession.credentialsSaved) {
                            pendingQuickEnter = true
                            sqlViewModel.quickEnter()
                        } else {
                            navController.navigate(Routes.DIRECT_SQL)
                        }
                    },
                    onOpenServerConfig = { navController.navigate(Routes.WELCOME) },
                    onQuickEnter = {
                        if (serverSession.loggedIn) {
                            enterPanel()
                        } else {
                            pendingQuickEnter = true
                            sqlViewModel.quickEnter()
                        }
                    },
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenChat = { navController.navigate(Routes.CHAT) },
                    onOpenVisits = {
                        navController.navigate(Routes.VISITS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onOpenCatalog = { navController.navigate(Routes.CATALOG) },
                    onOpenManager = { navController.navigate(Routes.MANAGER) },
                    onOpenBriefing = { navController.navigate(Routes.BRIEFING) },
                    onOpenActivities = { navController.navigate(Routes.ACTIVITIES) },
                )
            }
            // ── «اطلاع‌رسانی اولیه به ویزیتور» (v2.18.0) ──
            composable(Routes.BRIEFING) {
                ir.atiran.vizitor.ui.screens.BriefingScreen(
                    viewModel = viewModel,
                    onStart = enterAfterLogin,
                    onOpenActivities = { navController.navigate(Routes.ACTIVITIES) },
                    onOpenVisits = { navController.navigate(Routes.VISITS) },
                    onOpenCustomers = { navController.navigate(Routes.CUSTOMERS) },
                    onOpenCatalog = { navController.navigate(Routes.CATALOG) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            // ── مرکز «همهٔ فعالیت‌های ویزیتور» (v2.18.0) ──
            composable(Routes.ACTIVITIES) {
                ir.atiran.vizitor.ui.screens.ActivitiesScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (navController.previousBackStackEntry != null) navController.popBackStack()
                        else enterPanel()
                    },
                    onOpenBriefing = { navController.navigate(Routes.BRIEFING) },
                    onOpenVisits = { navController.navigate(Routes.VISITS) },
                    onOpenCustomers = { navController.navigate(Routes.CUSTOMERS) },
                    onOpenCatalog = { navController.navigate(Routes.CATALOG) },
                    onOpenCart = { navController.navigate(Routes.CART) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onOpenScanner = {
                        if (cameraPermissionGranted.value) navController.navigate(Routes.SCANNER)
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenChat = { navController.navigate(Routes.CHAT) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            // ── تب «ثبت ویزیت» — ثبت مراجعه در جدول واقعی dbo.Visit (v2.14.0) ──
            composable(Routes.VISITS) { VisitScreen(viewModel = visitViewModel) }
            // ── اتاق گفتگوی ویزیتورها (v2.3.0) ─────────────────────────────
            composable(Routes.CHAT) { ChatScreen(viewModel) }
            composable(Routes.CATALOG) {
                CatalogScreen(
                    viewModel = viewModel,
                    onOpenScanner = {
                        if (cameraPermissionGranted.value) navController.navigate(Routes.SCANNER)
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
            composable(Routes.CART) { CartScreen(viewModel) }
            composable(Routes.CUSTOMERS) { CustomersScreen(viewModel) }
            composable(Routes.REPORTS) { ReportsScreen(viewModel) }
            // ── پنل مدیریت (نمودارها و جدول‌های گزارش کامل) — v2.16.0 ──
            composable(Routes.MANAGER) {
                ManagerScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (navController.previousBackStackEntry != null) navController.popBackStack()
                        else navController.navigate(Routes.SPLASH) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    },
                    onOpenSettings = { navController.navigate(Routes.WELCOME) },
                    onLogout = {
                        sqlViewModel.logout()
                        navController.navigate(Routes.SPLASH) {
                            popUpTo(Routes.MANAGER) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenDirectSql = { navController.navigate(Routes.WELCOME) },
                    serverSession = serverSession,
                    onSyncNow = { sqlViewModel.syncNow() },
                )
            }
            // صفحهٔ «تنظیمات ورود» — سه گام اتصال/سرور/ورود با طراحی لاکچری
            // (همان پیاده‌سازی صفحهٔ اتصال؛ یک منبع، دو مسیر ورودی)
            composable(Routes.WELCOME) {
                DirectSqlScreen(
                    viewModel = sqlViewModel,
                    onBack = settingsBack,
                    onEnterPanel = enterAfterLogin,
                )
            }
            // اتصال مستقیم به SQL Server روی پورت ۱۴۳۳ (بدون API/IIS)
            composable(Routes.DIRECT_SQL) {
                DirectSqlScreen(
                    viewModel = sqlViewModel,
                    onBack = {
                        if (navController.previousBackStackEntry != null) navController.popBackStack()
                        else enterPanel()
                    },
                    onEnterPanel = enterAfterLogin,
                )
            }
            composable(Routes.SCANNER) {
                ScannerScreen(
                    onBarcode = { code ->
                        viewModel.findProductByBarcode(code) { product ->
                            viewModel.showToast(
                                if (product != null) "«${product.name}» با بارکد $code به سبد اضافه شد ✅"
                                else "کالایی با بارکد $code یافت نشد ❌"
                            )
                        }
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * نوار ناوبری پایین شیشه‌ای:
 * [پیشخوان][ویترین] — (FAB مرکزی: سبد سفارش) — [مشتری][گزارشات]
 */
@Composable
private fun VizitorBottomBar(
    navController: androidx.navigation.NavController,
    onFabClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.hierarchy?.firstOrNull()?.route
    val vm: VizitorViewModel = viewModel()
    val cartCount by vm.cartItems.collectAsState()

    // رنگ‌های تم — کپچر در کانتکست کامپوزبل پیش از ورود به لایه رسم
    val hairlinePrimary = NeonPurple
    val hairlineGold = Gold
    val barSurface = vizitorPalette.surface
    val barSurfaceDeep = vizitorPalette.surfaceDeep
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(barSurface.copy(alpha = 0.97f), barSurfaceDeep.copy(alpha = 0.97f))
                    )
                )
                .border(0.5.dp, GlassBorder, RoundedCornerShape(0.dp))
                // خط نور گرادیانی بالای نوار (امضای لوکس)
                .drawBehind {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, hairlinePrimary, hairlineGold, Color.Transparent)
                        ),
                        start = Offset(0f, 0.5f),
                        end = Offset(size.width, 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            rightTabs.forEach { tab ->
                BottomTab(tab, currentRoute == tab.route, navController)
            }

            // جایگاه FAB مرکزی — هم‌ارتفاع با تب‌ها تا کل نوار دقیقاً یک خط تراز شود
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.width(88.dp).height(BAR_HEIGHT),
                contentAlignment = Alignment.Center
            ) { }

            leftTabs.forEach { tab ->
                BottomTab(tab, currentRoute == tab.route, navController)
            }
        }

        // ── دکمه مرکزی شناور (FAB) — تب ۳: سبد سفارش ────────────────────────
        // قوس نور طلایی که دور FAB در مدار می‌چرخد ✨ (در حالت «سبک» ثابت)
        val orbitAngle = if (VizitorPerf.screenFx) {
            val orbitTransition = rememberInfiniteTransition(label = "fabOrbit")
            val a by orbitTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
                label = "orbitAngle"
            )
            a
        } else 35f
        val ringGold = Gold
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-32).dp)
                .size(TAB_ICON_BOX),
            contentAlignment = Alignment.Center
        ) {
            // حلقه مداری نور (رویه غیرکلیپ‌شده)
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f - 4.dp.toPx()
                val cx = size.width / 2f
                val cy = size.height / 2f
                val tl = Offset(cx - r, cy - r)
                val arcSize = Size(r * 2f, r * 2f)
                drawArc(
                    color = ringGold.copy(alpha = 0.20f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = tl, size = arcSize,
                    style = Stroke(2.5.dp.toPx())
                )
                drawArc(
                    color = ringGold,
                    startAngle = orbitAngle, sweepAngle = 105f, useCenter = false,
                    topLeft = tl, size = arcSize,
                    style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // دایره اصلی دکمه
            Box(
                modifier = Modifier
                    .size(TAB_ICON_BOX - 4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonPurpleDark, NeonPurple))
                    )
                    .border(1.5.dp, ringGold.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = Color.Transparent,
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier
                        .size(TAB_ICON_BOX - 4.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "سبد سفارش",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                        val count = cartCount.sumOf { it.quantity }.toInt()
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .clip(CircleShape)
                                    .background(ir.atiran.vizitor.ui.theme.NeonGreen)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$count",
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomTab(
    tab: TabItem,
    selected: Boolean,
    navController: androidx.navigation.NavController
) {
    val p = vizitorPalette
    // همه تب‌ها دقیقاً هم‌اندازه و هم‌ترازاند — تفاوت حالت صرفاً رنگ/حاشیه/نشانگر است
    // (بدون بالاپریدن، بدون تغییر ابعاد؛ بردر و پس‌زمینه دقیقاً در قاب تب رسم می‌شوند)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(BAR_HEIGHT)
            .padding(horizontal = 3.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected)
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(p.primary.copy(alpha = 0.32f), p.primaryDark.copy(alpha = 0.24f))
                            )
                        )
                        .border(1.dp, p.gold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                else
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
            )
            .clickable {
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // نشانگر طلایی بالای تب انتخاب‌شده (داخل قاب، روی خط بالایی)
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 3.dp)
                    .width(30.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(p.gold)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // نشان سه‌بعدی اختصاصی هر تب — ابعاد ثابت در هر دو حالت + واکنش به تم:
            // حلقه/سطح با رنگ‌های پالت، حالت غیرفعال کمی کم‌رنگ‌تر
            // v2.17.0 — کاشی طلایی گوشه‌گرد پشت آیکن (به سبک منوی مرجع)
            Box(
                modifier = Modifier
                    .size(TAB_ICON_BOX)
                    .clip(RoundedCornerShape(17.dp))
                    .then(
                        if (selected)
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(p.goldHighlight, p.gold, p.goldDark)
                                )
                            )
                        else
                            Modifier.background(Color(0x0FFFFFFF))
                    )
                    .then(
                        if (selected)
                            Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(17.dp))
                        else
                            Modifier.border(1.dp, p.gold.copy(alpha = 0.20f), RoundedCornerShape(17.dp))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(tab.iconRes),
                    contentDescription = tab.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (selected) TAB_IMAGE else TAB_IMAGE - 6.dp)
                        .clip(RoundedCornerShape(13.dp)),
                    colorFilter = if (selected) null
                    else ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(0.55f) }
                    )
                )
            }
            Spacer(Modifier.height(3.dp))
            AutoFitText(
                text = tab.label,
                color = if (selected) p.gold else TextSecondary,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                minimumSize = 9.sp,
                maximumSize = 12.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


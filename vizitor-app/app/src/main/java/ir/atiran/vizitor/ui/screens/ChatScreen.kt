/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | اتاق گفتگوی ویزیتورها (v2.3.0)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  ثبت‌نام (نام/تماس/نام‌کاربری انگلیسی/رمز لاتین) + پیام متنی، ویس،
 *  استیکر و ویدیو + ابزار مدیر: سنجاق پیام، قفل موقت گروه، عدم نمایش
 *  شماره/آیدی، حذف پیام، مسدودسازی اعضا — همه همگام با تم فعال برنامه.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import ir.atiran.vizitor.data.local.ChatMessageEntity
import ir.atiran.vizitor.data.local.ChatMessageType
import ir.atiran.vizitor.data.local.ChatPrefs
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.royalBorder
import ir.atiran.vizitor.ui.theme.AccentText
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaTime
import kotlin.math.abs

/** آواتارهای انتخابی خود کاربر در ثبت‌نام گفتگو (۴ چهره واقعی). */
private val MyAvatarChoices = listOf(
    R.drawable.avatar_m_visitor, R.drawable.avatar_f_visitor,
    R.drawable.avatar_m_warehouse, R.drawable.avatar_f_manager
)

/** مخزن آواتار سایر ویزیتورها — آجیل‌ها + چهره‌ها (پایدار بر اساس نام‌کاربری). */
private val VisitorAvatarPool = listOf(
    R.drawable.avatar_pistachio, R.drawable.avatar_almond, R.drawable.avatar_cashew,
    R.drawable.avatar_walnut, R.drawable.avatar_fig,
    R.drawable.avatar_m_visitor, R.drawable.avatar_f_visitor,
    R.drawable.avatar_f_manager, R.drawable.avatar_m_warehouse
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: VizitorViewModel) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val fullName by ChatPrefs.fullName.collectAsState()
    val phone by ChatPrefs.phone.collectAsState()
    val username by ChatPrefs.username.collectAsState()
    val isAdmin by ChatPrefs.isAdmin.collectAsState()
    val hideContact by ChatPrefs.hideContact.collectAsState()
    val groupLocked by ChatPrefs.groupLocked.collectAsState()
    val blocked by ChatPrefs.blocked.collectAsState()
    val myAvatarIdx by ChatPrefs.avatarIndex.collectAsState()

    var showAdmin by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<ChatMessageEntity?>(null) }
    val registered = username.isNotBlank()
    val pinned = messages.filter { it.pinned }
    val memberCount = messages.map { it.senderUsername }.toSet().size + 1
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // ── سرصفحه ──────────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                ShimmerGoldText("گفتگو")
                Text(
                    "اتاق گفتگوی ویزیتورهای آتیران",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            // نشان اعضا
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x0DFFFFFF))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "👥 ${memberCount.toFaNumber()} عضو",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Gold
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { showAdmin = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Gold.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "تنظیمات گفتگو", tint = Gold, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── بنر قفل موقت گروه ───────────────────────────────────────────────
        if (groupLocked) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerRed.copy(alpha = 0.12f))
                    .border(1.dp, DangerRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = DangerRed, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "گروه موقتاً توسط مدیر قفل است — ارسال پیام ممکن نیست.",
                    style = MaterialTheme.typography.labelMedium,
                    color = DangerRed
                )
            }
        }

        // ── نوار پیام سنجاق‌شده ─────────────────────────────────────────────
        pinned.lastOrNull()?.let { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (groupLocked) 6.dp else 0.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gold.copy(alpha = 0.12f))
                    .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { actionTarget = p }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "سنجاق‌شده توسط مدیر: ${p.senderName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (p.type == ChatMessageType.TEXT) p.text else p.type.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ── لیست پیام‌ها ────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "هنوز پیامی رد و بدل نشده — اولین سلام را شما بفرستید 💬",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                if (!msg.mine && msg.senderUsername in blocked) {
                    Text(
                        "🚫 پیام این کاربر مسدودشده است (${msg.senderUsername})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    ChatBubble(
                        msg = msg,
                        myUsername = username,
                        myAvatarRes = MyAvatarChoices[myAvatarIdx.coerceIn(0, MyAvatarChoices.lastIndex)],
                        hideContact = hideContact,
                        onLong = { actionTarget = msg }
                    )
                }
            }
        }

        // ── ثبت‌نام یا کمپوزر ───────────────────────────────────────────────
        if (!registered) {
            RegistrationCard(
                onSubmit = { fn, ph, un, pw, av ->
                    ChatPrefs.saveProfile(context, fn, ph, un, pw, av)
                    viewModel.showToast("به اتاق گفتگو خوش آمدی، «» $fn » 🎉")
                    viewModel.sendChatMessage("به اتاق گفتگوی ویزیتورها پیوستم! 👋", ChatMessageType.TEXT)
                }
            )
        } else {
            ChatComposer(
                locked = groupLocked,
                onSendText = { viewModel.sendChatMessage(it, ChatMessageType.TEXT) },
                onSendVoice = {
                    val d = "00:${(12..59).random()}"
                    viewModel.sendChatMessage(d, ChatMessageType.VOICE)
                    viewModel.showToast("ویس ضبط و ارسال شد 🎙️")
                },
                onSendVideo = {
                    val d = "0${(1..3).random()}:${(10..59).random()}"
                    viewModel.sendChatMessage(d, ChatMessageType.VIDEO)
                    viewModel.showToast("ویدیو ارسال شد 🎬")
                },
                onSticker = { viewModel.sendChatMessage(it, ChatMessageType.STICKER) },
                onLongChatSettings = { showAdmin = true }
            )
        }
    }

    // ── عملیات روی پیام (فشردن طولانی) ──────────────────────────────────────
    actionTarget?.let { m ->
        MessageActionDialog(
            msg = m,
            isMine = m.mine || m.senderUsername == username,
            isAdmin = isAdmin,
            isBlocked = m.senderUsername in blocked,
            onPin = { pin ->
                viewModel.pinChatMessage(m.id, pin)
            },
            onDelete = { viewModel.deleteChatMessage(m.id) },
            onBlock = {
                val nowBlocked = ChatPrefs.toggleBlocked(context, m.senderUsername)
                viewModel.showToast(if (nowBlocked) "کاربر «» ${m.senderUsername} » مسدود شد 🚫" else "کاربر رفع‌مسدود شد ✅")
            },
            onDismiss = { actionTarget = null }
        )
    }

    if (showAdmin) {
        AdminSheetDialog(
            groupLocked = groupLocked,
            hideContact = hideContact,
            isAdmin = isAdmin,
            onToggleLeft = { ChatPrefs.setGroupLocked(context, it) },
            onToggleHide = { ChatPrefs.setHideContact(context, it) },
            onUnlockAdmin = { user, pass ->
                val ok = ChatPrefs.verifyAdmin(context, user, pass)
                if (ok) ChatPrefs.setAdmin(context, true)
                ok
            },
            onChangePass = { newPass -> ChatPrefs.changeAdminPassword(context, newPass) },
            onToast = viewModel::showToast,
            onDismiss = { showAdmin = false }
        )
    }
}

// ═════════════════════════ حباب پیام ═════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    msg: ChatMessageEntity,
    myUsername: String,
    myAvatarRes: Int,
    hideContact: Boolean,
    onLong: () -> Unit
) {
    val mine = msg.mine
    val p = vizitorPalette
    // رنگ آواتار پایدار بر اساس نام‌کاربری
    val avatarGrad = remember(msg.senderUsername) {
        listOf(
            listOf(Color(0xFFFFD166), Color(0xFFC99B2E)),
            listOf(Color(0xFFB04BF8), Color(0xFF7A2FB8)),
            listOf(Color(0xFF2BFF88), Color(0xFF0FBF62)),
            listOf(Color(0xFFFF8FA3), Color(0xFFFF4D6D))
        )[abs(msg.senderUsername.hashCode()) % 4]
    }
    val shape = if (mine)
        RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)
    else
        RoundedCornerShape(5.dp, 18.dp, 18.dp, 18.dp)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End
    ) {
        if (!mine) {
            ChatAvatar(
                resId = VisitorAvatarPool[kotlin.math.abs(msg.senderUsername.hashCode()) % VisitorAvatarPool.size],
                gradient = avatarGrad
            )
            Spacer(Modifier.width(7.dp))
        }
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .then(
                    if (mine)
                        Modifier.background(Brush.linearGradient(listOf(p.btnPrimaryTop, p.primaryDark)))
                    else
                        Modifier.background(Color(0x14FFFFFF))
                )
                .border(1.dp, if (mine) p.gold.copy(alpha = 0.55f) else p.glassBorder, shape)
                .combinedClickable(onClick = onLong, onLongClick = onLong)
                .padding(horizontal = 11.dp, vertical = 8.dp)
        ) {
            // نام فرستنده (یکتا برای دیگران؛ مو «من» لاغیر)
            if (!mine) {
                Text(
                    if (hideContact) "ویزیتور آتیران"
                    else "${msg.senderName} • @${msg.senderUsername}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = AccentText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
            }
            when (msg.type) {
                ChatMessageType.TEXT -> {
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mine) p.onPrimary else p.textPrimary
                    )
                }
                ChatMessageType.VOICE -> VoiceContent(msg, mine, p)
                ChatMessageType.STICKER -> Text(msg.text, fontSize = 40.sp)
                ChatMessageType.VIDEO -> VideoContent(msg)
            }
            Spacer(Modifier.height(3.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.pinned) {
                    Icon(
                        Icons.Filled.Bookmark, contentDescription = "سنجاق‌شده",
                        tint = if (mine) Color(0xFFFFF3D6) else Gold, modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    msg.timeLong.toFaTime(),
                    fontSize = 9.sp,
                    color = if (mine) p.onPrimary.copy(alpha = 0.75f) else TextSecondary
                )
            }
        }
        if (mine) {
            Spacer(Modifier.width(7.dp))
            ChatAvatar(resId = myAvatarRes, gradient = avatarGrad)
        }
    }
}

/** آواتار گرد تصویری — چهره سه‌بعدی از استخر آواتارها + حلقه گرادیان پایدار. */
@Composable
private fun ChatAvatar(resId: Int, gradient: List<Color>) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.5.dp, Brush.linearGradient(gradient), CircleShape)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = "آواتار",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
        )
    }
}

/** محتوای ویس — شکل موج + دکمه پخش + مدت زمان. */
@Composable
private fun VoiceContent(msg: ChatMessageEntity, mine: Boolean, p: ir.atiran.vizitor.ui.theme.VizitorPalette) {
    val tint = if (mine) p.onPrimary else NeonPurple
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (mine) Color(0x33FFFFFF) else NeonPurple.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayCircle, contentDescription = "پخش ویس", tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        // شکل موج مصنوعی (میله‌های نوبتی)
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(18.dp)
        ) {
            listOf(5, 11, 14, 8, 13, 6, 10, 4).forEach { h ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (mine) p.onPrimary.copy(alpha = 0.75f) else NeonPurple.copy(alpha = 0.6f))
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            msg.text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

/** محتوای ویدیو — تامی کوچک گرادیانی + دکمه پخش + مدت. */
@Composable
private fun VideoContent(msg: ChatMessageEntity) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2C0B4E), NeonPurple, Color(0xFFC99B2E))
                )
            )
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.PlayCircle, contentDescription = "پخش ویدیو",
            tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(34.dp)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xCC0B1220))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(msg.text, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = NeonGreen)
        }
    }
}

// ═════════════════════════ کمپوزر پیام ═════════════════════════

/** نوار ارسال — متن، ویس، استیکر و ویدیو؛ با نوار استیکرهای آماده. */
@Composable
private fun ChatComposer(
    locked: Boolean,
    onSendText: (String) -> Unit,
    onSendVoice: () -> Unit,
    onSendVideo: () -> Unit,
    onSticker: (String) -> Unit,
    onLongChatSettings: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var stickerOpen by remember { mutableStateOf(false) }
    val canSend = !locked && text.isNotBlank()

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0x10FFFFFF))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (stickerOpen) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("😂", "🔥", "👍", "❤️", "🎉", "🙏", "🤩", "😍", "🤝", "💪", "✨", "🥜").forEach { st ->
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Gold.copy(alpha = 0.25f), CircleShape)
                            .clickable(enabled = !locked) { onSticker(st) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(st, fontSize = 22.sp)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ویس / ویدیو / استیکر
            ComposerIcon(if (locked) Icons.Filled.Lock else Icons.Filled.Mic, "ارسال ویس", NeonGreen, !locked, onSendVoice)
            Spacer(Modifier.width(4.dp))
            ComposerIcon(if (locked) Icons.Filled.Lock else Icons.Filled.Videocam, "ارسال ویدیو", NeonPurple, !locked, onSendVideo)
            Spacer(Modifier.width(4.dp))
            ComposerIcon(Icons.Filled.Face, "استیکر", Gold, !locked) { stickerOpen = !stickerOpen }
            Spacer(Modifier.width(6.dp))
            // فیلد متن
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        if (locked) "گروه قفل است 🔒" else "پیامت را بنویس…",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                enabled = !locked,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    cursorColor = NeonPurple
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            // دکمه ارسال دایره‌ای
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) Brush.linearGradient(listOf(NeonGreen.copy(alpha = 0.9f), Color(0xFF0FBF62)))
                        else Brush.linearGradient(listOf(Color(0x1FFFFFFF), Color(0x14FFFFFF)))
                    )
                    .border(1.dp, if (canSend) Color(0x8CFFFFFF) else Color(0x22FFFFFF), CircleShape)
                    .clickable(enabled = canSend) {
                        onSendText(text.trim())
                        text = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Send, contentDescription = "ارسال",
                    tint = if (canSend) Color(0xFF04150C) else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun ComposerIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = if (enabled) 0.14f else 0.06f))
            .border(1.dp, tint.copy(alpha = if (enabled) 0.4f else 0.15f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = if (enabled) tint else TextSecondary, modifier = Modifier.size(17.dp))
    }
}

// ═════════════════════════ کارت ثبت‌نام ═════════════════════════

/** فرم ورود به گفتگو — اعتبارسنجی لاتین برای نام‌کاربری و رمز. */
@Composable
private fun RegistrationCard(onSubmit: (String, String, String, String, Int) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var avatarIdx by remember { mutableStateOf(0) }

    val phoneOk = phone.trim().replace('۰', '0').filter { it.isDigit() }.length >= 10
    val usernameOk = username.matches(Regex("^[A-Za-z][A-Za-z0-9_.]{2,19}$"))
    val passwordOk = run {
        val asciiOk = password.matches(Regex("^[\\x21-\\x7E]{6,}$"))
        val mixOk = password.any { it.isLetter() } && password.any { it.isDigit() }
        asciiOk && mixOk
    }
    val ready = fullName.isNotBlank() && phoneOk && usernameOk && passwordOk

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .royalBorder()
    ) {
        Column {
            RoyalHeader(text = "ورود به اتاق گفتگو", icon = Icons.Filled.Lock)
            Spacer(Modifier.height(6.dp))
            Text(
                "برای ورود، اطلاعاتت را ثبت کن — رمز فقط از حروف/اعداد/نمادهای انگلیسی (بدون فارسی) قبول است.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            RegField(fullName, { fullName = it }, "نام و نام‌خانوادگی", KeyboardType.Text, false)
            Spacer(Modifier.height(7.dp))
            RegField(phone, { phone = it }, "شماره تماس (موبایل)", KeyboardType.Phone, false)
            Spacer(Modifier.height(7.dp))
            RegField(username, { username = it }, "نام کاربری انگلیسی (مثل milad.y)", KeyboardType.Ascii, false)
            if (username.isNotBlank() && !usernameOk) {
                HintDanger("نام‌کاربری: فقط حروف/اعداد انگلیسی با . یا _ — شروع با حرف، ۳ تا ۲۰ نویسه")
            }
            Spacer(Modifier.height(7.dp))
            RegField(password, { password = it }, "رمز ورود (حداقل ۶ نویسه — حروف+عدد انگلیسی)", KeyboardType.Password, true)
            if (password.isNotBlank() && !passwordOk) {
                HintDanger("رمز: حداقل ۶ نویسه از حروف/اعداد/نمادهای انگلیسی، با حداقل یک حرف و یک عدد — بدون نویسه فارسی")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "انتخاب آواتار:",
                style = MaterialTheme.typography.labelMedium,
                color = Gold
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MyAvatarChoices.forEachIndexed { i, res ->
                    val selected = avatarIdx == i
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (selected) Gold else Color(0x33FFFFFF),
                                CircleShape
                            )
                            .clickable { avatarIdx = i }
                            .padding(if (selected) 3.dp else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = "آواتار ${i + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(if (selected) 44.dp else 48.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NeonGreenButton(
                text = "ورود به اتاق گفتگو 🎉",
                enabled = ready,
                onClick = { onSubmit(fullName.trim(), phone.trim(), username.trim(), password, avatarIdx) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RegField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    isPassword: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPurple,
            unfocusedBorderColor = Color(0x33FFFFFF),
            focusedLabelColor = NeonPurple,
            cursorColor = NeonPurple
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HintDanger(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = DangerRed,
        modifier = Modifier.padding(top = 3.dp)
    )
}

// ═════════════════════════ دیالوگ عملیات پیام ═════════════════════════

@Composable
private fun MessageActionDialog(
    msg: ChatMessageEntity,
    isMine: Boolean,
    isAdmin: Boolean,
    isBlocked: Boolean,
    onPin: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عملیات روی پیام") },
        text = {
            Column {
                if (isAdmin) {
                    TextButton(
                        onClick = { onPin(!msg.pinned); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (msg.pinned) Icons.Filled.BookmarkBorder else Icons.Filled.Bookmark,
                            contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (msg.pinned) "برداشتن سنجاق از بالا" else "سنجاق کردن در بالای گفتگو 📌", color = Gold)
                    }
                }
                if (!isMine && isAdmin) {
                    TextButton(
                        onClick = { onBlock(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isBlocked) "رفع مسدودیت «» ${msg.senderUsername} »" else "مسدود کردن «» ${msg.senderUsername} » 🚫", color = DangerRed)
                    }
                }
                if (isMine || isAdmin) {
                    TextButton(
                        onClick = { onDelete(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("حذف پیام", color = DangerRed)
                    }
                }
                if (!isAdmin && !isMine) {
                    Text("برای این پیام گزینه‌ای در دسترس نیست.", color = TextSecondary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

// ═════════════════════════ دیالوگ تنظیمات مدیر ═════════════════════════

/** مدیریت اتاق گفتگو — ورود بی‌پروا (بدون هیچ نشانه)، قفل موقت، عدم نمایش آیدی، تغییر رمز. */
@Composable
private fun AdminSheetDialog(
    groupLocked: Boolean,
    hideContact: Boolean,
    isAdmin: Boolean,
    onToggleLeft: (Boolean) -> Unit,
    onToggleHide: (Boolean) -> Unit,
    onUnlockAdmin: (String, String) -> Boolean,
    onChangePass: (String) -> Boolean,
    onToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var u by remember { mutableStateOf("") }
    var w by remember { mutableStateOf("") }
    var showChangePass by remember { mutableStateOf(false) }
    var np1 by remember { mutableStateOf("") }
    var np2 by remember { mutableStateOf("") }
    var badLogin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مدیریت اتاق گفتگو") },
        text = {
            Column {
                if (!isAdmin) {
                    // فرم ورود مدیر — کاملاً بی‌نشانه
                    OutlinedTextField(
                        value = u,
                        onValueChange = { u = it; badLogin = false },
                        label = { Text("نام کاربری") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold, cursorColor = Gold, focusedLabelColor = Gold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = w,
                        onValueChange = { w = it; badLogin = false },
                        label = { Text("رمز ورود") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold, cursorColor = Gold, focusedLabelColor = Gold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (badLogin) {
                        Text(
                            "نام کاربری یا رمز نادرست است ❌",
                            style = MaterialTheme.typography.labelSmall,
                            color = DangerRed,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    NeonGreenButton(
                        text = "ورود به مدیریت",
                        onClick = {
                            val ok = onUnlockAdmin(u.trim(), w)
                            if (!ok) { badLogin = true } else { onToast("حالت مدیر فعال شد 👑") }
                            u = ""; w = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("👑 حالت مدیر فعال است", color = NeonGreen, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    AdminSwitchRow(
                        title = "قفل موقت گروه",
                        subtitle = "تا باز شدن، هیچ‌کس نمی‌تواند پیام بفرستد",
                        icon = if (groupLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        checked = groupLocked,
                        enabled = true,
                        onChange = onToggleLeft
                    )
                    AdminSwitchRow(
                        title = "عدم نمایش شماره/آیدی ویزیتورها",
                        subtitle = "در پیام‌ها فقط عنوان «ویزیتور آتیران» نمایش داده می‌شود",
                        icon = Icons.Filled.Face,
                        checked = hideContact,
                        enabled = true,
                        onChange = onToggleHide
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { showChangePass = !showChangePass }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showChangePass) "بستن فرم تغییر رمز" else "تغییر رمز مدیر 🔑", color = Gold)
                    }
                    if (showChangePass) {
                        OutlinedTextField(
                            value = np1,
                            onValueChange = { np1 = it },
                            label = { Text("رمز جدید مدیر (حداقل ۴ نویسه)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold, cursorColor = Gold, focusedLabelColor = Gold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = np2,
                            onValueChange = { np2 = it },
                            label = { Text("تکرار رمز جدید") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold, cursorColor = Gold, focusedLabelColor = Gold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        NeonGreenButton(
                            text = "ذخیره رمز جدید ✅",
                            onClick = {
                                val m = when {
                                    np1.length < 4 -> "رمز باید حداقل ۴ نویسه باشد ❌"
                                    np1 != np2 -> "تکرار رمز یکسان نیست ❌"
                                    onChangePass(np1) -> { showChangePass = false; np1 = ""; np2 = ""; "رمز مدیر تغییر کرد ✅" }
                                    else -> "خطا در ذخیره رمز ❌"
                                }
                                onToast(m)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun AdminSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) NeonPurple else TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonPurple
            )
        )
    }
}

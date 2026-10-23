package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiChatMessage
import com.example.model.AiChatSession
import com.example.ui.DashboardUiState
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.RoyalBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun sanitizeMarkdown(text: String): String {
    return text
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("#{1,6}\\s*"), "")
        .replace("`", "")
        .trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    uiState: DashboardUiState,
    chatMessages: List<AiChatMessage>,
    chatSessions: List<AiChatSession> = emptyList(),
    activeSessionId: String = "",
    aiCredits: Int,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    onUpgradeClicked: () -> Unit,
    onClearHistory: () -> Unit = {},
    onNewSession: () -> Unit = {},
    onSelectSession: (String) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val lastMessageTextLength = chatMessages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(chatMessages.size, lastMessageTextLength) {
        if (chatMessages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("ai_chat_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Pockethome AI Advisor", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("🪙 AI Tokens Left: ${aiCredits.coerceAtLeast(0)} / 20,000", style = MaterialTheme.typography.labelSmall, color = MoneyGreen)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isProUser) {
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("chat_history_btn")
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Chat History", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(
                            onClick = onNewSession,
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Text("New Chat ➕", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = RoyalBlue))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!uiState.isProUser) {
                    // PRO Lock Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AI Chat Advisor is a PRO Feature",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unlock unlimited financial guidance, AI transaction queries, and 20,000 monthly AI tokens with Pockethome PRO for just ₹15/mo.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onUpgradeClicked,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("unlock_ai_chat_btn")
                        ) {
                            Text("Unlock PRO for ₹15/mo ➔", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                } else {
                    if (chatMessages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Pockethome AI se kuch bhi poochhein!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("e.g. 'Mera is mahine ka kharcha kahan zyada hua?' ya 'How to start SIP?'", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isUser = msg.sender == "USER"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        ),
                                        color = if (isUser) RoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.widthIn(max = 300.dp)
                                    ) {
                                        Text(
                                            text = sanitizeMarkdown(msg.text),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isProUser) {
                if (aiCredits <= 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🚫 20,000 Monthly AI Tokens limit poori ho chuki hai. Agle billing cycle me naye tokens milenge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (aiCredits > 0) "Apna financial sawal poochhein..." else "Token limit reached (0/20,000)") },
                            modifier = Modifier.weight(1f).testTag("ai_chat_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            enabled = aiCredits > 0
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isGenerating && aiCredits > 0) {
                                    onSendMessage(inputText.trim())
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (aiCredits > 0 && !isGenerating) RoyalBlue else Color.Gray)
                                .testTag("ai_chat_send_button"),
                            enabled = !isGenerating && aiCredits > 0
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat History & Sessions 📜", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    IconButton(onClick = {
                        onNewSession()
                        showHistorySheet = false
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New", tint = RoyalBlue)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onNewSession()
                        showHistorySheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start New Chat ➕", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (chatSessions.isEmpty()) {
                    Text("Koi purani chat history nahi mili.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val sdf = remember { SimpleDateFormat("d MMM, hh:mm a", Locale.getDefault()) }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
                    ) {
                        items(chatSessions) { session ->
                            val isActive = session.id == activeSessionId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSession(session.id)
                                        showHistorySheet = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) RoyalBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, RoyalBlue) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${sdf.format(Date(session.timestamp))} • ${session.messages.size} messages",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isActive) {
                                            Surface(
                                                color = RoyalBlue,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Active", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        IconButton(onClick = { onDeleteSession(session.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

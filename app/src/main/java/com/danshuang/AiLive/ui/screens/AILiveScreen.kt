package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.ui.components.QuickActionVectorData
import com.danshuang.AiLive.ui.theme.AiLiveTheme

// 快捷功能数据已迁移到通用组件
// 保留密封类用于类型安全，但转换为通用数据类使用
sealed class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color
) {
    data object StartLive : QuickActionItem(
        "开始直播",
        Icons.Default.PlayArrow,
        Color(0xFF4CAF50)
    )

    data object ScheduleLive : QuickActionItem(
        "预约直播",
        Icons.Default.DateRange,
        Color(0xFF2196F3)
    )

    data object LiveHistory : QuickActionItem(
        "直播记录",
        Icons.AutoMirrored.Filled.List,
        Color(0xFF9C27B0)
    )

    data object LiveSettings : QuickActionItem(
        "直播设置",
        Icons.Default.Settings,
        Color(0xFFFF9800)
    )

    data object VoiceSettings : QuickActionItem(
        "语音设置",
        Icons.Default.Call,
        Color(0xFFE91E63)
    )

    data object ContentTemplate : QuickActionItem(
        "内容模板",
        Icons.Default.Edit,
        Color(0xFF00BCD4)
    )

    data object AudienceAnalytics : QuickActionItem(
        "观众分析",
        Icons.Default.Info,
        Color(0xFF795548)
    )

    data object InteractMode : QuickActionItem(
        "互动模式",
        Icons.Default.Email,
        Color(0xFF607D8B)
    )

    data object AIHost : QuickActionItem(
        "AI主播",
        Icons.Default.Person,
        Color(0xFF3F51B5)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AILiveScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                title = {
                    Text("AI直播中心")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 头部欢迎区域
            WelcomeSection()
            
            // 快捷操作组标题
            Text(
                text = "快捷操作",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 快捷操作网格
            QuickActionsGrid()
        }
    }
}

@Composable
private fun WelcomeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "AI主播",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "智能AI主播",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "为你带来24小时不间断的精彩直播内容",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickActionsGrid() {
    val quickActions = listOf(
        QuickActionItem.StartLive,
        QuickActionItem.ScheduleLive,
        QuickActionItem.LiveHistory,
        QuickActionItem.LiveSettings,
        QuickActionItem.VoiceSettings,
        QuickActionItem.ContentTemplate,
        QuickActionItem.AudienceAnalytics,
        QuickActionItem.InteractMode
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quickActions) { action ->
            QuickActionCard(action)
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickActionItem) {
    val actionData = QuickActionVectorData(
        title = action.title,
        icon = action.icon,
        backgroundColor = action.backgroundColor,
        onClick = { /* TODO: 实现点击逻辑 */ }
    )
    
    Card(
        onClick = actionData.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = actionData.backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionData.icon,
                    contentDescription = actionData.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = actionData.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AILiveScreenPreview() {
    AiLiveTheme {
        AILiveScreen()
    }
}
package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.painterResource
import com.danshuang.AiLive.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.ui.components.QuickActionVectorData
import com.danshuang.AiLive.ui.components.QuickActionVectorCard
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import com.danshuang.AiLive.data.VoiceSettings
import com.danshuang.AiLive.ui.components.QuickActionData
import com.danshuang.AiLive.ui.components.QuickActionCard
import com.danshuang.AiLive.config.AppConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelScreen(navigationState: NavigationState) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                title = {
                    Text("主播模型")
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 快捷功能入口
            item {
                QuickActionsSection(navigationState)
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuickActionsSection(navigationState: NavigationState) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "门店设置",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val quickShopSettingActions = listOf(
            QuickActionVectorData(
                title = "门店品牌",
                icon = Icons.Default.Person,
                backgroundColor = Color(0xFF2196F3),
                onClick = {
                    navigationState.navigateToContentEdit("shop_name")
                }
            ),
            QuickActionVectorData(
                title = "产品业务",
                icon = Icons.Default.Person,
                backgroundColor = Color(0xFF4CAF50),
                onClick = {
                    navigationState.navigateToContentEdit("business_scope")
                }
            ),
            QuickActionVectorData(
                title = "推广活动",
                icon = Icons.Default.MoreVert,
                backgroundColor = Color(0xFFFF9800),
                onClick = {
                    navigationState.navigateToContentEdit("shop_activity")
                }
            ),
            QuickActionVectorData(
                title = "客户群体",
                icon = Icons.Default.PlayArrow,
                backgroundColor = Color(0xFF9C27B0),
                onClick = {
                    navigationState.navigateToContentEdit("customer_scope")
                }
            ),
            QuickActionVectorData(
                title = "门店地址",
                icon = Icons.Default.Home,
                backgroundColor = Color(0xFF2196F3),
                onClick = {
                    navigationState.navigateToContentEdit("common_knowledge")
                }
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(quickShopSettingActions) { action ->
                QuickActionVectorCard(
                    data = action,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "直播话术循环",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val quickLiveScriptActions = listOf(
            QuickActionData(
                title = "开场 >",
                icon = R.drawable.counter_1_24px,
                backgroundColor = Color(0xFF2196F3),
                onClick = {
                    navigationState.navigateToLiveScript("WELCOME")
                }
            ),
            QuickActionData(
                title = "品宣 >",
                icon = R.drawable.counter_2_24px,
                backgroundColor = Color(0xFF4CAF50),
                onClick = {
                    navigationState.navigateToLiveScript("ROOM")
                }
            ),
            QuickActionData(
                title = "讲品 >",
                icon = R.drawable.counter_3_24px,
                backgroundColor = Color(0xFFFF9800),
                onClick = {
                    navigationState.navigateToLiveScript("PRODUCT")
                }
            ),
            QuickActionData(
                title = "留咨 >",
                icon = R.drawable.counter_4_24px,
                backgroundColor = Color(0xFF9C27B0),
                onClick = {
                    navigationState.navigateToLiveScript("KEEP")
                }
            ),
            QuickActionData(
                title = "成交 >",
                icon = R.drawable.counter_5_24px,
                backgroundColor = Color(0xFF2196F3),
                onClick = {
                    navigationState.navigateToLiveScript("ORDER")
                }
            ),
            QuickActionData(
                title = "结束语",
                icon = R.drawable.counter_6_24px,
                backgroundColor = Color(0xFF4CAF50),
                onClick = {
                    navigationState.navigateToLiveScript("THANKS")
                }
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(quickLiveScriptActions) { action ->
                QuickActionCard(
                    action = action,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI主播会按照1->6顺序播放话术库，\n同项中有多条内容将随机播报其中的一条",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, top = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "控制参数",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val quickLiveSettingActions = listOf(
            QuickActionVectorData(
                title = "招呼设置",
                icon = Icons.AutoMirrored.Filled.Send,
                backgroundColor = Color(0xFF2196F3),
                onClick = {
                    navigationState.navigateToGreetingSettings()
                }
            ),
            QuickActionVectorData(
                title = "呢称限制",
                icon = Icons.Default.Warning,
                backgroundColor = Color(0xFF4CAF50),
                onClick = {
                    navigationState.navigateToNicknameRestrictions()
                }
            ),
            QuickActionVectorData(
                title = "评论限制",
                icon = Icons.Default.Warning,
                backgroundColor = Color(0xFFFF9800),
                onClick = {
                    navigationState.navigateToCommentRestrictions()
                }
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quickLiveSettingActions) { action ->
                QuickActionVectorCard(
                    data = action
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "语音管理",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val quickVoiceCloningActions = listOf(
            QuickActionVectorData(
                title = "音色管理",
                icon = Icons.Default.Face,
                backgroundColor = Color(0xFF2196F3),
                onClick = {
                    navigationState.navigateToVoiceManagement()
                }
            ),
//
//            QuickActionVectorData(
//                title = "声音克隆",
//                icon = Icons.Default.Face,
//                backgroundColor = Color(0xFF2196F3),
//                onClick = {
//                    navigationState.navigateToVoiceCloning()
//                }
//            ),


            QuickActionVectorData(
                title = "语音调速",
                icon = Icons.Default.Phone,
                backgroundColor = Color(0xFF9C27B0),
                onClick = {
                    navigationState.navigateToVoiceSettings()
                }
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quickVoiceCloningActions) { action ->
                QuickActionVectorCard(
                    data = action
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModelScreenPreview() {
    AiLiveTheme {
        ModelScreen(navigationState = remember { NavigationState() })
    }
}
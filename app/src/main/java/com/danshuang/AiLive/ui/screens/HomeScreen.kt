package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.danshuang.AiLive.R
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.navigation.Screen
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.api.service.DouyinApiService
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.config.AppConfig
import com.danshuang.AiLive.service.LiveChattingService
import com.danshuang.AiLive.service.LiveChattingService.Companion.CHATTING_STATE_PLAYING
import com.danshuang.AiLive.service.LiveRoomPullingService
import com.danshuang.AiLive.service.LiveRoomPullingService.Companion.PULLING_STATE_MONITORING
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.danshuang.AiLive.ui.theme.backgroundLight

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavigationState) {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }
    val douyinApiService = remember { DouyinApiService(tokenManager, AppConfig.getApiBaseUrl(context), context) }
    val userProfile by loginRepository.userProfile.collectAsState(initial = com.danshuang.AiLive.data.model.UserProfile())
    val authToken by loginRepository.authToken.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val pullingState by LiveRoomPullingService.pullingStateFlow.collectAsState()
    val liveChattingServiceRunning by LiveChattingService.isServiceRunning.collectAsState()
    var aiLiveEnabled by remember { mutableStateOf(false) }

    // 根据服务运行状态更新UI状态
    LaunchedEffect(liveChattingServiceRunning) {
        aiLiveEnabled = liveChattingServiceRunning
    }
    // 登录时获取绑定信息
    LaunchedEffect(authToken) {
        authToken?.let { token ->
            try {
                val response = douyinApiService.getBinding(token)
                if (response.isSuccess && response.data != null) {
                    loginRepository.updateBoundAccountFromBindingData(response.data)
                }
            } catch (e: Exception) {
                // 静默失败，不影响正常使用
            }
        }
    }
    
    // 自动轮播
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000)
            val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                title = {
                    Text("首页")
                }
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Banner轮播图
            item {
                BannerSection(pagerState = pagerState)
            }

            // 快捷功能入口
        item {
            QuickActionsSection(navController = navController, userProfile = userProfile, pullingState, aiLiveEnabled)
        }

            // 核心功能标题
            item {
                SectionTitle(title = "核心功能")
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    for (row in 0 until 3) {
                        Row {
                            for (col in 0 until 2) {
                                val index = row * 2 + col
                                if (index < noteCards.size) {
                                    NoteCardItem(
                                        card = noteCards[index],
                                        navController = navController,
                                        modifier = Modifier
                                            .weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BannerSection(pagerState: androidx.compose.foundation.pager.PagerState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (page) {
                                0 -> Color(0xFF4CAF50)
                                1 -> Color(0xFF2196F3)
                                else -> Color(0xFFFF9800)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (page) {
                                0 -> "AI门店营销利器"
                                1 -> "实时互动直播"
                                else -> "为美业直播而生"
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (page) {
                                0 -> "智能对话，贴心服务"
                                1 -> "实时推理，智慧互动"
                                else -> "整合行业直播数据"
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 指示器
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) {
                    Color.White
                } else {
                    Color.White.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(navController: NavigationState, userProfile: com.danshuang.AiLive.data.model.UserProfile, pullingState : Int, aiLiveEnabled : Boolean) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "直播账户",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LiveAccountItem(navController = navController, userProfile = userProfile, pullingState = pullingState, aiLiveEnabled = aiLiveEnabled)
    }
}

@Composable
private fun LiveAccountItem(navController: NavigationState, userProfile: com.danshuang.AiLive.data.model.UserProfile, pullingState : Int, aiLiveEnabled : Boolean) {
    val boundAccount = userProfile.boundAccount
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        if (boundAccount != null && boundAccount.avatar.isNotEmpty()) {
            // 显示绑定账户的头像
            AsyncImage(
                model = boundAccount.avatar,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                fallback = painterResource(id = R.drawable.ic_douyin_logo),
                error = painterResource(id = R.drawable.ic_douyin_logo)
            )
        } else {
            // 显示默认头像
            Image(
                painter = painterResource(id = R.drawable.ic_douyin_logo),
                contentDescription = "默认头像",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 用户信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (boundAccount != null && boundAccount.nickname.isNotEmpty()) {
                    boundAccount.nickname
                } else {
                    "未绑定账户"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            val color = if (aiLiveEnabled == true) Color(0xFF4CAF50) else if (pullingState == PULLING_STATE_MONITORING) Color(0xFF2196F3) else Color.Gray
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            color
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                var text = if (aiLiveEnabled == true) "AI运行中" else "AI未开启"
                text = if (pullingState == PULLING_STATE_MONITORING) "$text / 直播中" else "$text / 直播间未连接"
                Text(
                    text = text,
                    fontSize = 12.sp,
                    color = color
                )
            }
        }
        // 修改按钮
        Button(
            onClick = { navController.navigateToAccountBinding() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (boundAccount != null) "更换账户" else "绑定账户",
                fontSize = 14.sp
            )
        }
    }
}

// 1. 数据类
data class NoteCard(
    val tag: String,
    val title: String,
    val subtitle: String,
    val url: String
)

// 2. 示例数据
private val noteCards = listOf(
    NoteCard("model", "门店大模型", "智能对话，贴心服务", "https://www.huichuang100.com/android/v2/z/ai-model.png"),
    NoteCard("knowledge", "知识库", "专用解答，行业知识驱动", "https://www.huichuang100.com/android/v2/z/kb-lib.png"),
    NoteCard("voice_cloning", "音色管理", "选择匹配业务风格的音色，提升辨识度", "https://www.huichuang100.com/android/v2/z/voice-cloning.png"),
    NoteCard("live_params", "直播参数", "对话过程控制优化", "https://www.huichuang100.com/android/v2/z/live-params.png"),
    NoteCard("live_review", "直播回顾", "回看每场AI对话，同步优化", "https://www.huichuang100.com/android/v2/z/ai-review.png"),
    NoteCard("card_activation", "卡密续费", "卡密兑换，账户续费", "https://www.huichuang100.com/android/v2/z/account-rights.png")
)

// 3. 卡片组件
@Composable
fun NoteCardItem(card: NoteCard, navController: NavigationState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .width(0.dp)
            .aspectRatio(1f)
            .padding(8.dp)
            .clickable {
                // 根据卡片tag导航到相应页面
                when (card.tag) {
                    "model" -> navController.navigateTo(Screen.Model)
                    "knowledge" -> navController.navigateTo(Screen.Knowledge)
                    "voice_cloning" -> navController.navigateToWebView(
                        url = "/wap/addon/dsalive/pages/voice/index",
                        context,
                        title = "音色管理"
                    )
                    "live_params" -> navController.navigateTo(Screen.VoiceSettings)
                    //"live_review" -> navController.navigateTo(Screen.AILive)
                    "live_review" -> navController.navigateToLiveHistory()
                    "card_activation" -> navController.navigateToCardActivation()
                    else -> {} // 默认情况不执行任何操作
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundLight
        )
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(card.url),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = card.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Text(
                text = card.subtitle,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AiLiveTheme {
        HomeScreen(navController = remember { NavigationState() })
    }
}
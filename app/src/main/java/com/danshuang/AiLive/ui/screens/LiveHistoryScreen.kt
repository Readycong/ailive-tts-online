package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.danshuang.AiLive.api.service.LiveRoomApiService
import com.danshuang.AiLive.config.AppConfig
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.LiveInteractionRecord
import com.danshuang.AiLive.data.model.LiveSession
import com.danshuang.AiLive.data.model.LiveStatistics
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }
    val liveRoomApiService = remember {
        LiveRoomApiService(tokenManager, AppConfig.getApiBaseUrl(context), context)
    }

    val authToken by loginRepository.authToken.collectAsState(initial = null)

    var sessions by remember { mutableStateOf<List<LiveSession>>(emptyList()) }
    var statistics by remember { mutableStateOf<LiveStatistics?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSession by remember { mutableStateOf<LiveSession?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // 分页状态
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    
    // LazyColumn滚动状态
    val listState = rememberLazyListState()

    // 默认时间范围：null表示不限制时间
    var startTime by remember { mutableStateOf<String?>(null) }
    var endTime by remember { mutableStateOf<String?>(null) }
    
    // 显示的日期文本
    val dateRangeText = remember(startTime, endTime) {
        if (startTime != null && endTime != null) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                val start = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(startTime!!)
                val end = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(endTime!!)
                if (start != null && end != null) {
                    "${format.format(start)} ~ ${format.format(end)}"
                } else {
                    "全部"
                }
            } catch (e: Exception) {
                "全部"
            }
        } else {
            "全部"
        }
    }

    // 加载数据
    fun loadData(page: Int = 1, append: Boolean = false) {
        scope.launch {
            authToken?.let { token ->
                if (append) {
                    isLoadingMore = true
                } else {
                    isLoading = true
                }
                errorMessage = null
                try {
                    // 获取直播场次列表
                    val sessionResponse = liveRoomApiService.getLiveSessionList(
                        token = token,
                        startTime = startTime,
                        endTime = endTime,
                        page = page,
                        pageSize = 10
                    )

                    if (sessionResponse.isSuccess && sessionResponse.data != null) {
                        val newSessions = sessionResponse.data.data
                        sessions = if (append) {
                            sessions + newSessions
                        } else {
                            newSessions
                        }
                        currentPage = sessionResponse.data.currentPage
                        totalPages = sessionResponse.data.lastPage
                        hasMore = currentPage < totalPages
                    } else {
                        errorMessage = sessionResponse.msg
                    }

                    // 仅在首次加载时获取统计数据
                    if (!append) {
                        val statsResponse = liveRoomApiService.getLiveStatistics(
                            token = token,
                            startTime = startTime,
                            endTime = endTime
                        )

                        if (statsResponse.isSuccess && statsResponse.data != null) {
                            statistics = statsResponse.data
                        }
                    }

                } catch (e: Exception) {
                    errorMessage = "加载失败: ${e.message}"
                } finally {
                    isLoading = false
                    isLoadingMore = false
                }
            }
        }
    }
    
    // 加载下一页
    fun loadNextPage() {
        if (!isLoadingMore && hasMore) {
            loadData(page = currentPage + 1, append = true)
        }
    }
    
    // 监听滚动到底部
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= sessions.size - 2 && hasMore && !isLoadingMore) {
                    loadNextPage()
                }
            }
    }

    // 初始加载 - 等待authToken可用后自动加载
    LaunchedEffect(authToken) {
        authToken?.let {
            // 只在首次加载时执行
            if (sessions.isEmpty() && !isLoading) {
                loadData()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("直播场次记录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // 标题和日期筛选按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "直播记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 清除筛选按钮
                    if (startTime != null && endTime != null) {
                        TextButton(
                            onClick = {
                                startTime = null
                                endTime = null
                                currentPage = 1
                                loadData()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFFF5252)
                            )
                        ) {
                            Text(
                                text = "清除",
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    // 筛选日期按钮
                    Row(
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateRangeText,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "选择日期",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 统计卡片
            statistics?.let { stats ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem(
                            value = stats.totalCount.toString(),
                            label = "场次",
                            color = Color(0xFFFF5252)
                        )
                        
                        Divider(
                            modifier = Modifier
                                .height(50.dp)
                                .width(1.dp),
                            color = Color(0xFFE0E0E0)
                        )
                        
                        StatisticItem(
                            value = stats.totalUsePoint.toString(),
                            label = "消耗算力",
                            color = Color(0xFF000000)
                        )
                        
                        Divider(
                            modifier = Modifier
                                .height(50.dp)
                                .width(1.dp),
                            color = Color(0xFFE0E0E0)
                        )
                        
                        StatisticItem(
                            value = "¥${stats.totalBalanceData}",
                            label = "消耗金额",
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 直播场次列表
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无直播记录",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions) { session ->
                        LiveSessionCard(
                            session = session,
                            onClick = { selectedSession = session }
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    // 已加载全部数据提示
                    if (!hasMore && sessions.isNotEmpty()) {
                        item {
                            Text(
                                text = "已加载全部数据",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // 显示场次详情抽屉
    if (selectedSession != null) {
        LiveSessionDetailBottomSheet(
            session = selectedSession!!,
            liveRoomApiService = liveRoomApiService,
            token = authToken ?: "",
            onDismiss = { selectedSession = null }
        )
    }

    // 日期选择对话框（简化版）
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                startTime = start
                endTime = end
                currentPage = 1
                showDatePicker = false
                loadData()
            }
        )
    }
}

@Composable
fun StatisticItem(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun LiveSessionCard(session: LiveSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "直播间 #${session.roomId}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = session.statusName,
                    fontSize = 12.sp,
                    color = when (session.status) {
                        0 -> Color(0xFFFF5252) // 进行中 - 红色
                        1 -> Color(0xFF4CAF50) // 已结束 - 绿色
                        else -> Color.Gray
                    },
                    modifier = Modifier
                        .background(
                            when (session.status) {
                                0 -> Color(0xFFFFEBEE)
                                1 -> Color(0xFFE8F5E9)
                                else -> Color(0xFFF5F5F5)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "开始时间", value = session.startTime)
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(label = "结束时间", value = session.endTime)
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(label = "时长", value = formatDuration(session.duration))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "算力: ${session.usePoint}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "金额: ¥${session.balanceData ?: "0"}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSessionDetailBottomSheet(
    session: LiveSession,
    liveRoomApiService: LiveRoomApiService,
    token: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var interactionRecords by remember { mutableStateOf<List<LiveInteractionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 加载交互记录
    LaunchedEffect(session.roomId) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = liveRoomApiService.getLiveRoomHistory(token, session.roomId)
                if (response.isSuccess && response.data != null) {
                    interactionRecords = response.data
                } else {
                    errorMessage = response.msg
                }
            } catch (e: Exception) {
                errorMessage = "加载失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 16.dp)
        ) {
            // 标题和场次信息
            Text(
                text = "直播间 #${session.roomId}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.startTime,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "时长: ${formatDuration(session.duration)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            
            Text(
                text = "交互记录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (interactionRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无交互记录",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(interactionRecords) { record ->
                        InteractionRecordCard(record)
                    }
                }
            }
        }
    }
}

@Composable
fun InteractionRecordCard(record: LiveInteractionRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (record.method) {
                        "chat" -> "评论"
                        "greet" -> "招呼"
                        else -> record.method
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.createTime,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (record.liveUserNickname.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "用户: ${record.liveUserNickname}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (record.question.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = record.question,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            if (record.answer.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "回复: ${record.answer}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startTime: String, endTime: String) -> Unit
) {
    // 使用中国本地化设置
    val locale = Locale.CHINA
    val dateRangePickerState = rememberDateRangePickerState()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
    
    // 快捷选项选择状态
    var selectedQuickOption by remember { mutableStateOf<String?>(null) }
    
    // 处理快捷选项
    fun handleQuickOption(option: String) {
        selectedQuickOption = option
        val startCal = Calendar.getInstance(locale).apply {
            firstDayOfWeek = Calendar.MONDAY // 设置周一为第一天
        }
        val endCal = Calendar.getInstance(locale).apply {
            firstDayOfWeek = Calendar.MONDAY
        }
        
        when (option) {
            "昨天" -> {
                startCal.add(Calendar.DAY_OF_YEAR, -1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                
                endCal.add(Calendar.DAY_OF_YEAR, -1)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
            }
            "本周" -> {
                // 设置为本周一
                val dayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) {
                    6 // 周日需要回退6天到周一
                } else {
                    dayOfWeek - Calendar.MONDAY
                }
                startCal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                
                // 当前时间
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
            }
            "本月" -> {
                // 本月1号
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                
                // 当前时间
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
            }
        }
        
        // 设置日期选择器状态
        dateRangePickerState.setSelection(
            startDateMillis = startCal.timeInMillis,
            endDateMillis = endCal.timeInMillis
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f) // 增加宽度占比
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 快捷选项按钮组
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickDateChip(
                        label = "昨天",
                        selected = selectedQuickOption == "昨天",
                        onClick = { handleQuickOption("昨天") }
                    )
                    QuickDateChip(
                        label = "本周",
                        selected = selectedQuickOption == "本周",
                        onClick = { handleQuickOption("本周") }
                    )
                    QuickDateChip(
                        label = "本月",
                        selected = selectedQuickOption == "本月",
                        onClick = { handleQuickOption("本月") }
                    )
                }

                HorizontalDivider(color = Color(0xFFE0E0E0))

                // 日期范围选择器
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    title = {
                        Text(
                            text = "选择日期范围",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    headline = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateRangePickerState.selectedStartDateMillis?.let {
                                    SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(Date(it))
                                } ?: "开始日期",
                                fontSize = 14.sp,
                                color = if (dateRangePickerState.selectedStartDateMillis != null) 
                                    Color.Black else Color.Gray
                            )
                            Text(
                                text = "→",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = dateRangePickerState.selectedEndDateMillis?.let {
                                    SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(Date(it))
                                } ?: "结束日期",
                                fontSize = 14.sp,
                                color = if (dateRangePickerState.selectedEndDateMillis != null) 
                                    Color.Black else Color.Gray
                            )
                        }
                    },
                    showModeToggle = false
                )

                // 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val startMillis = dateRangePickerState.selectedStartDateMillis
                            val endMillis = dateRangePickerState.selectedEndDateMillis
                            
                            if (startMillis != null && endMillis != null) {
                                val startCal = Calendar.getInstance(Locale.CHINA).apply {
                                    firstDayOfWeek = Calendar.MONDAY
                                    timeInMillis = startMillis
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }
                                val endCal = Calendar.getInstance(Locale.CHINA).apply {
                                    firstDayOfWeek = Calendar.MONDAY
                                    timeInMillis = endMillis
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                }
                                onConfirm(
                                    dateFormat.format(startCal.time),
                                    dateFormat.format(endCal.time)
                                )
                            }
                        },
                        enabled = dateRangePickerState.selectedStartDateMillis != null && 
                                 dateRangePickerState.selectedEndDateMillis != null
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickDateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return buildString {
        if (hours > 0) append("${hours}小时")
        if (minutes > 0) append("${minutes}分")
        append("${secs}秒")
    }
}

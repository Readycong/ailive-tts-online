package com.danshuang.AiLive.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.zIndex

data class QAItem(
    val id: Int,
    val category: String,
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var currentSwipeId by remember { mutableStateOf<Int?>(null) }

    val categories = listOf("我的知识库", "行业库")

    // 美业领域问答数据
    val beautyAppQA = listOf(
        QAItem(1, "我的知识", "如何选择适合自己肤质的护肤品？", "根据肤质类型选择：干性皮肤选择滋润型，油性皮肤选择清爽控油型，混合性皮肤分区护理，敏感性皮肤选择温和无刺激产品。建议先做皮肤测试再购买。"),
        QAItem(2, "我的知识", "美容院预约系统如何使用？", "打开应用→选择服务项目→选择时间→选择美容师→确认预约→收到确认短信→到店扫码签到。支持在线支付和会员积分抵扣。"),
        QAItem(3, "我的知识", "美业会员卡有哪些优惠？", "会员卡享受项目折扣8.5折起，生日当月免费体验项目，积分兑换礼品，专属美容师服务，优先预约权，会员专享活动邀请。"),
        QAItem(4, "我的知识", "如何查看美容项目的效果对比？", "在【我的】页面点击【效果记录】，可查看前后对比照片，系统会自动标注时间和项目类型，支持分享朋友圈获得积分奖励。"),
        QAItem(5, "我的知识", "皮肤检测功能准确吗？", "采用AI皮肤检测技术，准确率达95%以上，可识别毛孔粗大、色素沉着、细纹、水分油分等8项指标，建议每月检测一次对比效果。"),
        QAItem(6, "我的知识", "美业小程序和APP有什么区别？", "小程序无需下载即开即用，适合轻度用户；APP功能更完整，支持离线缓存、消息推送、AR试妆等高级功能，会员数据互通。"),
        QAItem(7, "我的知识", "如何成为美业平台认证美容师？", "提交身份证+美容师资格证→平台审核→技能考核→背景调查→认证通过→获得专属二维码和认证标识→开始接单服务。"),
        QAItem(8, "我的知识", "美业直播带货可信吗？", "平台严格筛选合作品牌，所有产品均有质检报告，支持7天无理由退货，专业美容师直播讲解，用户可实时提问互动。")
    )

    val beautyIndustryQA = listOf(
        QAItem(9, "行业库", "2024年美业最新流行趋势是什么？", "轻医美项目持续火热，包括水光针、热玛吉、超声炮等；天然有机护肤品需求增长；个性化定制护肤方案；男性美容市场快速扩张。"),
        QAItem(10, "行业库", "美容院如何提高客户复购率？", "建立会员体系、定期回访关怀、个性化护肤方案、效果可视化对比、节日专属活动、老带新优惠、专业美容师一对一服务。"),
        QAItem(11, "行业库", "美业门店数字化转型需要哪些步骤？", "1.上线预约系统2.建立会员数据库3.引入AI皮肤检测4.开通线上商城5.运营私域社群6.数据分析优化7.培训员工数字化技能。"),
        QAItem(12, "行业库", "美容师职业发展路径有哪些？", "初级美容师→高级美容师→美容顾问→店长→区域经理→培训讲师→自主创业开店，或转向产品经理、品牌营销、培训教育等方向发展。"),
        QAItem(13, "行业库", "美业门店如何选址才赚钱？", "核心商圈人流量大但租金高，社区店稳定但增长慢，写字楼针对白领群体，商场店曝光度高，需根据目标客群和预算综合评估。"),
        QAItem(14, "行业库", "美业产品进货渠道有哪些？", "品牌官方代理、美博会采购、阿里巴巴1688、海外代购、OEM贴牌定制、加盟连锁品牌直供，注意查验产品资质和质检报告。"),
        QAItem(15, "行业库", "美业营销最有效的推广方式？", "短视频平台种草（抖音小红书）、KOL合作推广、会员转介绍奖励、节日主题活动、直播带货、私域社群运营、异业联盟合作。"),
        QAItem(16, "行业库", "美业门店如何控制成本？", "优化人员配置、精准库存管理、能耗监控、产品损耗控制、预约排班最大化、供应商谈判、数字化管理减少人工错误。"),
        QAItem(17, "行业库", "美业客户投诉如何处理？", "第一时间安抚情绪→详细了解问题→提出解决方案→及时补偿→跟进回访→记录案例培训→完善服务流程避免再次发生。")
    )

    // 合并所有问答数据
    var qaList by remember {
        mutableStateOf(beautyAppQA + beautyIndustryQA)
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<QAItem?>(null) }
    var editQuestion by remember { mutableStateOf("") }
    var editAnswer by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                title = {
                    Text("知识库")
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加问答"
                        )
                    }
                }
            )
        },

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 分类标签
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(category) }
                    )
                }
            }

            // 问答列表
            val filteredList = when (selectedTabIndex) {
                0 -> qaList.filter { it.category == "我的知识" }
                1 -> qaList.filter { it.category == "行业库" }
                else -> qaList
            }

            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(filteredList) { item ->
                    QACard(
                        item = item,
                        onDeleteClick = {
                            qaList = qaList.filterNot { it.id == item.id }
                            currentSwipeId = null
                        },
                        onEditClick = {
                            editingItem = item
                            editQuestion = item.question
                            editAnswer = item.answer
                            showEditDialog = true
                        },
                        currentSwipeId = currentSwipeId,
                        onSwipeStateChange = { newSwipeId ->
                            currentSwipeId = newSwipeId
                        }
                    )
                }
            }

            // 添加新项后自动滚动到底部
            val previousListSize = remember { mutableStateOf(qaList.size) }
            LaunchedEffect(qaList.size) {
                if (qaList.size > previousListSize.value) {
                    val lastItemIndex = filteredList.size - 1
                    if (lastItemIndex >= 0) {
                        listState.animateScrollToItem(lastItemIndex)
                    }
                }
                previousListSize.value = qaList.size
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingItem != null) "编辑问答" else "添加问答") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextField(
                        value = editQuestion,
                        onValueChange = { editQuestion = it },
                        label = { Text("问题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = editAnswer,
                        onValueChange = { editAnswer = it },
                        label = { Text("答案") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingItem != null) {
                            // 编辑现有问答
                            qaList = qaList.map {
                                if (it.id == editingItem!!.id) {
                                    it.copy(question = editQuestion, answer = editAnswer)
                                } else it
                            }
                        } else {
                            // 添加新问答
                            val newItem = QAItem(
                                id = qaList.maxOfOrNull { it.id }?.plus(1) ?: 1,
                                category = categories.getOrNull(selectedTabIndex) ?: "我的知识",
                                question = editQuestion,
                                answer = editAnswer
                            )
                            qaList = qaList + newItem
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false }
                ) {
                    Text("取消")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@Composable
fun QACard(
    item: QAItem,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    currentSwipeId: Int?,
    onSwipeStateChange: (Int?) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeOffset = -300f // 最大侧滑距离

    // 监听当前侧滑状态变化，如果其他卡片被侧滑，重置此卡片
    LaunchedEffect(currentSwipeId) {
        if (currentSwipeId != item.id) {
            offsetX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // 保证背景和卡片高度一致
    ) {
        // 背景删除按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onDeleteClick()
                        offsetX = 0f // 重置位置
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .zIndex(2f) // 保证按钮在最上层
                ) {
                    Text("删除")
                }
            }
        }

        // 前景卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .zIndex(3f) // 卡片在按钮之上，但通过offset让出空间
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // 滑动超过一半时显示删除按钮
                            val shouldOpen = offsetX < maxSwipeOffset / 2
                            if (shouldOpen) {
                                offsetX = maxSwipeOffset
                                onSwipeStateChange(item.id)
                            } else {
                                offsetX = 0f
                                onSwipeStateChange(null)
                            }
                        }
                    ) { change, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(maxSwipeOffset, 0f)
                        change.consume()

                        // 当开始滑动时，如果当前没有打开状态，则设置当前卡片为滑动状态
                        if (currentSwipeId == null && offsetX != 0f) {
                            onSwipeStateChange(item.id)
                        }
                    }
                }
                .clickable(
                    enabled = offsetX == 0f, // 只有在未侧滑状态下才能点击编辑
                    onClick = onEditClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KnowledgeScreenPreview() {
    AiLiveTheme {
        KnowledgeScreen()
    }
}
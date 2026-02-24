package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.config.AppConfig
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.ProductItem
import com.danshuang.AiLive.navigation.NavigationState
import kotlinx.coroutines.launch

// 颜色定义
private val SortTagBackground = Color(0xFFE8EAF6)
private val SortTagText = Color(0xFF3F51B5)
private val AiVoiceEnabledBackground = Color(0xFF2196F3)
private val AiVoiceDisabledBackground = Color(0xFFF44336)
private val PriceColor = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductListScreen(
    navController: NavigationState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 初始化API服务
    val tokenManager = remember { TokenManager(LoginRepository(context)) }
    val loginRepository = remember { LoginRepository(context) }
    val serverApiUtils = remember {
        ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
    }
    val wapBaseUrl = remember { AppConfig.Api.getWapBaseUrl(context) }
    
    // 页面状态
    var isLoading by remember { mutableStateOf(true) }
    var productList by remember { mutableStateOf<List<ProductItem>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalCount by remember { mutableIntStateOf(0) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var currentSwipeId by remember { mutableStateOf<Int?>(null) }
    
    // AI讲解切换Loading状态
    var aiVoiceLoadingProductId by remember { mutableStateOf<Int?>(null) }
    
    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingProduct by remember { mutableStateOf<ProductItem?>(null) }
    
    // 编号选择弹层状态
    var showSortSelector by remember { mutableStateOf(false) }
    var selectingSortProduct by remember { mutableStateOf<ProductItem?>(null) }
    var availableSortCount by remember { mutableIntStateOf(10) }
    val sheetState = rememberModalBottomSheetState()
    
    // 显示Snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarMessage = null
        }
    }
    
    // 加载商品列表
    fun loadProducts() {
        scope.launch {
            isLoading = true
            try {
                val response = serverApiUtils.fetchProductList(currentPage)
                if (response.isSuccess && response.data != null) {
                    productList = response.data.data
                    totalCount = response.data.total
                    // 确保编号数量至少与商品数量一致
                    if (availableSortCount < totalCount) {
                        availableSortCount = totalCount
                    }
                } else {
                    snackbarMessage = "加载失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "加载失败：${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 页面初始化
    LaunchedEffect(Unit) {
        loadProducts()
    }
    
    // 切换AI讲解状态
    fun toggleAiVoice(product: ProductItem) {
        scope.launch {
            aiVoiceLoadingProductId = product.id
            try {
                val newStatus = if (product.enableAiVoice == 1) 0 else 1
                val response = serverApiUtils.toggleProductAiVoice(product.id, newStatus)
                if (response.isSuccess) {
                    // 更新本地列表
                    productList = productList.map {
                        if (it.id == product.id) it.copy(enableAiVoice = newStatus) else it
                    }
                    snackbarMessage = if (newStatus == 1) "已开启AI讲解" else "已关闭AI讲解"
                } else {
                    snackbarMessage = "操作失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "操作失败：${e.message}"
            } finally {
                aiVoiceLoadingProductId = null
            }
        }
    }
    
    // 删除商品
    fun deleteProduct(product: ProductItem) {
        scope.launch {
            try {
                val response = serverApiUtils.deleteProduct(product.id)
                if (response.isSuccess) {
                    productList = productList.filter { it.id != product.id }
                    totalCount -= 1
                    snackbarMessage = "删除成功"
                } else {
                    snackbarMessage = "删除失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "删除失败：${e.message}"
            }
        }
        showDeleteDialog = false
        deletingProduct = null
    }
    
    // 更新商品排序
    fun updateProductSort(product: ProductItem, newSort: Int) {
        scope.launch {
            try {
                val response = serverApiUtils.updateProductSort(product.id, newSort)
                if (response.isSuccess) {
                    // 更新本地列表
                    productList = productList.map {
                        when {
                            it.id == product.id -> it.copy(sort = newSort)
                            it.sort == newSort && it.id != product.id -> it.copy(sort = 0) // 原占用者清空
                            else -> it
                        }
                    }
                    snackbarMessage = "编号已更新为 #${newSort.toString().padStart(2, '0')}"
                } else {
                    snackbarMessage = "更新失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "更新失败：${e.message}"
            }
        }
        showSortSelector = false
        selectingSortProduct = null
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("商品卡") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigateToProductEdit(0) }) {
                        Icon(Icons.Default.Add, contentDescription = "添加商品")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (productList.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("暂无商品", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { navController.navigateToProductEdit(0) }) {
                        Text("添加商品")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = productList,
                        key = { it.id }
                    ) { product ->
                        ProductCard(
                            product = product,
                            wapBaseUrl = wapBaseUrl,
                            currentSwipeId = currentSwipeId,
                            isAiVoiceLoading = aiVoiceLoadingProductId == product.id,
                            onSwipeStateChange = { currentSwipeId = it },
                            onCardClick = { navController.navigateToProductEdit(product.id) },
                            onSortClick = {
                                selectingSortProduct = product
                                showSortSelector = true
                            },
                            onAiVoiceClick = { toggleAiVoice(product) },
                            onDeleteClick = {
                                deletingProduct = product
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog && deletingProduct != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletingProduct = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除商品「${deletingProduct?.productName}」吗？此操作不可恢复。") },
            confirmButton = {
                Button(
                    onClick = { deletingProduct?.let { deleteProduct(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deletingProduct = null
                }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 编号选择弹层
    if (showSortSelector && selectingSortProduct != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSortSelector = false
                selectingSortProduct = null
            },
            sheetState = sheetState
        ) {
            ProductSortSelectorContent(
                currentProduct = selectingSortProduct!!,
                productList = productList,
                availableSortCount = availableSortCount,
                onSortSelect = { newSort ->
                    selectingSortProduct?.let { updateProductSort(it, newSort) }
                },
                onAddMoreSorts = {
                    availableSortCount += 5
                }
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductItem,
    wapBaseUrl: String,
    currentSwipeId: Int?,
    isAiVoiceLoading: Boolean = false,
    onSwipeStateChange: (Int?) -> Unit,
    onCardClick: () -> Unit,
    onSortClick: () -> Unit,
    onAiVoiceClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeOffset = -200f
    
    // 当其他卡片被滑动时，重置当前卡片
    LaunchedEffect(currentSwipeId) {
        if (currentSwipeId != product.id) {
            offsetX = 0f
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 背景删除按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red, RoundedCornerShape(12.dp))
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White
                )
            }
        }
        
        // 前景卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            onSwipeStateChange(product.id)
                        },
                        onDragEnd = {
                            val shouldOpen = offsetX < maxSwipeOffset / 2
                            offsetX = if (shouldOpen) maxSwipeOffset else 0f
                        }
                    ) { change, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(maxSwipeOffset, 0f)
                        change.consume()
                    }
                }
                .clickable(enabled = offsetX == 0f) { onCardClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // 商品图片
                    val fullImageUrl = product.getFullCoverImageUrl(wapBaseUrl)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fullImageUrl != null) {
                            AsyncImage(
                                model = fullImageUrl,
                                contentDescription = product.productName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "占位图",
                                modifier = Modifier.size(32.dp),
                                tint = Color.LightGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 商品信息
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = product.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = product.description,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.displayPrice,
                                color = PriceColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = product.stockText,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = product.statusText,
                                color = if (product.isOnline == 1) Color(0xFF4CAF50) else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // 序号标签（左上角）
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onSortClick() },
                    shape = RoundedCornerShape(4.dp),
                    color = SortTagBackground
                ) {
                    Text(
                        text = product.sortDisplayText,
                        color = SortTagText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // AI讲解标签（右上角）
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable(enabled = !isAiVoiceLoading) { onAiVoiceClick() },
                    shape = RoundedCornerShape(4.dp),
                    color = if (product.enableAiVoice == 1) AiVoiceEnabledBackground else AiVoiceDisabledBackground
                ) {
                    if (isAiVoiceLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = product.aiVoiceText,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductSortSelectorContent(
    currentProduct: ProductItem,
    productList: List<ProductItem>,
    availableSortCount: Int,
    onSortSelect: (Int) -> Unit,
    onAddMoreSorts: () -> Unit
) {
    // 构建已使用的编号映射
    val usedSortMap = remember(productList) {
        productList.associate { it.sort to it.id }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "选择商品编号：",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (sortNum in 1..availableSortCount) {
                val isCurrentProduct = currentProduct.sort == sortNum
                val isUsedByOther = usedSortMap.containsKey(sortNum) && usedSortMap[sortNum] != currentProduct.id
                
                SortChip(
                    sortNumber = sortNum,
                    isSelected = isCurrentProduct,
                    isUsedByOther = isUsedByOther,
                    onClick = { onSortSelect(sortNum) }
                )
            }
            
            // 添加更多编号按钮
            Surface(
                modifier = Modifier.clickable { onAddMoreSorts() },
                shape = RoundedCornerShape(8.dp),
                color = Color.LightGray.copy(alpha = 0.3f)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加更多",
                    modifier = Modifier.padding(12.dp),
                    tint = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SortChip(
    sortNumber: Int,
    isSelected: Boolean,
    isUsedByOther: Boolean,
    onClick: () -> Unit
) {
    val displayText = "#${sortNumber.toString().padStart(2, '0')}"
    
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> SortTagText
            else -> SortTagBackground
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                color = if (isSelected) Color.White else SortTagText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (isUsedByOther) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已使用",
                    modifier = Modifier.size(14.dp),
                    tint = if (isSelected) Color.White else SortTagText
                )
            }
        }
    }
}

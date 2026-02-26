package com.danshuang.AiLive.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.config.AppConfig
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.navigation.NavigationState
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

// 颜色定义
private val AiDescriptionBackground = Color(0xFFE3F2FD)
private val SwitchEnabledColor = Color(0xFF4CAF50)
private val CardElevation = 2.dp
private val SectionTitleColor = Color(0xFF1976D2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    navController: NavigationState,
    productId: Int // 0表示新增，>0表示编辑
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    
    // 初始化API服务
    val tokenManager = remember { TokenManager(LoginRepository(context)) }
    val loginRepository = remember { LoginRepository(context) }
    val serverApiUtils = remember {
        ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
    }
    val wapBaseUrl = remember { AppConfig.Api.getWapBaseUrl(context) }
    
    val isEditMode = productId > 0
    
    // 页面状态
    var isLoading by remember { mutableStateOf(isEditMode) }
    var isSaving by remember { mutableStateOf(false) }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var isGeneratingTts by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    // 表单数据
    var productName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var coverImage by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var isOnline by remember { mutableStateOf(true) }
    var enableAiVoice by remember { mutableStateOf(true) }
    var weight by remember { mutableIntStateOf(1) }
    var stock by remember { mutableIntStateOf(0) }
    var aiDescription by remember { mutableStateOf<String?>(null) }
    
    // 图片选择相关
    var localImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverImageBase64 by remember { mutableStateOf<String?>(null) }
    var imageChanged by remember { mutableStateOf(false) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            localImageUri = it
            imageChanged = true
            // 转换为base64
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val bytes = outputStream.toByteArray()
                    coverImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                snackbarMessage = "图片处理失败：${e.message}"
            }
        }
    }
    
    // 表单验证
    val isFormValid = productName.isNotBlank() && price.isNotBlank() && description.isNotBlank()
    
    // 显示Snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            snackbarMessage = null
        }
    }
    
    // 加载商品详情（编辑模式）
    LaunchedEffect(productId) {
        if (isEditMode) {
            isLoading = true
            try {
                val response = serverApiUtils.fetchProductDetail(productId)
                if (response.isSuccess && response.data != null) {
                    val product = response.data
                    productName = product.productName
                    price = product.price
                    coverImage = product.coverImage
                    description = product.description
                    isOnline = product.isOnline == 1
                    enableAiVoice = product.enableAiVoice == 1
                    weight = product.weight
                    stock = product.stock
                    aiDescription = product.aiDescription
                } else {
                    snackbarMessage = "加载商品详情失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "加载商品详情失败：${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 保存商品
    fun saveProduct() {
        if (!isFormValid) {
            snackbarMessage = "请填写完整商品信息"
            return
        }
        
        scope.launch {
            isSaving = true
            try {
                val response = if (isEditMode) {
                    serverApiUtils.editProduct(
                        id = productId,
                        productName = productName.trim(),
                        price = price.trim(),
                        coverImage = if (imageChanged) null else coverImage,
                        coverImageBase64 = if (imageChanged) coverImageBase64 else null,
                        description = description.trim(),
                        isOnline = if (isOnline) 1 else 0,
                        enableAiVoice = if (enableAiVoice) 1 else 0,
                        weight = weight,
                        stock = stock,
                        aiDescription = aiDescription?.trim()
                    )
                } else {
                    serverApiUtils.addProduct(
                        productName = productName.trim(),
                        price = price.trim(),
                        coverImage = if (!imageChanged) coverImage else null,
                        coverImageBase64 = if (imageChanged) coverImageBase64 else null,
                        description = description.trim(),
                        isOnline = if (isOnline) 1 else 0,
                        enableAiVoice = if (enableAiVoice) 1 else 0,
                        stock = stock
                    )
                }
                
                if (response.isSuccess) {
                    snackbarMessage = if (isEditMode) "保存成功" else "添加成功"
                    navController.navigateUp()
                } else {
                    snackbarMessage = "保存失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "保存失败：${e.message}"
            } finally {
                isSaving = false
            }
        }
    }
    
    // 生成AI讲解文案
    fun generateAiDescription() {
        if (!isEditMode) {
            snackbarMessage = "请先保存商品后再生成AI讲解文案"
            return
        }
        
        scope.launch {
            isGeneratingAi = true
            try {
                val response = serverApiUtils.generateProductAiDescription(productId)
                if (response.isSuccess && response.data != null) {
                    aiDescription = response.data.aiDescription
                    snackbarMessage = "AI讲解文案生成成功"
                } else {
                    snackbarMessage = "生成失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "生成失败：${e.message}"
            } finally {
                isGeneratingAi = false
            }
        }
    }
    
    // 生成/更新AI讲品语音
    fun generateAiTts() {
        if (!isEditMode) {
            snackbarMessage = "请先保存商品"
            return
        }
        val desc = aiDescription?.trim()
        if (desc.isNullOrBlank()) {
            snackbarMessage = "讲解稿内容不能为空"
            return
        }
        
        scope.launch {
            isGeneratingTts = true
            try {
                val response = serverApiUtils.generateProductAiTts(productId, desc)
                if (response.isSuccess) {
                    snackbarMessage = response.msg ?: "提交成功，请到讲品列表查看"
                } else {
                    snackbarMessage = "生成失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "生成失败：${e.message}"
            } finally {
                isGeneratingTts = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑商品" else "新增商品") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { saveProduct() },
                        enabled = isFormValid && !isSaving,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "保存",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
            ) {
                // 基本信息Section
                Text(
                    text = "基本信息",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SectionTitleColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 产品图
                        FormRow(label = "产品图：") {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    // 优先显示本地选择的图片
                                    localImageUri != null -> {
                                        AsyncImage(
                                            model = localImageUri,
                                            contentDescription = "产品图",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    // 显示服务器图片
                                    coverImage != null -> {
                                        val fullUrl = if (coverImage!!.startsWith("http://") || coverImage!!.startsWith("https://")) {
                                            coverImage
                                        } else {
                                            "${wapBaseUrl.trimEnd('/')}/${coverImage!!.trimStart('/')}"
                                        }
                                        AsyncImage(
                                            model = fullUrl,
                                            contentDescription = "产品图",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    // 占位符
                                    else -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = "选择图片",
                                                modifier = Modifier.size(32.dp),
                                                tint = Color(0xFF9E9E9E)
                                            )
                                            Text(
                                                text = "上传",
                                                fontSize = 10.sp,
                                                color = Color(0xFF9E9E9E),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        // 品名
                        FormRow(label = "品　名：") {
                            OutlinedTextField(
                                value = productName,
                                onValueChange = { productName = it },
                                placeholder = { Text("请输入商品名称", color = Color(0xFFBDBDBD)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFFF0F0F0)
                        )
                        
                        // 价格
                        FormRow(label = "价　格：") {
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                placeholder = { Text("0.00", color = Color(0xFFBDBDBD)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp),
                                leadingIcon = {
                                    Text(
                                        text = "¥",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFFF0F0F0)
                        )
                        
                        // 上架
                        FormRow(label = "上　架：") {
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { isOnline = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = SwitchEnabledColor
                                )
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFFF0F0F0)
                        )
                        
                        // 库存
                        FormRow(label = "库　存：") {
                            OutlinedTextField(
                                value = stock.toString(),
                                onValueChange = { newValue ->
                                    stock = newValue.filter { it.isDigit() }.toIntOrNull() ?: 0
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                suffix = { Text("件", color = Color.Gray) }
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFFF0F0F0)
                        )
                        
                        // 权重
                        FormRow(label = "权　重：") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (weight > 1) weight-- },
                                    enabled = weight > 1
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "减少",
                                        tint = if (weight > 1) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD)
                                    )
                                }
                                
                                Text(
                                    text = weight.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                
                                IconButton(onClick = { weight++ }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "增加",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 产品描述Section
                Text(
                    text = "产品描述",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SectionTitleColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { 
                                if (it.length <= 200) description = it
                            },
                            placeholder = { Text("请输入产品描述、关键字", color = Color(0xFFBDBDBD)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        Text(
                            text = "${description.length}/200",
                            color = if (description.length > 180) Color(0xFFFF6B6B) else Color(0xFF9E9E9E),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 生成AI讲解稿按钮
                OutlinedButton(
                    onClick = { generateAiDescription() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    enabled = isEditMode && !isGeneratingAi,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGeneratingAi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (isGeneratingAi) "生成中..." else if (!aiDescription.isNullOrBlank()) "🔄 重新生成AI讲解稿" else "✨ 生成AI讲解稿",
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // AI讲解稿编辑区域
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AiDescriptionBackground.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "AI讲解稿",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "约300字",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                        OutlinedTextField(
                            value = aiDescription ?: "",
                            onValueChange = { aiDescription = it },
                            placeholder = { Text("若此处为空，将使用产品描述进行讲解", color = Color(0xFFBDBDBD)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            maxLines = 10,
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 生成/更新商品卡讲品语音按钮
                OutlinedButton(
                    onClick = { generateAiTts() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    enabled = isEditMode && !aiDescription.isNullOrBlank() && !isGeneratingTts,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGeneratingTts) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (isGeneratingTts) "提交中..." else "🎙️ 生成/更新商品卡讲品语音",
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FormRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF616161),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(70.dp)
        )
        
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

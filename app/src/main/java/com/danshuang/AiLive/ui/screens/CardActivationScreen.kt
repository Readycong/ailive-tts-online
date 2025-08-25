package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardActivationScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 三个输入框的状态
    var code1 by remember { mutableStateOf("") }
    var code2 by remember { mutableStateOf("") }
    var code3 by remember { mutableStateOf("") }
    
    // Focus requesters for automatic navigation between fields
    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    
    // 检查是否所有输入框都已填满
    val isAllFieldsFilled = code1.length == 4 && code2.length == 4 && code3.length == 4
    
    // 激活状态
    var isActivating by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                title = {
                    Text("卡密激活")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 标题和说明
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "激活您的使用权益",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "请输入12位卡密进行激活",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // 卡密输入区域
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "卡密",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // 三个输入框用横线连接
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 第一个输入框
                    CodeInputField(
                        value = code1,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4) {
                                val oldLength = code1.length
                                code1 = newValue.uppercase()
                                if (newValue.length == 4 && oldLength < 4) {
                                    focusRequester2.requestFocus()
                                }
                            }
                        },
                        focusRequester = focusRequester1
                    )
                    
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 第二个输入框
                    CodeInputField(
                        value = code2,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4) {
                                val oldLength = code2.length
                                code2 = newValue.uppercase()
                                if (newValue.length == 4 && oldLength < 4) {
                                    focusRequester3.requestFocus()
                                }
                            }
                        },
                        focusRequester = focusRequester2
                    )
                    
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 第三个输入框
                    CodeInputField(
                        value = code3,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4) {
                                code3 = newValue.uppercase()
                            }
                        },
                        focusRequester = focusRequester3,
                        isLast = true
                    )
                }
                
                // 整体清空功能
                val hasAnyInput = code1.isNotEmpty() || code2.isNotEmpty() || code3.isNotEmpty()
                if (hasAnyInput) {
                    TextButton(
                        onClick = {
                            code1 = ""
                            code2 = ""
                            code3 = ""
                            focusRequester1.requestFocus()
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            "清空输入",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 提示信息
                if (!isAllFieldsFilled) {
                    Text(
                        text = "请完整输入12位卡密",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 激活按钮
            Button(
                onClick = {
                    if (isAllFieldsFilled) {
                        isActivating = true
                        val fullCode = "$code1-$code2-$code3"
                        
                        // 模拟激活过程
                        scope.launch {
                            delay(2000) // 模拟网络请求
                            isActivating = false
                            
                            // 这里添加实际的激活逻辑
                            // 模拟激活结果
                            if (fullCode == "ABCD-1234-EFGH") { // 示例成功卡密
                                Toast.makeText(context, "激活成功！", Toast.LENGTH_LONG).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "卡密无效，请检查后重试", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = isAllFieldsFilled && !isActivating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllFieldsFilled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isAllFieldsFilled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                if (isActivating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("激活中...")
                    }
                } else {
                    Text(
                        "验证并激活",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isLast: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(88.dp)
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                // Auto navigation is handled in onValueChange
            },
            onDone = {
                // Clear focus when done
            }
        ),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CardActivationScreenPreview() {
    AiLiveTheme {
        CardActivationScreen()
    }
}
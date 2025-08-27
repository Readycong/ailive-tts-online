package com.danshuang.AiLive.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 通用快捷功能数据类
 * @param title 功能标题
 * @param icon 图标资源ID（使用painterResource）
 * @param backgroundColor 背景颜色
 * @param onClick 点击事件回调
 */
data class QuickActionData(
    val title: String,
    val icon: Int,
    val backgroundColor: Color,
    val onClick: () -> Unit = {}
)

/**
 * 通用快捷功能数据类（使用矢量图标）
 * @param title 功能标题
 * @param icon 矢量图标
 * @param backgroundColor 背景颜色
 * @param onClick 点击事件回调
 */
data class QuickActionVectorData(
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val onClick: () -> Unit = {}
)

/**
 * 通用快捷功能卡片组件
 * @param action 快捷功能数据
 * @param modifier 修饰符
 * @param iconSize 图标大小
 * @param cardSize 卡片大小
 */
@Composable
fun QuickActionCard(
    action: QuickActionData,
    modifier: Modifier = Modifier,
    iconSize: Dp = 19.4.dp,
    cardSize: Dp = 48.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(64.dp)
    ) {
        Card(
            onClick = action.onClick,
            modifier = Modifier.size(cardSize),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = action.backgroundColor
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = action.icon),
                    contentDescription = action.title,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}

/**
 * 通用快捷功能卡片组件（使用矢量图标）
 * @param action 快捷功能数据
 * @param modifier 修饰符
 * @param iconSize 图标大小
 * @param cardSize 卡片大小
 */
@Composable
fun QuickActionVectorCard(
    data: QuickActionVectorData,
    modifier: Modifier = Modifier,
    iconSize: Dp = 21.6.dp,
    cardSize: Dp = 54.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(72.dp)
    ) {
        Card(
            onClick = data.onClick,
            modifier = Modifier.size(cardSize),
            shape = RoundedCornerShape(10.8.dp),
            colors = CardDefaults.cardColors(
                containerColor = data.backgroundColor
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = data.title,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = data.title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}

/**
 * 快捷功能网格布局组件
 * @param actions 快捷功能数据列表
 * @param modifier 修饰符
 * @param columns 列数
 * @param horizontalArrangement 水平排列方式
 * @param verticalArrangement 垂直排列方式
 */
@Composable
fun QuickActionsGrid(
    actions: List<QuickActionData>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp)
) {
    Column(modifier = modifier) {
        var currentRow = 0
        val itemsPerRow = columns
        val totalRows = (actions.size + itemsPerRow - 1) / itemsPerRow

        while (currentRow < totalRows) {
            val startIndex = currentRow * itemsPerRow
            val endIndex = minOf(startIndex + itemsPerRow, actions.size)
            val rowActions = actions.subList(startIndex, endIndex)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = horizontalArrangement
            ) {
                rowActions.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f)
                    )
                }
                // 如果最后一行不足columns个，补充空白占位
                repeat(itemsPerRow - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            
            if (currentRow < totalRows - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            currentRow++
        }
    }
}

/**
 * 快捷功能横向滚动布局组件
 * @param actions 快捷功能数据列表
 * @param modifier 修饰符
 * @param horizontalArrangement 水平排列方式
 */
@Composable
fun QuickActionsRow(
    actions: List<QuickActionData>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(actions.size) { index ->
            QuickActionCard(action = actions[index])
        }
    }
}
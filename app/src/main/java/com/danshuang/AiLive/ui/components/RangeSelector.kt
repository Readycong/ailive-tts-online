package com.danshuang.AiLive.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.data.RangeValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeSelectorCard(
    title: String,
    rangeValue: RangeValue,
    minRange: Float,
    maxRange: Float,
    description: String,
    onValueChange: (RangeValue) -> Unit
) {
    val options = remember { RangeValue.generateOptions(minRange, maxRange) }
    
    var minExpanded by remember { mutableStateOf(false) }
    var maxExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 最小值选择器
                ExposedDropdownMenuBox(
                    expanded = minExpanded,
                    onExpandedChange = { minExpanded = !minExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "%.1f".format(rangeValue.min),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("最小值") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = minExpanded,
                        onDismissRequest = { minExpanded = false }
                    ) {
                        options.filter { it <= rangeValue.max }.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("%.1f".format(option)) },
                                onClick = {
                                    onValueChange(RangeValue(option, maxOf(option, rangeValue.max)))
                                    minExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("到", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // 最大值选择器
                ExposedDropdownMenuBox(
                    expanded = maxExpanded,
                    onExpandedChange = { maxExpanded = !maxExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "%.1f".format(rangeValue.max),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("最大值") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = maxExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = maxExpanded,
                        onDismissRequest = { maxExpanded = false }
                    ) {
                        options.filter { it >= rangeValue.min }.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("%.1f".format(option)) },
                                onClick = {
                                    onValueChange(RangeValue(minOf(rangeValue.min, option), option))
                                    maxExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
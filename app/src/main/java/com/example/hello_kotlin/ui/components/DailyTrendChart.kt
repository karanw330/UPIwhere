package com.example.hello_kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello_kotlin.ui.theme.AccentPrimary
import com.example.hello_kotlin.ui.theme.AccentRed
import com.example.hello_kotlin.ui.theme.TextPrimary
import com.example.hello_kotlin.ui.theme.TextSecondary
import com.example.hello_kotlin.viewmodel.DailySpent
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DailyTrendChart(
    dailySpending: List<DailySpent>,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val maxSpend = remember(dailySpending) {
        dailySpending.maxOfOrNull { it.totalAmount } ?: 1.0
    }

    var selectedDay by remember(dailySpending) { mutableStateOf<DailySpent?>(null) }

    NeoCard(
        borderColor = AccentPrimary.copy(alpha = 0.25f),
        cornerRadius = 24.dp,
        shadowOffset = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title & Selected Day info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Spending Trend",
                    color = TextPrimary,
                    fontFamily = CondensedFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = selectedDay?.let { "Day ${it.day}: ${currencyFormat.format(it.totalAmount)}" }
                        ?: "Tap a bar for details",
                    color = if (selectedDay != null) AccentPrimary else TextSecondary,
                    fontFamily = MonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (dailySpending.isEmpty() || maxSpend <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trend data this month",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontFamily = CondensedFontFamily,
                        fontSize = 13.sp
                    )
                }
            } else {
                val scrollState = rememberScrollState()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailySpending.forEach { item ->
                        val isSelected = selectedDay?.day == item.day
                        val heightRatio = if (maxSpend > 0) (item.totalAmount / maxSpend).toFloat() else 0f
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight()
                                .clickable {
                                    selectedDay = if (isSelected) null else item
                                }
                        ) {
                            // Bar Track Container
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .width(10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2A2A3E)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Filled bar segment
                                if (heightRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(heightRatio)
                                            .background(if (isSelected) AccentRed else AccentPrimary)
                                    )
                                }
                            }

                            // Day label
                            Text(
                                text = String.format("%02d", item.day),
                                color = if (isSelected) AccentRed else TextSecondary,
                                fontFamily = MonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

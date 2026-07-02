package com.example.hello_kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello_kotlin.ui.theme.TextSecondary

val PRESET_COLORS = listOf(
    "#FF6B6B", "#E74C3C", "#C0392B",  // Reds
    "#FF8A65", "#F39C12", "#E67E22",  // Oranges
    "#FFEAA7", "#FFD700", "#F1C40F",  // Yellows
    "#00C9A7", "#2ECC71", "#27AE60",  // Greens
    "#4ECDC4", "#1ABC9C", "#16A085",  // Teals
    "#45B7D1", "#3498DB", "#2980B9",  // Blues
    "#667EEA", "#764BA2", "#9B59B6",  // Purples
    "#DDA0DD", "#E91E8F", "#FF69B4",  // Pinks
    "#95A5A6", "#7F8C8D", "#34495E"   // Grays
)

@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Choose a color",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))

        val chunkedColors = PRESET_COLORS.chunked(6)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunkedColors.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { colorHex ->
                        val color = try {
                            val c = colorHex.removePrefix("#").toLong(16)
                            Color(0xFF000000 or c)
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { onColorSelected(colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

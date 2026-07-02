package com.example.hello_kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello_kotlin.data.db.dao.TransactionWithTag
import com.example.hello_kotlin.ui.theme.AccentGreen
import com.example.hello_kotlin.ui.theme.AccentRed
import com.example.hello_kotlin.ui.theme.TextPrimary
import com.example.hello_kotlin.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItem(
    transaction: TransactionWithTag,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isDebit = transaction.type == "DEBIT"

    val tagColor = remember(transaction.tagColor) {
        transaction.tagColor?.let {
            try {
                val c = it.removePrefix("#").toLong(16)
                Color(0xFF000000 or c)
            } catch (e: Exception) {
                Color(0xFF667EEA)
            }
        } ?: Color(0xFF95A5A6)
    }

    NeoCard(
        modifier = modifier,
        borderColor = tagColor.copy(alpha = 0.3f),
        cornerRadius = 14.dp,
        shadowOffset = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tagColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.tagEmoji ?: if (transaction.isTagged) "🏷️" else "❓",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.tagName ?: "Untagged",
                    color = TextPrimary,
                    fontFamily = CondensedFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.accountName ?: transaction.senderAddress,
                    color = TextSecondary,
                    fontFamily = CondensedFontFamily,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Amount & Time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDebit) "-" else "+"}${currencyFormat.format(transaction.amount)}",
                    color = if (isDebit) AccentRed else AccentGreen,
                    fontFamily = MonoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    color = TextSecondary,
                    fontFamily = CondensedFontFamily,
                    fontSize = 10.sp
                )
            }
        }
    }
}

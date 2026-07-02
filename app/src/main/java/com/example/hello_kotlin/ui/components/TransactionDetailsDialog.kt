package com.example.hello_kotlin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello_kotlin.data.db.dao.TransactionWithTag
import com.example.hello_kotlin.data.db.entity.TagEntity
import com.example.hello_kotlin.ui.theme.AccentGreen
import com.example.hello_kotlin.ui.theme.AccentPrimary
import com.example.hello_kotlin.ui.theme.AccentRed
import com.example.hello_kotlin.ui.theme.DarkBackground
import com.example.hello_kotlin.ui.theme.DarkSurface
import com.example.hello_kotlin.ui.theme.TextPrimary
import com.example.hello_kotlin.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailsDialog(
    transaction: TransactionWithTag,
    allTags: List<TagEntity>,
    onTagSelected: (tagId: Long) -> Unit,
    onDeleteTransaction: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transaction.isTagged) "TRANSACTION DETAILS" else "TAG TRANSACTION",
                fontFamily = CondensedFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isDebit = transaction.type == "DEBIT"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDebit) "DEBIT" else "CREDIT",
                        color = if (isDebit) AccentRed else AccentGreen,
                        fontFamily = CondensedFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = dateFormat.format(Date(transaction.timestamp)),
                        color = TextSecondary,
                        fontFamily = CondensedFontFamily,
                        fontSize = 11.sp
                    )
                }

                Column {
                    Text(
                        text = "AMOUNT",
                        color = TextSecondary,
                        fontFamily = CondensedFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "₹${String.format("%.2f", transaction.amount)}",
                        color = TextPrimary,
                        fontFamily = MonoFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = "SOURCE/MERCHANT",
                        color = TextSecondary,
                        fontFamily = CondensedFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sourceText = transaction.accountName ?: transaction.senderAddress
                        Text(
                            text = sourceText,
                            color = TextPrimary,
                            fontFamily = CondensedFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Text(
                            text = "COPY",
                            color = AccentPrimary,
                            fontFamily = CondensedFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(sourceText))
                                    android.widget.Toast.makeText(context, "Copied: $sourceText", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "SMS MESSAGE BODY",
                        color = TextSecondary,
                        fontFamily = CondensedFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NeoCard(
                        borderColor = AccentPrimary.copy(alpha = 0.15f),
                        cornerRadius = 10.dp,
                        shadowOffset = 3.dp,
                        backgroundColor = DarkBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = transaction.rawMessage,
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontFamily = CondensedFontFamily,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = if (transaction.isTagged) "CHANGE CATEGORY TAG" else "SELECT CATEGORY TAG",
                        color = TextSecondary,
                        fontFamily = CondensedFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            val isCurrentTag = transaction.tagId == tag.id
                            val tagColor = try {
                                val c = tag.colorHex.removePrefix("#").toLong(16)
                                Color(0xFF000000 or c)
                            } catch (e: Exception) {
                                AccentPrimary
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentTag) tagColor.copy(alpha = 0.25f) else tagColor.copy(alpha = 0.12f),
                                border = if (isCurrentTag) BorderStroke(1.5.dp, tagColor) else null,
                                modifier = Modifier.clickable {
                                    onTagSelected(tag.id)
                                    onDismiss()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = tag.emoji ?: "🏷️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tag.name.uppercase(),
                                        color = tagColor,
                                        fontFamily = CondensedFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrentTag) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDeleteTransaction()
                    onDismiss()
                }
            ) {
                Text(
                    text = "DELETE",
                    color = AccentRed,
                    fontFamily = CondensedFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CLOSE",
                    fontFamily = CondensedFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = DarkSurface
    )
}

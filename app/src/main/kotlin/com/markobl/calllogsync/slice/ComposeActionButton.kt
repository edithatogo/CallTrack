package com.markobl.calllogsync.slice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markobl.calllogsync.R

@Composable
internal fun ComposeActionButton(
    label: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    accessibilityLabel: String? = null,
) {
    val dark = isSystemInDarkTheme()
    val background =
        if (enabled) {
            colorResource(if (dark) R.color.purple_200 else R.color.purple_500)
        } else {
            colorResource(android.R.color.darker_gray)
        }
    val foreground = if (dark && enabled) Color.Black else Color.White

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(background)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    testTagsAsResourceId = true
                    testTag = tag
                    contentDescription = accessibilityLabel ?: label
                    text = androidx.compose.ui.text.AnnotatedString(accessibilityLabel ?: label)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )
    }
}

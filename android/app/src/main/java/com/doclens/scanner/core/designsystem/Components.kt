package com.doclens.scanner.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

val ShapeCard = RoundedCornerShape(16.dp)
val ShapeControl = RoundedCornerShape(12.dp)
val ShapeBadge = RoundedCornerShape(8.dp)

@Composable
fun ScannerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled && !isLoading,
        shape = ShapeControl,
        colors = ButtonDefaults.buttonColors(
            containerColor = ScannerTheme.colors.primary,
            contentColor = ScannerTheme.colors.onPrimary,
            disabledContainerColor = ScannerTheme.colors.surfaceVariant,
            disabledContentColor = ScannerTheme.colors.onSurfaceVariant
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ScannerTheme.colors.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = ScannerTheme.colors.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ScannerTheme.typography.buttonLabel,
            color = if (enabled && !isLoading) ScannerTheme.colors.onPrimary else ScannerTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
fun ScannerOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ShapeControl,
        border = BorderStroke(
            1.dp,
            if (enabled) ScannerTheme.colors.outlineStrong else ScannerTheme.colors.borderSubtle
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ScannerTheme.colors.onSurface,
            disabledContentColor = ScannerTheme.colors.onSurfaceVariant
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) ScannerTheme.colors.onSurface else ScannerTheme.colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ScannerTheme.typography.buttonLabel
        )
    }
}

@Composable
fun ScannerDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ShapeControl,
        colors = ButtonDefaults.buttonColors(
            containerColor = ScannerTheme.colors.error,
            contentColor = Color.White
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ScannerTheme.typography.buttonLabel,
            color = Color.White
        )
    }
}

@Composable
fun ScannerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = ScannerTheme.colors.surface,
        contentColor = ScannerTheme.colors.onSurface
    )
    val border = BorderStroke(1.dp, ScannerTheme.colors.borderSubtle)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = ShapeCard,
            colors = cardColors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = ShapeCard,
            colors = cardColors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ScannerStatusBadge(
    text: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ShapeBadge,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = ScannerTheme.typography.supportingLabel,
                color = color
            )
        }
    }
}

@Composable
fun ScannerFilterChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        ScannerTheme.colors.primary.copy(alpha = 0.2f)
    } else {
        ScannerTheme.colors.surface
    }
    val borderColor = if (selected) {
        ScannerTheme.colors.primary
    } else {
        ScannerTheme.colors.borderSubtle
    }
    val textColor = if (selected) {
        ScannerTheme.colors.primary
    } else {
        ScannerTheme.colors.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(ShapeControl)
            .background(backgroundColor)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ScannerTheme.typography.buttonLabel,
            color = textColor
        )
    }
}

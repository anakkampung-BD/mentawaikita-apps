package com.obill.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.obill.app.ui.theme.BlueBrand
import com.obill.app.ui.theme.OrangeBrand

@Composable
fun ObillGradientButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    iconSize: Dp = 18.dp,
) {
    val brush = Brush.horizontalGradient(listOf(OrangeBrand, BlueBrand))
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .background(brush, RoundedCornerShape(14.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        if (icon != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = text,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        } else {
            Text(
                text = text,
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

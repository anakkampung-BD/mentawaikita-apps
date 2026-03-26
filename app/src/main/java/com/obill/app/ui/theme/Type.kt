package com.obill.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.obill.app.R

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

fun obillTypography(textScale: Float): Typography {
    val s = textScale
    val body = (12f * s).sp
    val title = (20f * s).sp
    val label = (11f * s).sp
    return Typography(
        displayLarge = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = (34f * s).sp,
            lineHeight = (40f * s).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = title,
            lineHeight = (26f * s).sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = body,
            lineHeight = (18f * s).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = body,
            lineHeight = (18f * s).sp,
        ),
        bodySmall = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = (11f * s).sp,
            lineHeight = (16f * s).sp,
        ),
        labelLarge = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = label,
            lineHeight = (16f * s).sp,
        ),
        labelMedium = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = label,
            lineHeight = (14f * s).sp,
        ),
        labelSmall = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = (10f * s).sp,
            lineHeight = (14f * s).sp,
        ),
    )
}

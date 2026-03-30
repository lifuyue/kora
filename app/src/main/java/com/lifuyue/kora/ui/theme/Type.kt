package com.lifuyue.kora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val KoraDisplay = FontFamily.Serif
private val KoraBody = FontFamily.SansSerif

val KoraTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = KoraDisplay,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = KoraDisplay,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 34.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = KoraDisplay,
                fontWeight = FontWeight.SemiBold,
                fontSize = 25.sp,
                lineHeight = 30.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = KoraBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
                lineHeight = 27.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = KoraBody,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                lineHeight = 24.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = KoraBody,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 25.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = KoraBody,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = KoraBody,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = KoraBody,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = KoraBody,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
    )

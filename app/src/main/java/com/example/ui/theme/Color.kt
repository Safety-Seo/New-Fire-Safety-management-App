package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Tones (Updated to match Bold Typography theme colors)
val FireRed = Color(0xFFBA1A1A)       // Bold Red bg-[#BA1A1A]
val SafetyOrange = Color(0xFF7D5900)  // Bold Amber bg-[#7D5900]
val SafetyGreen = Color(0xFF006E33)   // Bold Green bg-[#006E33]
val SlateBlue = Color(0xFF006399)     // Bold Primary Blue bg-[#006399]

val LightBackground = Color(0xFFF3F4F9) // Updated to match Bold Typography bg-[#F3F4F9]
val LightSurface = Color(0xFFFFFFFF)
val LightText = Color(0xFF0F172A) // Slate-900

// Bold Typography Theme Specific Colors
val BoldPrimaryBlue = Color(0xFF006399)       // Primary brand blue from HTML bg-[#006399]
val BoldPrimaryContainer = Color(0xFFD1E4FF)  // Selected item pill bg-[#D1E4FF]
val BoldTextSlate = Color(0xFF0F172A)         // text-slate-900
val BoldTextMuted = Color(0xFF64748B)         // text-slate-500
val BoldBorderSlate = Color(0xFFE2E8F0)       // border-slate-100/200

// Semantic colors
val BoldSecondaryAmber = Color(0xFF7D5900)    // Amber text text-[#7D5900]
val BoldSecondaryContainer = Color(0xFFFFF4E5)// Amber card bg-[#FFF4E5]
val BoldSecondaryBorder = Color(0xFFFFE1B9)   // Amber border border-[#FFE1B9]

val BoldTertiaryGreen = Color(0xFF006E33)     // Green text text-[#006E33]
val BoldTertiaryContainer = Color(0xFFE8F5E9) // Green tag bg-[#E8F5E9]

val BoldErrorRed = Color(0xFFBA1A1A)          // Red text text-[#BA1A1A]
val BoldErrorContainer = Color(0xFFFFEAEA)    // Red card bg-[#FFEAEA]
val BoldErrorBorder = Color(0xFFFFD2D2)       // Red border border-[#FFD2D2]

// Dark Theme / "Fire Command Center" Aesthetic with Bold Typography principles
val DarkBackground = Color(0xFF0B0F19) // Deeper midnight slate
val DarkSurface = Color(0xFF151D30)    // Charcoal slate
val DarkCard = Color(0xFF1E293B)       // Elevated card outline
val DarkText = Color(0xFFF8FAFC)

// Mapping old color variables to new theme colors for instant system-wide integration
val PrimaryRed = BoldErrorRed          // #BA1A1A
val AccentAmber = BoldSecondaryAmber    // #7D5900
val AccentGreen = BoldTertiaryGreen    // #006E33
val InfoBlue = BoldPrimaryBlue         // #006399

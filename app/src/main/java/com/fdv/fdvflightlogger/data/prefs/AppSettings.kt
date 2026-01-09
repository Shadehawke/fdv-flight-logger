package com.fdv.fdvflightlogger.data.prefs

enum class ThemeMode { FDV, LIGHT, DARK }
enum class WeightUnit { LB, KG }
enum class QnhUnit { INHG, HPA }
enum class TempUnit { F, C }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.FDV,
    val weightUnit: WeightUnit = WeightUnit.LB,
    val fuelUnit: WeightUnit = WeightUnit.LB, // Fuel logged as lb/kg for MVP
    val qnhUnit: QnhUnit = QnhUnit.INHG,
    val tempUnit: TempUnit = TempUnit.F
)

data class PilotProfile(
    val pilotId: String = "",
    val name: String = "",
    val hub: String = ""
) {
    val isComplete: Boolean
        get() = pilotId.isNotBlank() && name.isNotBlank() && hub.isNotBlank()
}

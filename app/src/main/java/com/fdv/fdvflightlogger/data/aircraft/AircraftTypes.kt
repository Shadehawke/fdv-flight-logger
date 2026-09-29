package com.fdv.fdvflightlogger.data.aircraft

data class AircraftType(
    val icao: String,
    val name: String,
    val family: AircraftFamily
) {
    // Precomputed once so search doesn't re-uppercase on every keystroke
    internal val nameKey: String = name.uppercase()
    internal val nameWords: List<String> = nameKey.split(' ', '-', '/', '(', ')').filter { it.isNotBlank() }
}

/**
 * Aircraft in service with Delta, SkyTeam members, and Delta partners.
 * Verified September 2026. Several variants share one ICAO code
 * (737-900/900ER = B739, A321neo/LR = A21N), so they're merged into one entry.
 */
object AircraftTypes {
    val ALL: List<AircraftType> = listOf(
        // Airbus
        AircraftType("BCS1", "Airbus A220-100", AircraftFamily.A220),
        AircraftType("BCS3", "Airbus A220-300", AircraftFamily.A220),
        AircraftType("A318", "Airbus A318", AircraftFamily.A320),
        AircraftType("A319", "Airbus A319", AircraftFamily.A320),
        AircraftType("A320", "Airbus A320", AircraftFamily.A320),
        AircraftType("A20N", "Airbus A320neo", AircraftFamily.A320),
        AircraftType("A321", "Airbus A321", AircraftFamily.A320),
        AircraftType("A21N", "Airbus A321neo / A321LR", AircraftFamily.A320),
        AircraftType("A332", "Airbus A330-200", AircraftFamily.A330),
        AircraftType("A333", "Airbus A330-300", AircraftFamily.A330),
        AircraftType("A339", "Airbus A330-900neo", AircraftFamily.A330),
        AircraftType("A359", "Airbus A350-900", AircraftFamily.A350),
        AircraftType("A35K", "Airbus A350-1000", AircraftFamily.A350),
        AircraftType("A388", "Airbus A380-800", AircraftFamily.A380),

        // Boeing
        AircraftType("B712", "Boeing 717-200", AircraftFamily.B717),
        AircraftType("B733", "Boeing 737-300 (Freighter)", AircraftFamily.B737),
        AircraftType("B737", "Boeing 737-700", AircraftFamily.B737),
        AircraftType("B738", "Boeing 737-800", AircraftFamily.B737),
        AircraftType("B739", "Boeing 737-900 / 900ER", AircraftFamily.B737),
        AircraftType("B38M", "Boeing 737 MAX 8", AircraftFamily.B737),
        AircraftType("B39M", "Boeing 737 MAX 9", AircraftFamily.B737),
        AircraftType("B744", "Boeing 747-400 (Freighter)", AircraftFamily.B747),
        AircraftType("B748", "Boeing 747-8", AircraftFamily.B747),
        AircraftType("B752", "Boeing 757-200", AircraftFamily.B757),
        AircraftType("B753", "Boeing 757-300", AircraftFamily.B757),
        AircraftType("B763", "Boeing 767-300ER", AircraftFamily.B767),
        AircraftType("B764", "Boeing 767-400ER", AircraftFamily.B767),
        AircraftType("B772", "Boeing 777-200ER", AircraftFamily.B777),
        AircraftType("B773", "Boeing 777-300", AircraftFamily.B777),
        AircraftType("B77W", "Boeing 777-300ER", AircraftFamily.B777),
        AircraftType("B77L", "Boeing 777F", AircraftFamily.B777),
        AircraftType("B788", "Boeing 787-8", AircraftFamily.B787),
        AircraftType("B789", "Boeing 787-9", AircraftFamily.B787),
        AircraftType("B78X", "Boeing 787-10", AircraftFamily.B787),

        // Regional and other
        AircraftType("E170", "Embraer 170", AircraftFamily.EJET),
        AircraftType("E75L", "Embraer 175", AircraftFamily.EJET),
        AircraftType("E190", "Embraer 190", AircraftFamily.EJET),
        AircraftType("E195", "Embraer 195", AircraftFamily.EJET),
        AircraftType("E295", "Embraer 195-E2", AircraftFamily.EJET_E2),
        AircraftType("CRJ7", "Bombardier CRJ-700", AircraftFamily.CRJ),
        AircraftType("CRJ9", "Bombardier CRJ-900", AircraftFamily.CRJ),
        AircraftType("AT76", "ATR 72-600", AircraftFamily.ATR72),
        AircraftType("DH8D", "De Havilland Dash 8-400", AircraftFamily.DASH8),
        AircraftType("AJ27", "COMAC C909 (ARJ21-700)", AircraftFamily.ARJ21),
        AircraftType("C919", "COMAC C919", AircraftFamily.C919)
    )

    private val byIcao: Map<String, AircraftType> = ALL.associateBy { it.icao }

    fun getByIcao(icao: String?): AircraftType? =
        icao?.trim()?.uppercase()?.let { byIcao[it] }

    /**
     * Ranked search: ICAO code matches first, then name words ("737" → 737-800, MAX 8...).
     * One character is enough, since the list is short.
     */
    fun search(query: String, limit: Int = 8): List<AircraftType> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return emptyList()

        return ALL
            .mapNotNull { type -> rank(type, q)?.let { it to type } }
            .sortedWith(compareBy({ it.first }, { it.second.icao }))
            .take(limit)
            .map { it.second }
    }

    // Lower rank = better match; null = no match.
    private fun rank(t: AircraftType, q: String): Int? = when {
        t.icao == q -> 0
        t.icao.startsWith(q) -> 1
        t.nameWords.any { it.startsWith(q) } -> 2
        t.nameKey.contains(q) -> 3
        else -> null
    }
}
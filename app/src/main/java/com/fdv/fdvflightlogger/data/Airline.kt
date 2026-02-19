package com.fdv.fdvflightlogger.data

/**
 * Represents an airline with ICAO code and full name
 */
data class Airline(
    val icao: String,
    val name: String
)

/**
 * Predefined list of SkyTeam and codeshare airlines
 */
object Airlines {
    val ALL = listOf(
        // SkyTeam
        Airline("ARG", "Aerolíneas Argentinas"),
        Airline("AMX", "Aeromexico"),
        Airline("AEA", "Air Europa"),
        Airline("AFR", "Air France"),
        Airline("CAL", "China Airlines"),
        Airline("CES", "China Eastern"),
        Airline("DAL", "Delta Air Lines"),
        Airline("GIA", "Garuda Indonesia"),
        Airline("KQA", "Kenya Airways"),
        Airline("KLM", "KLM Royal Dutch Airlines"),
        Airline("KAL", "Korean Air"),
        Airline("MEA", "Middle East Airlines"),
        Airline("SAS", "SAS Scandinavian Airlines"),
        Airline("SVA", "Saudia"),
        Airline("ROT", "TAROM"),
        Airline("HVN", "Vietnam Airlines"),
        Airline("VIR", "Virgin Atlantic"),
        Airline("CXA", "Xiamen Airlines"),

        // Codeshares
        Airline("BTI", "airBaltic"),
        Airline("CSN", "China Southern Airlines"),
        Airline("CSA", "Czech Airlines"),
        Airline("ENY", "Endeavor Air"), // Delta Connection
        Airline("HAL", "Hawaiian Airlines"),
        Airline("ITY", "ITA Airways"),
        Airline("LAN", "LATAM Airlines"),
        Airline("RPA", "Republic Airways"), // Delta Connection
        Airline("SBS", "Seaborne Airlines"),
        Airline("CES", "Shanghai Airlines"),
        Airline("SIL", "Silver Airways"),
        Airline("SKW", "SkyWest Airlines"), // Delta Connection
        Airline("TRA", "Transavia"),
        Airline("WJA", "WestJet")
    ).sortedBy { it.icao }

    /**
     * Get airline by ICAO code, returns null if not found
     */
    fun getByIcao(icao: String?): Airline? {
        return ALL.find { it.icao == icao }
    }

    /**
     * Get full airline name by ICAO code, returns empty string if not found
     */
    fun getNameByIcao(icao: String?): String {
        return getByIcao(icao)?.name.orEmpty()
    }
}
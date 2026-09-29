package com.fdv.fdvflightlogger.data

/**
 * An airline the pilot can fly for.
 * [marketedAs] is set for regional operators whose flights carry another airline's
 * code, e.g. Endeavor flies as Delta Connection under DAL flight numbers.
 */
data class Airline(
    val icao: String,
    val name: String,
    val marketedAs: String? = null
) {
    val flightNumberPrefix: String get() = marketedAs ?: icao
}

/**
 * Delta, current SkyTeam members, and Delta airline partners.
 * Verified September 2026 against the SkyTeam fact sheet and Delta's partner page.
 */
object Airlines {
    val ALL = listOf(
        // SkyTeam members (Aeroflot is suspended and excluded)
        Airline("ARG", "Aerolíneas Argentinas"),
        Airline("AMX", "Aeroméxico"),
        Airline("AEA", "Air Europa"),
        Airline("AFR", "Air France"),
        Airline("CAL", "China Airlines"),
        Airline("CES", "China Eastern Airlines"),
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
        Airline("CXA", "XiamenAir"),

        // Delta Connection: operated regionally, marketed under DAL flight numbers
        Airline("EDV", "Delta Connection (Endeavor Air)", marketedAs = "DAL"),
        Airline("RPA", "Delta Connection (Republic Airways)", marketedAs = "DAL"),
        Airline("SKW", "Delta Connection (SkyWest Airlines)", marketedAs = "DAL"),

        // Delta airline partners
        Airline("BTI", "airBaltic"),
        Airline("ELY", "EL AL Israel Airlines"),
        Airline("LAN", "LATAM Airlines"),
        Airline("CSH", "Shanghai Airlines"),
        Airline("WJA", "WestJet")
    ).sortedBy { it.icao }

    fun getByIcao(icao: String?): Airline? =
        icao?.let { code -> ALL.find { it.icao == code } }

    fun getNameByIcao(icao: String?): String =
        getByIcao(icao)?.name.orEmpty()

    /**
     * Builds the header line: "DAL1703 • A320 • Delta Air Lines".
     * Unknown codes from older saved flights still show as the raw code.
     */
    fun formatFlightInfo(airlineIcao: String?, flightNumber: String?, aircraft: String?): String {
        val airline = getByIcao(airlineIcao)
        val code = airlineIcao?.takeIf { it.isNotBlank() }
        val number = flightNumber?.takeIf { it.isNotBlank() }

        return buildList {
            when {
                number != null && airline != null -> add(airline.flightNumberPrefix + number)
                number != null && code != null -> add(code + number)
                number != null -> add(number)
            }
            aircraft?.takeIf { it.isNotBlank() }?.let { add(it) }
            airline?.let { add(it.name) }
        }.joinToString(" • ")
    }
}
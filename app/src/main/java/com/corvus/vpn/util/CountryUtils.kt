package com.corvus.vpn.util

import java.util.Locale

object CountryUtils {

    private val countryMap = mapOf(
        "AF" to ("Afghanistan" to "🇦🇫"),
        "AL" to ("Albania" to "🇦🇱"),
        "DZ" to ("Algeria" to "🇩🇿"),
        "AD" to ("Andorra" to "🇦🇩"),
        "AO" to ("Angola" to "🇦🇴"),
        "AG" to ("Antigua & Barbuda" to "🇦🇬"),
        "AR" to ("Argentina" to "🇦🇷"),
        "AM" to ("Armenia" to "🇦🇲"),
        "AU" to ("Australia" to "🇦🇺"),
        "AT" to ("Austria" to "🇦🇹"),
        "AZ" to ("Azerbaijan" to "🇦🇿"),
        "BS" to ("Bahamas" to "🇧🇸"),
        "BH" to ("Bahrain" to "🇧🇭"),
        "BD" to ("Bangladesh" to "🇧🇩"),
        "BB" to ("Barbados" to "🇧🇧"),
        "BY" to ("Belarus" to "🇧🇾"),
        "BE" to ("Belgium" to "🇧🇪"),
        "BZ" to ("Belize" to "🇧🇿"),
        "BJ" to ("Benin" to "🇧🇯"),
        "BT" to ("Bhutan" to "🇧🇹"),
        "BO" to ("Bolivia" to "🇧🇴"),
        "BA" to ("Bosnia & Herzegovina" to "🇧🇦"),
        "BW" to ("Botswana" to "🇧🇼"),
        "BR" to ("Brazil" to "🇧🇷"),
        "BN" to ("Brunei" to "🇧🇳"),
        "BG" to ("Bulgaria" to "🇧🇬"),
        "BF" to ("Burkina Faso" to "🇧🇫"),
        "BI" to ("Burundi" to "🇧🇮"),
        "KH" to ("Cambodia" to "🇰🇭"),
        "CM" to ("Cameroon" to "🇨🇲"),
        "CA" to ("Canada" to "🇨🇦"),
        "CV" to ("Cape Verde" to "🇨🇻"),
        "CF" to ("Central African Rep." to "🇨🇫"),
        "TD" to ("Chad" to "🇹🇩"),
        "CL" to ("Chile" to "🇨🇱"),
        "CN" to ("China" to "🇨🇳"),
        "CO" to ("Colombia" to "🇨🇴"),
        "KM" to ("Comoros" to "🇰🇲"),
        "CG" to ("Congo" to "🇨🇬"),
        "CD" to ("Congo (DRC)" to "🇨🇩"),
        "CR" to ("Costa Rica" to "🇨🇷"),
        "HR" to ("Croatia" to "🇭🇷"),
        "CU" to ("Cuba" to "🇨🇺"),
        "CY" to ("Cyprus" to "🇨🇾"),
        "CZ" to ("Czech Republic" to "🇨🇿"),
        "DK" to ("Denmark" to "🇩🇰"),
        "DJ" to ("Djibouti" to "🇩🇯"),
        "DM" to ("Dominica" to "🇩🇲"),
        "DO" to ("Dominican Republic" to "🇩🇴"),
        "EC" to ("Ecuador" to "🇪🇨"),
        "EG" to ("Egypt" to "🇪🇬"),
        "SV" to ("El Salvador" to "🇸🇻"),
        "GQ" to ("Equatorial Guinea" to "🇬🇶"),
        "ER" to ("Eritrea" to "🇪🇷"),
        "EE" to ("Estonia" to "🇪🇪"),
        "SZ" to ("Eswatini" to "🇸🇿"),
        "ET" to ("Ethiopia" to "🇪🇹"),
        "FJ" to ("Fiji" to "🇫🇯"),
        "FI" to ("Finland" to "🇫🇮"),
        "FR" to ("France" to "🇫🇷"),
        "GA" to ("Gabon" to "🇬🇦"),
        "GM" to ("Gambia" to "🇬🇲"),
        "GE" to ("Georgia" to "🇬🇪"),
        "DE" to ("Germany" to "🇩🇪"),
        "GH" to ("Ghana" to "🇬🇭"),
        "GR" to ("Greece" to "🇬🇷"),
        "GD" to ("Grenada" to "🇬🇩"),
        "GT" to ("Guatemala" to "🇬🇹"),
        "GN" to ("Guinea" to "🇬🇳"),
        "GW" to ("Guinea-Bissau" to "🇬🇼"),
        "GY" to ("Guyana" to "🇬🇾"),
        "HT" to ("Haiti" to "🇭🇹"),
        "HN" to ("Honduras" to "🇭🇳"),
        "HK" to ("Hong Kong" to "🇭🇰"),
        "HU" to ("Hungary" to "🇭🇺"),
        "IS" to ("Iceland" to "🇮🇸"),
        "IN" to ("India" to "🇮🇳"),
        "ID" to ("Indonesia" to "🇮🇩"),
        "IR" to ("Iran" to "🇮🇷"),
        "IQ" to ("Iraq" to "🇮🇶"),
        "IE" to ("Ireland" to "🇮🇪"),
        "IL" to ("Israel" to "🇮🇱"),
        "IT" to ("Italy" to "🇮🇹"),
        "JM" to ("Jamaica" to "🇯🇲"),
        "JP" to ("Japan" to "🇯🇵"),
        "JO" to ("Jordan" to "🇯🇴"),
        "KZ" to ("Kazakhstan" to "🇰🇿"),
        "KE" to ("Kenya" to "🇰🇪"),
        "KI" to ("Kiribati" to "🇰🇮"),
        "KP" to ("North Korea" to "🇰🇵"),
        "KR" to ("South Korea" to "🇰🇷"),
        "KW" to ("Kuwait" to "🇰🇼"),
        "KG" to ("Kyrgyzstan" to "🇰🇬"),
        "LA" to ("Laos" to "🇱🇦"),
        "LV" to ("Latvia" to "🇱🇻"),
        "LB" to ("Lebanon" to "🇱🇧"),
        "LS" to ("Lesotho" to "🇱🇸"),
        "LR" to ("Liberia" to "🇱🇷"),
        "LY" to ("Libya" to "🇱🇾"),
        "LI" to ("Liechtenstein" to "🇱🇮"),
        "LT" to ("Lithuania" to "🇱🇹"),
        "LU" to ("Luxembourg" to "🇱🇺"),
        "MO" to ("Macau" to "🇲🇴"),
        "MG" to ("Madagascar" to "🇲🇬"),
        "MW" to ("Malawi" to "🇲🇼"),
        "MY" to ("Malaysia" to "🇲🇾"),
        "MV" to ("Maldives" to "🇲🇻"),
        "ML" to ("Mali" to "🇲🇱"),
        "MT" to ("Malta" to "🇲🇹"),
        "MH" to ("Marshall Islands" to "🇲🇭"),
        "MR" to ("Mauritania" to "🇲🇷"),
        "MU" to ("Mauritius" to "🇲🇺"),
        "MX" to ("Mexico" to "🇲🇽"),
        "FM" to ("Micronesia" to "🇫🇲"),
        "MD" to ("Moldova" to "🇲🇩"),
        "MC" to ("Monaco" to "🇲🇨"),
        "MN" to ("Mongolia" to "🇲🇳"),
        "ME" to ("Montenegro" to "🇲🇪"),
        "MA" to ("Morocco" to "🇲🇦"),
        "MZ" to ("Mozambique" to "🇲🇿"),
        "MM" to ("Myanmar" to "🇲🇲"),
        "NA" to ("Namibia" to "🇳🇦"),
        "NR" to ("Nauru" to "🇳🇷"),
        "NP" to ("Nepal" to "🇳🇵"),
        "NL" to ("Netherlands" to "🇳🇱"),
        "NZ" to ("New Zealand" to "🇳🇿"),
        "NI" to ("Nicaragua" to "🇳🇮"),
        "NE" to ("Niger" to "🇳🇪"),
        "NG" to ("Nigeria" to "🇳🇬"),
        "MK" to ("North Macedonia" to "🇲🇰"),
        "NO" to ("Norway" to "🇳🇴"),
        "OM" to ("Oman" to "🇴🇲"),
        "PK" to ("Pakistan" to "🇵🇰"),
        "PW" to ("Palau" to "🇵🇼"),
        "PS" to ("Palestine" to "🇵🇸"),
        "PA" to ("Panama" to "🇵🇦"),
        "PG" to ("Papua New Guinea" to "🇵🇬"),
        "PY" to ("Paraguay" to "🇵🇾"),
        "PE" to ("Peru" to "🇵🇪"),
        "PH" to ("Philippines" to "🇵🇭"),
        "PL" to ("Poland" to "🇵🇱"),
        "PT" to ("Portugal" to "🇵🇹"),
        "QA" to ("Qatar" to "🇶🇦"),
        "RO" to ("Romania" to "🇷🇴"),
        "RU" to ("Russia" to "🇷🇺"),
        "RW" to ("Rwanda" to "🇷🇼"),
        "KN" to ("Saint Kitts & Nevis" to "🇰🇳"),
        "LC" to ("Saint Lucia" to "🇱🇨"),
        "VC" to ("St. Vincent & Grenadines" to "🇻🇨"),
        "WS" to ("Samoa" to "🇼🇸"),
        "SM" to ("San Marino" to "🇸🇲"),
        "ST" to ("Sao Tome & Principe" to "🇸🇹"),
        "SA" to ("Saudi Arabia" to "🇸🇦"),
        "SN" to ("Senegal" to "🇸🇳"),
        "RS" to ("Serbia" to "🇷🇸"),
        "SC" to ("Seychelles" to "🇸🇨"),
        "SL" to ("Sierra Leone" to "🇸🇱"),
        "SG" to ("Singapore" to "🇸🇬"),
        "SK" to ("Slovakia" to "🇸🇰"),
        "SI" to ("Slovenia" to "🇸🇮"),
        "SB" to ("Solomon Islands" to "🇸🇧"),
        "SO" to ("Somalia" to "🇸🇴"),
        "ZA" to ("South Africa" to "🇿🇦"),
        "SS" to ("South Sudan" to "🇸🇸"),
        "ES" to ("Spain" to "🇪🇸"),
        "LK" to ("Sri Lanka" to "🇱🇰"),
        "SD" to ("Sudan" to "🇸🇩"),
        "SR" to ("Suriname" to "🇸🇷"),
        "SE" to ("Sweden" to "🇸🇪"),
        "CH" to ("Switzerland" to "🇨🇭"),
        "SY" to ("Syria" to "🇸🇾"),
        "TW" to ("Taiwan" to "🇹🇼"),
        "TJ" to ("Tajikistan" to "🇹🇯"),
        "TZ" to ("Tanzania" to "🇹🇿"),
        "TH" to ("Thailand" to "🇹🇭"),
        "TL" to ("Timor-Leste" to "🇹🇱"),
        "TG" to ("Togo" to "🇹🇬"),
        "TO" to ("Tonga" to "🇹🇴"),
        "TT" to ("Trinidad & Tobago" to "🇹🇹"),
        "TN" to ("Tunisia" to "🇹🇳"),
        "TR" to ("Turkey" to "🇹🇷"),
        "TM" to ("Turkmenistan" to "🇹🇲"),
        "TV" to ("Tuvalu" to "🇹🇻"),
        "UG" to ("Uganda" to "🇺🇬"),
        "UA" to ("Ukraine" to "🇺🇦"),
        "AE" to ("United Arab Emirates" to "🇦🇪"),
        "GB" to ("United Kingdom" to "🇬🇧"),
        "US" to ("United States" to "🇺🇸"),
        "UY" to ("Uruguay" to "🇺🇾"),
        "UZ" to ("Uzbekistan" to "🇺🇿"),
        "VU" to ("Vanuatu" to "🇻🇺"),
        "VA" to ("Vatican City" to "🇻🇦"),
        "VE" to ("Venezuela" to "🇻🇪"),
        "VN" to ("Vietnam" to "🇻🇳"),
        "YE" to ("Yemen" to "🇾🇪"),
        "ZM" to ("Zambia" to "🇿🇲"),
        "ZW" to ("Zimbabwe" to "🇿🇼")
    )

    private val aliasMap = mapOf(
        "UK" to "GB",
        "UNITED KINDOM" to "GB",
        "UNITED KINGDOM" to "GB",
        "GREAT BRITAIN" to "GB",
        "ENGLAND" to "GB",
        "SCOTLAND" to "GB",
        "USA" to "US",
        "UNITED STATES" to "US",
        "UNITED STATES OF AMERICA" to "US",
        "AMERICA" to "US",
        "VIETNAM" to "VN",
        "VIET NAM" to "VN",
        "VITE NAM" to "VN",
        "VINTENM" to "VN",
        "TURKEY" to "TR",
        "TURKIYE" to "TR",
        "TÜRKIYE" to "TR",
        "RUSSIA" to "RU",
        "RUSSIAN FEDERATION" to "RU",
        "KOREA" to "KR",
        "SOUTH KOREA" to "KR",
        "KOREA REPUBLIC OF" to "KR",
        "ROK" to "KR",
        "UAE" to "AE",
        "UNITED ARAB EMIRATES" to "AE",
        "SAUDI ARABIA" to "SA",
        "KSA" to "SA",
        "CZECHIA" to "CZ",
        "CZECH REPUBLIC" to "CZ",
        "TAIWAN" to "TW",
        "HONG KONG" to "HK",
        "HONGKONG" to "HK"
    )

    fun getCountryName(countryCode: String): String {
        val code = countryCode.uppercase().trim()
        if (code.isBlank() || code == "UN" || code == "XX") return "Global / Unknown"
        return countryMap[code]?.first ?: try {
            Locale("", code).displayCountry
        } catch (e: Exception) {
            "Country ($code)"
        }
    }

    fun getCodeFromName(countryName: String): String? {
        if (countryName.isBlank()) return null
        val clean = countryName.uppercase().trim().replace(Regex("[^A-Z\\s]"), "")
        if (aliasMap.containsKey(clean)) return aliasMap[clean]

        for ((code, entry) in countryMap) {
            val officialUpper = entry.first.uppercase()
            if (officialUpper == clean || clean.contains(officialUpper) || officialUpper.contains(clean)) {
                return code
            }
        }
        return null
    }

    fun getFlagEmoji(countryCode: String): String {
        val code = countryCode.uppercase().trim()
        if (countryMap.containsKey(code)) {
            return countryMap[code]!!.second
        }
        if (code.length != 2) return "🌐"
        return try {
            val firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
            String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
        } catch (e: Exception) {
            "🌐"
        }
    }

    fun getContinent(countryCode: String): String {
        val code = countryCode.uppercase().trim()
        return when (code) {
            // Europe
            "AD", "AL", "AT", "BA", "BE", "BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GB", "GE", "GR", "HR", "HU", "IE", "IS", "IT", "LI", "LT", "LU", "LV", "MC", "MD", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SK", "SM", "UA", "VA" -> "Europe"
            
            // Asia
            "AF", "AM", "AZ", "BD", "BH", "BN", "BT", "CN", "HK", "ID", "IL", "IN", "IQ", "IR", "JO", "JP", "KG", "KH", "KP", "KR", "KW", "KZ", "LA", "LB", "LK", "MM", "MN", "MO", "MV", "MY", "NP", "OM", "PH", "PK", "PS", "QA", "SA", "SG", "SY", "TH", "TJ", "TM", "TR", "TW", "UZ", "VN", "YE" -> "Asia"
            
            // America
            "AG", "AR", "BB", "BO", "BR", "BS", "BZ", "CA", "CL", "CO", "CR", "CU", "DM", "DO", "EC", "GD", "GT", "GY", "HN", "HT", "JM", "KN", "LC", "MX", "NI", "PA", "PE", "PY", "SV", "SR", "TT", "US", "UY", "VC", "VE" -> "America"
            
            // Africa
            "AO", "BF", "BI", "BJ", "BW", "CD", "CF", "CG", "CI", "CM", "CV", "DJ", "DZ", "EG", "ER", "ET", "GA", "GH", "GM", "GN", "GQ", "GW", "KE", "KM", "LR", "LS", "LY", "MA", "MG", "ML", "MR", "MU", "MW", "MZ", "NA", "NE", "NG", "RW", "SC", "SD", "SL", "SN", "SO", "SS", "ST", "SZ", "TD", "TG", "TN", "TZ", "UG", "ZA", "ZM", "ZW" -> "Africa"
            
            // Oceania
            "AU", "FJ", "KI", "MH", "FM", "NR", "NZ", "PW", "PG", "SB", "TO", "TV", "VU", "WS" -> "Oceania"
            
            else -> "Other"
        }
    }
}

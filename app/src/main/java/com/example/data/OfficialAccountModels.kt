package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class WorldCountry(
    val flag: String,
    val nameAr: String,
    val nameEn: String,
    val dialCode: String,
    val isoCode: String
)

@Entity(tableName = "official_accounts")
data class OfficialAccountProfile(
    @PrimaryKey val accountId: String, // 5+ digits ONLY, no letters (e.g. "584920")
    val firstName: String,
    val fatherName: String,
    val grandfatherName: String,
    val familyName: String,
    val countryDialCode: String,
    val countryNameAr: String,
    val countryFlag: String,
    val phoneNumberLocal: String, // Digits only
    val fullPhoneDigits: String, // Dial code digits + local digits
    val email: String,
    val passwordPlainOrHash: String,
    val balanceYer: Double = 250000.0,
    val balanceSar: Double = 5000.0,
    val balanceUsd: Double = 1500.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullQuadName: String
        get() = "$firstName $fatherName $grandfatherName $familyName".replace(Regex("\\s+"), " ").trim()

    val formattedInternationalPhone: String
        get() = "$countryDialCode $phoneNumberLocal"
}

data class PendingTransferVerification(
    val recipientAccount: OfficialAccountProfile,
    val enteredDestinationDigits: String,
    val matchedBy: String, // "ACCOUNT_ID" or "PHONE_NUMBER"
    val amount: Double,
    val formattedAmount: String,
    val currency: WalletCurrency,
    val note: String,
    val isOnlineAvailable: Boolean
)

object WorldCountriesCatalog {
    val allCountries: List<WorldCountry> = listOf(
        WorldCountry("🇾🇪", "اليمن", "Yemen", "+967", "YE"),
        WorldCountry("🇸🇦", "المملكة العربية السعودية", "Saudi Arabia", "+966", "SA"),
        WorldCountry("🇦🇪", "الإمارات العربية المتحدة", "United Arab Emirates", "+971", "AE"),
        WorldCountry("🇶🇦", "قطر", "Qatar", "+974", "QA"),
        WorldCountry("🇰🇼", "الكويت", "Kuwait", "+965", "KW"),
        WorldCountry("🇴🇲", "سلطنة عُمان", "Oman", "+968", "OM"),
        WorldCountry("🇧🇭", "البحرين", "Bahrain", "+973", "BH"),
        WorldCountry("🇪🇬", "مصر", "Egypt", "+20", "EG"),
        WorldCountry("🇯🇴", "الأردن", "Jordan", "+962", "JO"),
        WorldCountry("🇮🇶", "العراق", "Iraq", "+964", "IQ"),
        WorldCountry("🇵🇸", "فلسطين", "Palestine", "+970", "PS"),
        WorldCountry("🇸🇾", "سوريا", "Syria", "+963", "SY"),
        WorldCountry("🇱🇧", "لبنان", "Lebanon", "+961", "LB"),
        WorldCountry("🇸🇩", "السودان", "Sudan", "+249", "SD"),
        WorldCountry("🇱🇾", "ليبيا", "Libya", "+218", "LY"),
        WorldCountry("🇹🇳", "تونس", "Tunisia", "+216", "TN"),
        WorldCountry("🇩🇿", "الجزائر", "Algeria", "+213", "DZ"),
        WorldCountry("🇲🇦", "المغرب", "Morocco", "+212", "MA"),
        WorldCountry("🇲🇷", "موريتانيا", "Mauritania", "+222", "MR"),
        WorldCountry("🇸🇴", "الصومال", "Somalia", "+252", "SO"),
        WorldCountry("🇩🇯", "جيبوتي", "Djibouti", "+253", "DJ"),
        WorldCountry("🇰🇲", "جزر القمر", "Comoros", "+269", "KM"),
        WorldCountry("🇹🇷", "تركيا", "Turkey", "+90", "TR"),
        WorldCountry("🇲🇾", "ماليزيا", "Malaysia", "+60", "MY"),
        WorldCountry("🇮🇩", "إندونيسيا", "Indonesia", "+62", "ID"),
        WorldCountry("🇵🇰", "باكستان", "Pakistan", "+92", "PK"),
        WorldCountry("🇮🇳", "الهند", "India", "+91", "IN"),
        WorldCountry("🇧🇩", "بنغلاديش", "Bangladesh", "+880", "BD"),
        WorldCountry("🇨🇳", "الصين", "China", "+86", "CN"),
        WorldCountry("🇯🇵", "اليابان", "Japan", "+81", "JP"),
        WorldCountry("🇰🇷", "كوريا الجنوبية", "South Korea", "+82", "KR"),
        WorldCountry("🇸🇬", "سنغافورة", "Singapore", "+65", "SG"),
        WorldCountry("🇹🇭", "تايلاند", "Thailand", "+66", "TH"),
        WorldCountry("🇵🇭", "الفلبين", "Philippines", "+63", "PH"),
        WorldCountry("🇻🇳", "فيتنام", "Vietnam", "+84", "VN"),
        WorldCountry("🇬🇧", "المملكة المتحدة (بريطانيا)", "United Kingdom", "+44", "GB"),
        WorldCountry("🇩🇪", "ألمانيا", "Germany", "+49", "DE"),
        WorldCountry("🇫🇷", "فرنسا", "France", "+33", "FR"),
        WorldCountry("🇮🇹", "إيطاليا", "Italy", "+39", "IT"),
        WorldCountry("🇪🇸", "إسبانيا", "Spain", "+34", "ES"),
        WorldCountry("🇳🇱", "هولندا", "Netherlands", "+31", "NL"),
        WorldCountry("🇧🇪", "بلجيكا", "Belgium", "+32", "BE"),
        WorldCountry("🇨🇭", "سويسرا", "Switzerland", "+41", "CH"),
        WorldCountry("🇦🇹", "النمسا", "Austria", "+43", "AT"),
        WorldCountry("🇸🇪", "السويد", "Sweden", "+46", "SE"),
        WorldCountry("🇳🇴", "النرويج", "Norway", "+47", "NO"),
        WorldCountry("🇩🇰", "الدنمارك", "Denmark", "+45", "DK"),
        WorldCountry("🇫🇮", "فنلندا", "Finland", "+358", "FI"),
        WorldCountry("🇵🇱", "بولندا", "Poland", "+48", "PL"),
        WorldCountry("🇬🇷", "اليونان", "Greece", "+30", "GR"),
        WorldCountry("🇵🇹", "البرتغال", "Portugal", "+351", "PT"),
        WorldCountry("🇮🇪", "أيرلندا", "Ireland", "+353", "IE"),
        WorldCountry("🇷🇺", "روسيا", "Russia", "+7", "RU"),
        WorldCountry("🇺🇦", "أوكرانيا", "Ukraine", "+380", "UA"),
        WorldCountry("🇺🇸", "الولايات المتحدة الأمريكية", "United States", "+1", "US"),
        WorldCountry("🇨🇦", "كندا", "Canada", "+1", "CA"),
        WorldCountry("🇲🇽", "المكسيك", "Mexico", "+52", "MX"),
        WorldCountry("🇧🇷", "البرازيل", "Brazil", "+55", "BR"),
        WorldCountry("🇦🇷", "الأرجنتين", "Argentina", "+54", "AR"),
        WorldCountry("🇨🇱", "تشيلي", "Chile", "+56", "CL"),
        WorldCountry("🇨🇴", "كولومبيا", "Colombia", "+57", "CO"),
        WorldCountry("🇦🇺", "أستراليا", "Australia", "+61", "AU"),
        WorldCountry("🇳🇿", "نيوزيلندا", "New Zealand", "+64", "NZ"),
        WorldCountry("🇿🇦", "جنوب أفريقيا", "South Africa", "+27", "ZA"),
        WorldCountry("🇳🇬", "نيجيريا", "Nigeria", "+234", "NG"),
        WorldCountry("🇰🇪", "كينيا", "Kenya", "+254", "KE"),
        WorldCountry("🇪🇹", "إثيوبيا", "Ethiopia", "+251", "ET"),
        WorldCountry("🇬🇭", "غانا", "Ghana", "+233", "GH"),
        WorldCountry("🇹🇿", "تنزانيا", "Tanzania", "+255", "TZ"),
        WorldCountry("🇺🇬", "أوغندا", "Uganda", "+256", "UG"),
        WorldCountry("🇸🇳", "السنغال", "Senegal", "+221", "SN"),
        WorldCountry("🇲🇱", "مالي", "Mali", "+223", "ML"),
        WorldCountry("🇳🇪", "النيجر", "Niger", "+227", "NE"),
        WorldCountry("🇹🇩", "تشاد", "Chad", "+235", "TD")
    )

    fun search(query: String): List<WorldCountry> {
        val clean = query.trim()
        if (clean.isEmpty()) return allCountries
        val digitsOnly = clean.filter { it.isDigit() }
        return allCountries.filter { country ->
            country.nameAr.contains(clean, ignoreCase = true) ||
                country.nameEn.contains(clean, ignoreCase = true) ||
                country.dialCode.contains(clean, ignoreCase = true) ||
                (digitsOnly.isNotEmpty() && country.dialCode.filter { it.isDigit() }.contains(digitsOnly)) ||
                country.isoCode.contains(clean, ignoreCase = true)
        }
    }

    val defaultSeedAccounts: List<OfficialAccountProfile> = listOf(
        OfficialAccountProfile(
            accountId = "584920",
            firstName = "أحمد",
            fatherName = "محمد",
            grandfatherName = "عبدالله",
            familyName = "الشامي",
            countryDialCode = "+967",
            countryNameAr = "اليمن",
            countryFlag = "🇾🇪",
            phoneNumberLocal = "771234567",
            fullPhoneDigits = "967771234567",
            email = "ahmed.shami@vibrapay.com",
            passwordPlainOrHash = "123456",
            balanceYer = 350000.0,
            balanceSar = 6200.0,
            balanceUsd = 1850.0
        ),
        OfficialAccountProfile(
            accountId = "620145",
            firstName = "سالم",
            fatherName = "عبدالرحمن",
            grandfatherName = "علي",
            familyName = "العولقي",
            countryDialCode = "+966",
            countryNameAr = "المملكة العربية السعودية",
            countryFlag = "🇸🇦",
            phoneNumberLocal = "501234567",
            fullPhoneDigits = "966501234567",
            email = "salem.awlaqi@vibrapay.com",
            passwordPlainOrHash = "123456",
            balanceYer = 180000.0,
            balanceSar = 12400.0,
            balanceUsd = 3200.0
        ),
        OfficialAccountProfile(
            accountId = "739410",
            firstName = "خالد",
            fatherName = "صالح",
            grandfatherName = "منصور",
            familyName = "الحميري",
            countryDialCode = "+967",
            countryNameAr = "اليمن",
            countryFlag = "🇾🇪",
            phoneNumberLocal = "733987654",
            fullPhoneDigits = "967733987654",
            email = "khaled.himyari@vibrapay.com",
            passwordPlainOrHash = "123456",
            balanceYer = 420000.0,
            balanceSar = 4500.0,
            balanceUsd = 1100.0
        ),
        OfficialAccountProfile(
            accountId = "845120",
            firstName = "فهد",
            fatherName = "عبدالعزيز",
            grandfatherName = "ناصر",
            familyName = "الدوسري",
            countryDialCode = "+966",
            countryNameAr = "المملكة العربية السعودية",
            countryFlag = "🇸🇦",
            phoneNumberLocal = "559876543",
            fullPhoneDigits = "966559876543",
            email = "fahad.dosari@vibrapay.com",
            passwordPlainOrHash = "123456",
            balanceYer = 95000.0,
            balanceSar = 19500.0,
            balanceUsd = 4600.0
        ),
        OfficialAccountProfile(
            accountId = "912304",
            firstName = "نبيل",
            fatherName = "قاسم",
            grandfatherName = "يحيى",
            familyName = "المتوكل",
            countryDialCode = "+967",
            countryNameAr = "اليمن",
            countryFlag = "🇾🇪",
            phoneNumberLocal = "711456789",
            fullPhoneDigits = "967711456789",
            email = "nabil.mutawakel@vibrapay.com",
            passwordPlainOrHash = "123456",
            balanceYer = 510000.0,
            balanceSar = 3800.0,
            balanceUsd = 980.0
        )
    )
}

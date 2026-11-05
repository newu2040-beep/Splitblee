package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val age: Long? = null,
    val gender: String? = null,
    val address: String? = null,
    val preferredLanguage: String = "en",
    val defaultCurrency: String = "USD",
    val theme: String = "Ocean Blue",
    val themeMode: String = "Light",
    val notificationsEnabled: Boolean = true,
    val locationSharingEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseGroup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val settlementCurrency: String = "USD",
    val memberNames: List<String> = emptyList(),
    val memberCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FriendProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String? = null,
    val age: Long? = null,
    val gender: String = "Male",
    val address: String? = null,
    val status: String = "Accepted",
    val isOnline: Boolean = true,
    val locationSharing: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val owesYouMinor: Long = 0L,
    val youOweMinor: Long = 0L,
    val splitsCount: Long = 0L,
    val currency: String = "USD",
    val avatarIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val groupId: String? = null,
    val groupName: String? = null,
    val title: String = "",
    val category: String = "Food & Drink",
    val notes: String? = null,
    val place: String = "",
    val expenseDate: String = "",
    val monthKey: String = "",
    val currency: String = "USD",
    val subtotalMinor: Long = 0L,
    val taxPercent: Long = 0L,
    val taxMinor: Long = 0L,
    val serviceChargeMinor: Long = 0L,
    val tipMinor: Long = 0L,
    val extraAmountMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val totalMinor: Long = 0L,
    val splitMethod: String = "EQUAL",
    val status: String = "UNPAID",
    val paidMinor: Long = 0L,
    val owedToYouMinor: Long = 0L,
    val youOweMinor: Long = 0L,
    val participantCount: Long = 1L,
    val participantNames: List<String> = emptyList(),
    val participantSharesSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val expenseId: String = "",
    val expenseTitle: String = "",
    val groupId: String? = null,
    val payerName: String = "",
    val payeeName: String = "",
    val amountMinor: Long = 0L,
    val currency: String = "USD",
    val method: String = "UPI",
    val reference: String? = null,
    val entryType: String = "PAYMENT",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ParticipantShareDetail(
    val name: String,
    val assignedMinor: Long,
    val paidMinor: Long,
    val status: String // PAID, PARTIALLY_PAID, UNPAID, OVERPAID
) {
    val remainingMinor: Long
        get() = (assignedMinor - paidMinor).coerceAtLeast(0L)
}

object ParticipantShareCodec {
    fun encode(shares: List<ParticipantShareDetail>): String {
        return shares.joinToString("|") { s ->
            val safeName = s.name.replace("|", " ").replace(":", " ").trim().ifEmpty { "Member" }
            "$safeName:${s.assignedMinor}:${s.paidMinor}:${s.status}"
        }.take(1950)
    }

    fun decode(encoded: String, fallbackNames: List<String>, totalMinor: Long, expenseStatus: String): List<ParticipantShareDetail> {
        if (encoded.isNotBlank()) {
            val parsed = encoded.split("|").mapNotNull { part ->
                val tokens = part.split(":")
                when {
                    tokens.size >= 4 -> {
                        val name = tokens[0]
                        val assigned = tokens[1].toLongOrNull() ?: 0L
                        val paid = tokens[2].toLongOrNull() ?: 0L
                        val st = tokens[3]
                        ParticipantShareDetail(name, assigned, paid, st)
                    }
                    tokens.size == 3 -> {
                        val name = tokens[0]
                        val assigned = tokens[1].toLongOrNull() ?: 0L
                        val st = tokens[2]
                        val paid = if (st == "PAID") assigned else 0L
                        ParticipantShareDetail(name, assigned, paid, st)
                    }
                    else -> null
                }
            }
            if (parsed.isNotEmpty()) return parsed
        }
        val names = if (fallbackNames.isNotEmpty()) fallbackNames else listOf("You")
        val shares = SplitCalculator.calculateEqualShares(totalMinor, names.size)
        return names.mapIndexed { idx, name ->
            val assigned = shares.getOrElse(idx) { 0L }
            val isPaid = expenseStatus == "PAID" || idx == 0
            ParticipantShareDetail(
                name = name,
                assignedMinor = assigned,
                paidMinor = if (isPaid) assigned else 0L,
                status = if (isPaid) "PAID" else "UNPAID"
            )
        }
    }
}

data class CurrencySpec(
    val code: String,
    val name: String,
    val symbol: String,
    val minorUnits: Int,
    val approxUsdRate: Double // 1 USD in this currency for reference estimates
)

object CurrencyCatalog {
    val supported: List<CurrencySpec> = listOf(
        CurrencySpec("USD", "US Dollar", "$", 2, 1.0),
        CurrencySpec("NPR", "Nepalese Rupee", "रू ", 2, 134.50),
        CurrencySpec("INR", "Indian Rupee", "₹", 2, 84.10),
        CurrencySpec("PKR", "Pakistani Rupee", "Rs ", 2, 278.40),
        CurrencySpec("EUR", "Euro", "€", 2, 0.92),
        CurrencySpec("GBP", "British Pound", "£", 2, 0.78),
        CurrencySpec("AED", "UAE Dirham", "د.إ ", 2, 3.67),
        CurrencySpec("SAR", "Saudi Riyal", "ر.س ", 2, 3.75),
        CurrencySpec("QAR", "Qatari Riyal", "ر.ق ", 2, 3.64),
        CurrencySpec("KWD", "Kuwaiti Dinar", "د.ك ", 3, 0.307),
        CurrencySpec("AUD", "Australian Dollar", "A$", 2, 1.52),
        CurrencySpec("CAD", "Canadian Dollar", "C$", 2, 1.39),
        CurrencySpec("JPY", "Japanese Yen", "¥", 0, 153.20),
        CurrencySpec("BDT", "Bangladeshi Taka", "৳", 2, 119.50),
        CurrencySpec("LKR", "Sri Lankan Rupee", "Rs ", 2, 293.00)
    )

    fun specFor(code: String): CurrencySpec {
        return supported.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: supported.first()
    }

    fun formatMinor(amountMinor: Long, currencyCode: String, showPlusSign: Boolean = false): String {
        val spec = specFor(currencyCode)
        val isNegative = amountMinor < 0
        val absMinor = abs(amountMinor)
        val divisor = when (spec.minorUnits) {
            0 -> BigDecimal.ONE
            3 -> BigDecimal(1000)
            else -> BigDecimal(100)
        }
        val majorValue = BigDecimal(absMinor).divide(divisor, spec.minorUnits, RoundingMode.HALF_UP)
        val pattern = when (spec.minorUnits) {
            0 -> "#,##0"
            3 -> "#,##0.000"
            else -> "#,##0.00"
        }
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        val formattedNumber = formatter.format(majorValue)
        val signPrefix = when {
            isNegative -> "-"
            showPlusSign && amountMinor > 0 -> "+"
            else -> ""
        }
        return "$signPrefix${spec.symbol}$formattedNumber"
    }

    fun parseMajorToMinor(input: String, currencyCode: String): Long {
        val cleaned = input.replace(",", "").trim()
        if (cleaned.isEmpty()) return 0L
        val spec = specFor(currencyCode)
        return try {
            val bd = BigDecimal(cleaned)
            val multiplier = when (spec.minorUnits) {
                0 -> BigDecimal.ONE
                3 -> BigDecimal(1000)
                else -> BigDecimal(100)
            }
            bd.multiply(multiplier).setScale(0, RoundingMode.HALF_UP).toLong().coerceAtLeast(0L)
        } catch (e: Exception) {
            0L
        }
    }

    fun convertMinorEstimate(amountMinor: Long, fromCurrency: String, toCurrency: String): Long {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amountMinor
        val fromSpec = specFor(fromCurrency)
        val toSpec = specFor(toCurrency)
        val fromDivisor = when (fromSpec.minorUnits) {
            0 -> 1.0
            3 -> 1000.0
            else -> 100.0
        }
        val toMultiplier = when (toSpec.minorUnits) {
            0 -> 1.0
            3 -> 1000.0
            else -> 100.0
        }
        val majorInFrom = amountMinor.toDouble() / fromDivisor
        val majorInUsd = majorInFrom / fromSpec.approxUsdRate
        val majorInTo = majorInUsd * toSpec.approxUsdRate
        return Math.round(majorInTo * toMultiplier)
    }
}

object SplitCalculator {
    /**
     * Deterministically distributes totalMinor across count participants so all shares sum
     * strictly to totalMinor without losing or gaining minor units.
     */
    fun calculateEqualShares(totalMinor: Long, count: Int): List<Long> {
        val safeCount = count.coerceAtLeast(1)
        val baseShare = totalMinor / safeCount
        val remainder = (totalMinor % safeCount).toInt()
        return List(safeCount) { index ->
            if (index < remainder) baseShare + 1L else baseShare
        }
    }

    /**
     * Distributes totalMinor by percentages (which must sum to 100) and distributes any
     * rounding remainder deterministically to the first participants.
     */
    fun calculatePercentageShares(totalMinor: Long, percentages: List<Int>): List<Long> {
        if (percentages.isEmpty()) return listOf(totalMinor)
        val rawShares = percentages.map { pct -> (totalMinor * pct) / 100L }.toMutableList()
        val allocated = rawShares.sum()
        var remainder = totalMinor - allocated
        var i = 0
        while (remainder > 0 && rawShares.isNotEmpty()) {
            rawShares[i % rawShares.size] += 1L
            remainder -= 1L
            i++
        }
        return rawShares
    }

    fun computeTotalMinor(
        subtotalMinor: Long,
        taxPercent: Int,
        serviceChargeMinor: Long,
        tipMinor: Long,
        extraAmountMinor: Long,
        discountMinor: Long
    ): Pair<Long, Long> {
        val taxMinor = ((subtotalMinor * taxPercent.coerceIn(0, 100)) + 50L) / 100L
        val gross = subtotalMinor + taxMinor + serviceChargeMinor + tipMinor + extraAmountMinor
        val finalTotal = (gross - discountMinor).coerceAtLeast(1L)
        return taxMinor to finalTotal
    }

    fun deriveExpenseStatus(totalMinor: Long, paidMinor: Long): String {
        return when {
            paidMinor <= 0L -> "UNPAID"
            paidMinor < totalMinor -> "PARTIALLY_PAID"
            paidMinor == totalMinor -> "PAID"
            else -> "OVERPAID"
        }
    }
}

data class LocalizedStrings(
    val tagline: String,
    val loginSubtitle: String,
    val continueWithGoogle: String,
    val continueWithEmail: String,
    val guestMode: String,
    val hello: String,
    val dashboardSubtitle: String,
    val totalBalance: String,
    val newSplit: String,
    val groups: String,
    val history: String,
    val map: String,
    val recentSplits: String,
    val seeAll: String,
    val home: String,
    val profile: String,
    val splitDetails: String,
    val billSummary: String,
    val totalBill: String,
    val discount: String,
    val finalAmount: String,
    val splitAmong: String,
    val markAsUnpaid: String,
    val markAsPaid: String,
    val recordPayment: String,
    val shareDetails: String,
    val settings: String,
    val editProfile: String,
    val appPreferences: String,
    val theme: String,
    val language: String,
    val currency: String,
    val notifications: String,
    val dataAndExport: String,
    val exportDetails: String,
    val about: String,
    val helpAndSupport: String,
    val aboutSplitBlee: String
)

object AppLocalization {
    fun forLanguage(code: String): LocalizedStrings = when (code) {
        "ne" -> LocalizedStrings(
            tagline = "बाँड्नुहोस्। साझा गर्नुहोस्। रमाउनुहोस्।",
            loginSubtitle = "आफ्ना साथीहरूसँग सजिलै खर्च बाँड्नुहोस् र साझा गर्नुहोस्।",
            continueWithGoogle = "Google मार्फत जारी राख्नुहोस्",
            continueWithEmail = "इमेल मार्फत जानकारी",
            guestMode = "अतिथि पूर्वावलोकन",
            hello = "नमस्ते,",
            dashboardSubtitle = "आज केही खर्च बाँडौं!",
            totalBalance = "कुल मौज्दात",
            newSplit = "नयाँ बिल",
            groups = "समूहहरू",
            history = "इतिहास",
            map = "नक्सा",
            recentSplits = "हालका बिलहरू",
            seeAll = "सबै हेर्नुहोस्",
            home = "गृह",
            profile = "प्रोफाइल",
            splitDetails = "बिल विवरण",
            billSummary = "बिल सारांश",
            totalBill = "जम्मा बिल",
            discount = "छुट",
            finalAmount = "अन्तिम रकम",
            splitAmong = "बाँडफाँड",
            markAsUnpaid = "तिर्न बाँकी चिन्ह लगाउनुहोस्",
            markAsPaid = "भुक्तानी भयो चिन्ह लगाउनुहोस्",
            recordPayment = "भुक्तानी दर्ता गर्नुहोस्",
            shareDetails = "विवरण साझा गर्नुहोस्",
            settings = "सेटिङहरू",
            editProfile = "प्रोफाइल सम्पादन",
            appPreferences = "एप प्राथमिकताहरू",
            theme = "थिम",
            language = "भाषा",
            currency = "मुद्रा",
            notifications = "सूचनाहरू",
            dataAndExport = "डाटा र निर्यात",
            exportDetails = "विवरण निर्यात गर्नुहोस्",
            about = "बारेमा",
            helpAndSupport = "मद्दत र समर्थन",
            aboutSplitBlee = "SplitBlee को बारेमा"
        )
        "hi" -> LocalizedStrings(
            tagline = "बांटें। साझा करें। आनंद लें।",
            loginSubtitle = "अपने दोस्तों के साथ आसानी से खर्च बांटें और साझा करें।",
            continueWithGoogle = "Google के साथ जारी रखें",
            continueWithEmail = "ईमेल सहायता जानकारी",
            guestMode = "अतिथि पूर्वावलोकन",
            hello = "नमस्ते,",
            dashboardSubtitle = "चलिए आज कुछ खर्च बांटते हैं!",
            totalBalance = "कुल शेष राशि",
            newSplit = "नया बिल",
            groups = "समूह",
            history = "इतिहास",
            map = "मानचित्र",
            recentSplits = "हाल के बिल",
            seeAll = "सभी देखें",
            home = "होम",
            profile = "प्रोफ़ाइल",
            splitDetails = "बिल विवरण",
            billSummary = "बिल सारांश",
            totalBill = "कुल बिल",
            discount = "छूट",
            finalAmount = "अंतिम राशि",
            splitAmong = "इनके बीच बंटवारा",
            markAsUnpaid = "अदत्त के रूप में चिह्नित करें",
            markAsPaid = "भुगतान के रूप में चिह्नित करें",
            recordPayment = "भुगतान दर्ज करें",
            shareDetails = "विवरण साझा करें",
            settings = "सेटिंग्स",
            editProfile = "प्रोफ़ाइल संपादित करें",
            appPreferences = "ऐप प्राथमिकताएं",
            theme = "थीम",
            language = "भाषा",
            currency = "मुद्रा",
            notifications = "सूचनाएं",
            dataAndExport = "डेटा और निर्यात",
            exportDetails = "विवरण निर्यात करें",
            about = "के बारे में",
            helpAndSupport = "सहायता और समर्थन",
            aboutSplitBlee = "SplitBlee के बारे में"
        )
        else -> LocalizedStrings(
            tagline = "Split. Share. Enjoy.",
            loginSubtitle = "Easily split and share the costs\nwith your friends.",
            continueWithGoogle = "Continue with Google",
            continueWithEmail = "Continue with Email",
            guestMode = "Guest Mode",
            hello = "Hello,",
            dashboardSubtitle = "Let's split something today!",
            totalBalance = "Total Balance",
            newSplit = "New Split",
            groups = "Groups",
            history = "History",
            map = "Map",
            recentSplits = "Recent Splits",
            seeAll = "See all",
            home = "Home",
            profile = "Profile",
            splitDetails = "Split Details",
            billSummary = "Bill Summary",
            totalBill = "Total Bill",
            discount = "Discount",
            finalAmount = "Final Amount",
            splitAmong = "Split Among",
            markAsUnpaid = "Mark as Unpaid",
            markAsPaid = "Mark as Paid",
            recordPayment = "Record Payment",
            shareDetails = "Share Details",
            settings = "Settings",
            editProfile = "Edit Profile",
            appPreferences = "App Preferences",
            theme = "Theme",
            language = "Language",
            currency = "Currency",
            notifications = "Notifications",
            dataAndExport = "Data & Export",
            exportDetails = "Export Details",
            about = "About",
            helpAndSupport = "Help & Support",
            aboutSplitBlee = "About SplitBlee"
        )
    }

    fun languageDisplayName(code: String): String = when (code) {
        "ne" -> "Nepali (नेपाली)"
        "hi" -> "Hindi (हिन्दी)"
        else -> "English"
    }
}

package com.example

import com.example.model.CurrencyCatalog
import com.example.model.ParticipantShareCodec
import com.example.model.ParticipantShareDetail
import com.example.model.SplitCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun equalShares_distributesRemainderDeterministicallyAndSumsExactlyToTotal() {
        val totalMinor = 7693L // $76.93 across 4 people -> 1924, 1923, 1923, 1923
        val shares = SplitCalculator.calculateEqualShares(totalMinor, 4)
        assertEquals(4, shares.size)
        assertEquals(totalMinor, shares.sum())
        assertEquals(listOf(1924L, 1923L, 1923L, 1923L), shares)
    }

    @Test
    fun percentageShares_reconcilesRemainderToExactTotal() {
        val totalMinor = 10000L
        val shares = SplitCalculator.calculatePercentageShares(totalMinor, listOf(33, 33, 34))
        assertEquals(totalMinor, shares.sum())
    }

    @Test
    fun computeTotalMinor_calculatesTaxAndDiscountCorrectly() {
        // $72.50 subtotal + 13% tax ($9.43) - $5.00 discount = $76.93 (7693 minor units)
        val (taxMinor, finalTotalMinor) = SplitCalculator.computeTotalMinor(
            subtotalMinor = 7250L,
            taxPercent = 13,
            serviceChargeMinor = 0L,
            tipMinor = 0L,
            extraAmountMinor = 0L,
            discountMinor = 500L
        )
        assertEquals(943L, taxMinor)
        assertEquals(7693L, finalTotalMinor)
    }

    @Test
    fun currencyCatalog_formatsMinorUnitsWithCorrectPrecision() {
        assertEquals("$1,250.00", CurrencyCatalog.formatMinor(125000L, "USD"))
        assertEquals("रू 1,250.00", CurrencyCatalog.formatMinor(125000L, "NPR"))
        assertEquals("₹1,250.00", CurrencyCatalog.formatMinor(125000L, "INR"))
        assertEquals("Rs 1,250.00", CurrencyCatalog.formatMinor(125000L, "PKR"))
        assertEquals("¥1,250", CurrencyCatalog.formatMinor(1250L, "JPY"))
        assertEquals("د.ك 1,250.000", CurrencyCatalog.formatMinor(1250000L, "KWD"))
    }

    @Test
    fun participantShareCodec_roundTripsSharesAccurately() {
        val original = listOf(
            ParticipantShareDetail("Rahul Shah", 1924L, 1924L, "PAID"),
            ParticipantShareDetail("Aarav Thapa", 1923L, 0L, "UNPAID")
        )
        val encoded = ParticipantShareCodec.encode(original)
        val decoded = ParticipantShareCodec.decode(encoded, listOf("Rahul Shah", "Aarav Thapa"), 3847L, "PARTIALLY_PAID")
        assertEquals(2, decoded.size)
        assertEquals(1924L, decoded[0].assignedMinor)
        assertEquals("PAID", decoded[0].status)
        assertEquals(1923L, decoded[1].remainingMinor)
        assertEquals("UNPAID", decoded[1].status)
    }
}

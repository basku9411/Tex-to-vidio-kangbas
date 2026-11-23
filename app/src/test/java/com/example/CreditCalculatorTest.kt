package com.example

import com.example.model.AiModel
import com.example.model.ApiKeyBalance
import com.example.model.CreditCalculator
import com.example.model.ResolutionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditCalculatorTest {

    @Test
    fun `sumValidCredits sums all valid balances across keys`() {
        val balances = listOf(
            ApiKeyBalance(key = "k1", balance = 45.5),
            ApiKeyBalance(key = "k2", balance = null, error = "Invalid key"),
            ApiKeyBalance(key = "k3", balance = 120.0),
            ApiKeyBalance(key = "k4", balance = 34.5),
        )
        val total = CreditCalculator.sumValidCredits(balances)
        assertEquals(200.0, total ?: -1.0, 0.001)
    }

    @Test
    fun `sumValidCredits treats negative key balances as zero so other keys stay intact`() {
        val balances = listOf(
            ApiKeyBalance(key = "k1", balance = 20.0),
            ApiKeyBalance(key = "k2", balance = -4.0),
            ApiKeyBalance(key = "k3", balance = -4.0),
            ApiKeyBalance(key = "k4", balance = 8.0),
        )
        val total = CreditCalculator.sumValidCredits(balances)
        assertEquals(28.0, total ?: -1.0, 0.001)
    }

    @Test
    fun `sumValidCredits returns null when all keys fail`() {
        val balances = listOf(
            ApiKeyBalance(key = "k1", balance = null, error = "HTTP 401"),
            ApiKeyBalance(key = "k2", balance = null, error = "Timeout"),
        )
        assertNull(CreditCalculator.sumValidCredits(balances))
    }

    @Test
    fun `estimateCredits matches model pricing rules`() {
        assertEquals(
            20.0,
            CreditCalculator.estimateCredits(AiModel.SEEDANCE_V1_LITE_TEXT, 15, ResolutionOption.R480, false) ?: -1.0,
            0.001,
        )
        assertEquals(
            252.0,
            CreditCalculator.estimateCredits(AiModel.KLING_2_6_TEXT, 10, ResolutionOption.R720, true) ?: -1.0,
            0.001,
        )
        assertNull(
            CreditCalculator.estimateCredits(AiModel.SEEDANCE_2_5_FLASH, 5, ResolutionOption.R1080, false)
        )
    }
}

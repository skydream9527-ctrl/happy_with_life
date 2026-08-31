package com.xiaoquexing.app.data.remote

import org.junit.Assert.assertTrue
import org.junit.Test

class PlanStoreTest {
    @Test
    fun tiers_listBenefits() {
        assertTrue(PlanStore.freeBenefits.isNotEmpty())
        assertTrue(PlanStore.memberBenefits.any { it.contains("AI") })
    }
}

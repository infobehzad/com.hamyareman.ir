package com.hamyareman.ir.platform.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingStatusTest {
    @Test
    fun paidOnlyYearlyOrRefundPending() {
        assertTrue(BillingStatus.isPaid("yearly"))
        assertTrue(BillingStatus.isPaid("refund_pending"))
        assertFalse(BillingStatus.isPaid("pending"))
        assertFalse(BillingStatus.isPaid("free"))
        assertFalse(BillingStatus.isPaid("rejected"))
    }

    @Test
    fun chips() {
        assertEquals("اشتراک فعال", BillingStatus.chipFa("yearly"))
        assertEquals("انتظار تأیید پرداخت", BillingStatus.chipFa("pending"))
        assertEquals("انتظار بازگشت وجه", BillingStatus.chipFa("refund_pending"))
        assertEquals("مهمان همیار من", BillingStatus.chipFa("free"))
    }

    @Test
    fun adminOpsSharesBootstrapFunction() {
        assertEquals(FunctionIds.USER_BOOTSTRAP, FunctionIds.ADMIN_OPS)
    }
}

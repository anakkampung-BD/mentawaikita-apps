package com.obill.app.ui.saldo

import com.obill.app.ui.formatRupiahPlain

const val LOW_BALANCE_THRESHOLD_RP = 30_000.0

/** Di bawah ini: setelah OK dialog top-up, arahkan ke Beranda (bukan halaman Jual). */
const val SALE_ENTRY_CRITICAL_THRESHOLD_RP = 10_000.0

/** Di atas atau sama dengan ini: buka Jual tanpa dialog peringatan saldo. */
const val SALE_ENTRY_SUFFICIENT_THRESHOLD_RP = 30_000.0

enum class SaleSaldoTier {
    /** Saldo di bawah ambang kritis (10.000). */
    BelowCritical,

    /** Saldo menengah: perlu top-up, tetapi boleh lanjut ke transaksi setelah OK. */
    LowNeedTopUp,

    /** Saldo cukup: langsung ke Jual tanpa dialog. */
    Sufficient,
}

fun saleSaldoTierForEntry(saldo: Double?): SaleSaldoTier {
    val s = saldo ?: 0.0
    return when {
        s < SALE_ENTRY_CRITICAL_THRESHOLD_RP -> SaleSaldoTier.BelowCritical
        s < SALE_ENTRY_SUFFICIENT_THRESHOLD_RP -> SaleSaldoTier.LowNeedTopUp
        else -> SaleSaldoTier.Sufficient
    }
}

fun saldoNeedsLowBalanceWarning(saldo: Double?): Boolean {
    val s = saldo ?: 0.0
    return s < LOW_BALANCE_THRESHOLD_RP
}

fun formatSaldoForLowBalanceWarning(saldo: Double?): String {
    val s = saldo ?: 0.0
    return formatRupiahPlain(s).replace("Rp ", "Rp. ")
}

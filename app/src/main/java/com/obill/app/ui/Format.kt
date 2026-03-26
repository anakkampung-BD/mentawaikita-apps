package com.obill.app.ui

import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(value: Number): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return nf.format(value.toDouble())
}

fun formatRupiahPlain(value: Double): String {
    val n = value.toLong()
    return "Rp " + String.format(Locale.US, "%,d", n).replace(',', '.')
}

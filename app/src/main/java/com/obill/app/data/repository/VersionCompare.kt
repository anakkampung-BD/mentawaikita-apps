package com.obill.app.data.repository

/**
 * Bandingkan versi format "2.0.1" (dot-separated integers).
 * Return true jika `latest` lebih besar dari `current`.
 */
fun isNewerVersion(latest: String, current: String): Boolean {
    fun split(v: String): List<Int> =
        v.trim()
            .split('.')
            .mapNotNull { it.toIntOrNull() }

    val a = split(latest)
    val b = split(current)
    val max = maxOf(a.size, b.size)
    for (i in 0 until max) {
        val ai = a.getOrNull(i) ?: 0
        val bi = b.getOrNull(i) ?: 0
        if (ai != bi) return ai > bi
    }
    return false
}


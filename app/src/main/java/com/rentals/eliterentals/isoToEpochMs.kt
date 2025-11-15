package com.rentals.eliterentals

import java.time.Instant
import java.time.ZoneId

fun isoToEpochMs(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return try {
        Instant.parse(iso).toEpochMilli()
    } catch (e: Exception) {
        // fallback parse common format yyyy-MM-ddTHH:mm:ss
        try {
            val dt = java.time.LocalDateTime.parse(iso)
            dt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}

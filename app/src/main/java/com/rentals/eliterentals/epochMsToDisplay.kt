package com.rentals.eliterentals

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun epochMsToDisplay(epochMs: Long): String {
    if (epochMs <= 0) return "Unknown"
    val dt = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    val f = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    return dt.format(f)
}
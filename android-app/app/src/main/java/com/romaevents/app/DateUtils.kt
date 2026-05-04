package com.romaevents.app

object DateUtils {

    fun formatDateTime(value: String?): String {
        if (value.isNullOrBlank()) return "Data non disponibile"

        return try {
            val datePart = value.substringBefore("T")
            val parts = datePart.split("-")

            if (parts.size != 3) return value

            val year = parts[0]
            val month = parts[1]
            val day = parts[2]

            "$day/$month/$year"
        } catch (e: Exception) {
            value
        }
    }

    fun formatDateRange(start: String?, end: String?): String {
        val formattedStart = formatDateTime(start)

        if (end.isNullOrBlank()) {
            return formattedStart
        }

        val formattedEnd = formatDateTime(end)

        return if (formattedStart == formattedEnd) {
            formattedStart
        } else {
            "$formattedStart - $formattedEnd"
        }
    }
}
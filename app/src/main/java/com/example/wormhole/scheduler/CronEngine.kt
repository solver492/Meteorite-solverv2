package com.example.wormhole.scheduler

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CronEngine {

    /**
     * Validates a 5-field cron expression:
     * minute (0-59), hour (0-23), dayOfMonth (1-31), month (1-12), dayOfWeek (0-7)
     */
    fun isValid(cronExpression: String): Boolean {
        val parts = cronExpression.trim().split("\\s+".toRegex())
        if (parts.size != 5) return false

        return validateField(parts[0], 0, 59) &&
                validateField(parts[1], 0, 23) &&
                validateField(parts[2], 1, 31) &&
                validateField(parts[3], 1, 12) &&
                validateField(parts[4], 0, 7)
    }

    private fun validateField(field: String, min: Int, max: Int): Boolean {
        if (field == "*") return true

        // Check list e.g. "1,2,3"
        val elements = field.split(",")
        for (element in elements) {
            if (element.startsWith("*/")) {
                val step = element.substring(2).toIntOrNull() ?: return false
                if (step <= 0 || step > max) return false
                continue
            }
            if (element.contains("-")) {
                val range = element.split("-")
                if (range.size != 2) return false
                val start = range[0].toIntOrNull() ?: return false
                val end = range[1].toIntOrNull() ?: return false
                if (start < min || end > max || start > end) return false
                continue
            }
            val num = element.toIntOrNull() ?: return false
            if (num < min || num > max) return false
        }
        return true
    }

    private fun matchesField(value: Int, field: String, min: Int, max: Int): Boolean {
        if (field == "*") return true

        val elements = field.split(",")
        for (element in elements) {
            if (element.startsWith("*/")) {
                val step = element.substring(2).toIntOrNull() ?: return false
                if (step > 0 && (value - min) % step == 0) return true
                continue
            }
            if (element.contains("-")) {
                val range = element.split("-")
                if (range.size == 2) {
                    val start = range[0].toIntOrNull() ?: continue
                    val end = range[1].toIntOrNull() ?: continue
                    if (value in start..end) return true
                }
                continue
            }
            val num = element.toIntOrNull() ?: continue
            if (num == value) return true
        }
        return false
    }

    /**
     * Calculates the next execution time in milliseconds starting after [fromMillis].
     */
    fun calculateNextRun(cronExpression: String, fromMillis: Long = System.currentTimeMillis()): Long? {
        if (!isValid(cronExpression)) return null

        val parts = cronExpression.trim().split("\\s+".toRegex())
        val minuteField = parts[0]
        val hourField = parts[1]
        val domField = parts[2]
        val monthField = parts[3]
        val dowField = parts[4]

        val cal = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 1) // strictly after current minute
        }

        // Limit search to 366 days to avoid infinite loop
        val maxSteps = 366 * 24 * 60
        var steps = 0

        while (steps < maxSteps) {
            val month = cal.get(Calendar.MONTH) + 1 // 1-12
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH) // 1-31
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... in Calendar

            // Convert Calendar Day of Week (1=Sun, 2=Mon ... 7=Sat) to Cron standard:
            // 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
            val cronDow = when (dayOfWeek) {
                Calendar.SUNDAY -> 0
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> 0
            }

            if (!matchesField(month, monthField, 1, 12)) {
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                steps += 60
                continue
            }

            val dowMatches = matchesField(cronDow, dowField, 0, 7) || (cronDow == 0 && matchesField(7, dowField, 0, 7))
            val domMatches = matchesField(dayOfMonth, domField, 1, 31)

            // In cron: if both DOM and DOW are specified (not '*'), it's an OR, else AND
            val dayMatches = if (domField != "*" && dowField != "*") {
                domMatches || dowMatches
            } else {
                domMatches && dowMatches
            }

            if (!dayMatches) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                steps += 60
                continue
            }

            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (!matchesField(hour, hourField, 0, 23)) {
                cal.add(Calendar.HOUR_OF_DAY, 1)
                cal.set(Calendar.MINUTE, 0)
                steps += 60
                continue
            }

            val minute = cal.get(Calendar.MINUTE)
            if (matchesField(minute, minuteField, 0, 59)) {
                return cal.timeInMillis
            }

            cal.add(Calendar.MINUTE, 1)
            steps++
        }

        return null
    }

    /**
     * Generates a friendly, human-readable French description for the cron expression.
     */
    fun describe(cronExpression: String): String {
        if (!isValid(cronExpression)) return "Expression cron invalide"

        val parts = cronExpression.trim().split("\\s+".toRegex())
        val (min, hr, dom, mon, dow) = parts

        // Step patterns e.g. "*/15 * * * *"
        if (min.startsWith("*/") && hr == "*" && dom == "*" && mon == "*" && dow == "*") {
            val step = min.substring(2)
            return "Toutes les $step minutes"
        }

        // Hourly e.g. "0 * * * *"
        if (min == "0" && hr == "*" && dom == "*" && mon == "*" && dow == "*") {
            return "Toutes les heures au début de l'heure"
        }

        val timeStr = if (hr.toIntOrNull() != null && min.toIntOrNull() != null) {
            String.format(Locale.getDefault(), "%02d:%02d", hr.toInt(), min.toInt())
        } else {
            "à minute $min de l'heure $hr"
        }

        // Weekdays: 1-5
        if (dow == "1-5" && dom == "*" && mon == "*") {
            return "Tous les jours ouvrés (Lun-Ven) à $timeStr"
        }

        // Weekends: 0,6 or 6,0 or 6,7
        if ((dow == "0,6" || dow == "6,0" || dow == "6,7") && dom == "*" && mon == "*") {
            return "Tous les week-ends (Sam-Dim) à $timeStr"
        }

        // Every day: * *
        if (dow == "*" && dom == "*" && mon == "*") {
            return "Tous les jours à $timeStr"
        }

        // Specific days of week
        if (dow != "*" && dom == "*" && mon == "*") {
            val dayNames = dow.split(",").mapNotNull { d ->
                when (d.trim()) {
                    "1" -> "Lundi"
                    "2" -> "Mardi"
                    "3" -> "Mercredi"
                    "4" -> "Jeudi"
                    "5" -> "Vendredi"
                    "6" -> "Samedi"
                    "0", "7" -> "Dimanche"
                    else -> null
                }
            }
            if (dayNames.isNotEmpty()) {
                return "Chaque ${dayNames.joinToString(", ")} à $timeStr"
            }
        }

        // Specific day of month
        if (dom != "*" && dow == "*" && mon == "*") {
            val suffix = if (dom == "1") "1er" else dom
            return "Le $suffix de chaque mois à $timeStr"
        }

        return "Cron: $cronExpression ($timeStr)"
    }

    /**
     * Calculates the next execution timestamp for simple recurrence types or cron.
     */
    fun calculateNextExecution(
        recurrenceType: String,
        hour: Int,
        minute: Int,
        daysOfWeek: String = "",
        cronExpression: String = "",
        fromMillis: Long = System.currentTimeMillis()
    ): Long {
        return when (recurrenceType.uppercase()) {
            "CRON" -> {
                calculateNextRun(cronExpression, fromMillis) ?: (fromMillis + 24 * 3600 * 1000)
            }
            "ONCE" -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= fromMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                cal.timeInMillis
            }
            "DAILY" -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= fromMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                cal.timeInMillis
            }
            "WEEKLY" -> {
                val allowedDays = if (daysOfWeek.isNotBlank()) {
                    daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                } else {
                    listOf(1, 2, 3, 4, 5) // default Mon-Fri
                }

                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Check upcoming 14 days
                for (i in 0..14) {
                    val candidateCal = (cal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    if (candidateCal.timeInMillis > fromMillis) {
                        val dow = when (candidateCal.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.MONDAY -> 1
                            Calendar.TUESDAY -> 2
                            Calendar.WEDNESDAY -> 3
                            Calendar.THURSDAY -> 4
                            Calendar.FRIDAY -> 5
                            Calendar.SATURDAY -> 6
                            Calendar.SUNDAY -> 7
                            else -> 1
                        }
                        if (allowedDays.contains(dow)) {
                            return candidateCal.timeInMillis
                        }
                    }
                }
                fromMillis + 24 * 3600 * 1000
            }
            "MONTHLY" -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= fromMillis) {
                        add(Calendar.MONTH, 1)
                    }
                }
                cal.timeInMillis
            }
            else -> fromMillis + 24 * 3600 * 1000
        }
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.FRENCH)
        return sdf.format(Date(millis))
    }

    fun formatRelativeTime(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = millis - now
        if (diff < 0) return "En retard (${formatDateTime(millis)})"

        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Dans moins d'une minute"
            minutes < 60 -> "Dans $minutes min"
            hours < 24 -> {
                val remMin = minutes % 60
                if (remMin > 0) "Dans ${hours}h ${remMin}m" else "Dans ${hours}h"
            }
            days == 1L -> "Demain (${SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date(millis))})"
            days < 7 -> "Dans $days jours (${SimpleDateFormat("EEEE HH:mm", Locale.FRENCH).format(Date(millis))})"
            else -> formatDateTime(millis)
        }
    }
}

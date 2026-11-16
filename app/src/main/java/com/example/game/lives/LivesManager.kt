package com.example.game.lives

import android.content.SharedPreferences
import java.util.Locale

data class LifeSnapshot(
    val lives: Int,
    val nextRefillAtMs: Long,
    val remainingMs: Long
) {
    val isFull: Boolean get() = lives >= LivesManager.MAX_LIVES
}

object LivesManager {
    const val MAX_LIVES = 5
    const val REFILL_DURATION_MS = 20L * 60L * 1000L // 20 minutes per 1 life
    const val REFILL_COIN_COST = 900

    private const val PREF_LIVES = "lives_count"
    private const val PREF_NEXT_REFILL_AT_MS = "lives_next_refill_at_ms"

    fun reconcileLives(
        lives: Int,
        nextRefillAtMs: Long,
        nowMs: Long
    ): LifeSnapshot {
        val clampedLives = lives.coerceIn(0, MAX_LIVES)
        if (clampedLives >= MAX_LIVES) {
            return LifeSnapshot(
                lives = MAX_LIVES,
                nextRefillAtMs = 0L,
                remainingMs = 0L
            )
        }

        var currentLives = clampedLives
        var targetRefillAt = if (nextRefillAtMs > 0L) {
            nextRefillAtMs
        } else {
            nowMs + REFILL_DURATION_MS
        }

        while (currentLives < MAX_LIVES && nowMs >= targetRefillAt) {
            currentLives++
            targetRefillAt += REFILL_DURATION_MS
        }

        return if (currentLives >= MAX_LIVES) {
            LifeSnapshot(
                lives = MAX_LIVES,
                nextRefillAtMs = 0L,
                remainingMs = 0L
            )
        } else {
            LifeSnapshot(
                lives = currentLives,
                nextRefillAtMs = targetRefillAt,
                remainingMs = (targetRefillAt - nowMs).coerceIn(1L, REFILL_DURATION_MS)
            )
        }
    }

    fun consumeOneLife(
        lives: Int,
        nextRefillAtMs: Long,
        nowMs: Long
    ): LifeSnapshot {
        val reconciled = reconcileLives(lives, nextRefillAtMs, nowMs)
        if (reconciled.lives <= 0) {
            return reconciled
        }

        val newLives = (reconciled.lives - 1).coerceAtLeast(0)
        val newNextRefillAt = if (reconciled.lives >= MAX_LIVES) {
            // Dropping from full (5 -> 4) starts the 20-minute timer now
            nowMs + REFILL_DURATION_MS
        } else {
            // Preserve the active countdown already running
            reconciled.nextRefillAtMs
        }

        return reconcileLives(newLives, newNextRefillAt, nowMs)
    }

    fun addOneLife(
        lives: Int,
        nextRefillAtMs: Long,
        nowMs: Long
    ): LifeSnapshot {
        val reconciled = reconcileLives(lives, nextRefillAtMs, nowMs)
        val newLives = (reconciled.lives + 1).coerceAtMost(MAX_LIVES)
        return if (newLives >= MAX_LIVES) {
            LifeSnapshot(lives = MAX_LIVES, nextRefillAtMs = 0L, remainingMs = 0L)
        } else {
            reconcileLives(newLives, reconciled.nextRefillAtMs, nowMs)
        }
    }

    fun refillAllLives(): LifeSnapshot {
        return LifeSnapshot(
            lives = MAX_LIVES,
            nextRefillAtMs = 0L,
            remainingMs = 0L
        )
    }

    fun formatStatusText(snapshot: LifeSnapshot): String {
        return if (snapshot.isFull) {
            "Full"
        } else {
            formatCountdownTimer(snapshot.remainingMs)
        }
    }

    fun formatCountdownTimer(remainingMs: Long): String {
        val clampedMs = remainingMs.coerceAtLeast(0L)
        val totalSeconds = (clampedMs + 999L) / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun load(prefs: SharedPreferences, nowMs: Long = System.currentTimeMillis()): LifeSnapshot {
        val savedLives = prefs.getInt(PREF_LIVES, MAX_LIVES)
        val savedNextRefill = prefs.getLong(PREF_NEXT_REFILL_AT_MS, 0L)
        val snapshot = reconcileLives(savedLives, savedNextRefill, nowMs)
        if (snapshot.lives != savedLives || snapshot.nextRefillAtMs != savedNextRefill) {
            save(prefs, snapshot)
        }
        return snapshot
    }

    fun save(prefs: SharedPreferences, snapshot: LifeSnapshot) {
        prefs.edit()
            .putInt(PREF_LIVES, snapshot.lives)
            .putLong(PREF_NEXT_REFILL_AT_MS, snapshot.nextRefillAtMs)
            .apply()
    }
}

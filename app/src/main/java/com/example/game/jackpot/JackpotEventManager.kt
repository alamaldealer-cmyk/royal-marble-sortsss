package com.example.game.jackpot

import android.content.SharedPreferences
import java.util.Locale

data class JackpotEventSnapshot(
    val claimedSteps: Int,
    val cycleEndAtMs: Long,
    val remainingMs: Long
) {
    val remainingSteps: Int
        get() = (JackpotEventManager.TOTAL_STEPS - claimedSteps).coerceIn(0, JackpotEventManager.TOTAL_STEPS)

    val allClaimed: Boolean
        get() = claimedSteps >= JackpotEventManager.TOTAL_STEPS
}

object JackpotEventManager {
    const val TOTAL_STEPS = 5
    const val CYCLE_DURATION_MS = 24L * 60L * 60L * 1000L // Real 24 hours

    private const val PREF_CLAIMED_STEPS = "jackpot_claimed_steps"
    private const val PREF_CYCLE_END_AT_MS = "jackpot_cycle_end_at_ms"

    fun reconcile(
        claimedSteps: Int,
        cycleEndAtMs: Long,
        nowMs: Long
    ): JackpotEventSnapshot {
        if (cycleEndAtMs <= 0L) {
            val freshEnd = nowMs + CYCLE_DURATION_MS
            return JackpotEventSnapshot(
                claimedSteps = claimedSteps.coerceIn(0, TOTAL_STEPS),
                cycleEndAtMs = freshEnd,
                remainingMs = CYCLE_DURATION_MS
            )
        }

        if (nowMs >= cycleEndAtMs) {
            val elapsedPastEnd = nowMs - cycleEndAtMs
            val skippedCycles = elapsedPastEnd / CYCLE_DURATION_MS
            val nextEnd = cycleEndAtMs + (skippedCycles + 1L) * CYCLE_DURATION_MS
            val remaining = (nextEnd - nowMs).coerceIn(1L, CYCLE_DURATION_MS)
            return JackpotEventSnapshot(
                claimedSteps = 0,
                cycleEndAtMs = nextEnd,
                remainingMs = remaining
            )
        }

        val remaining = (cycleEndAtMs - nowMs).coerceIn(1L, CYCLE_DURATION_MS)
        return JackpotEventSnapshot(
            claimedSteps = claimedSteps.coerceIn(0, TOTAL_STEPS),
            cycleEndAtMs = cycleEndAtMs,
            remainingMs = remaining
        )
    }

    fun claimNextStep(
        claimedSteps: Int,
        cycleEndAtMs: Long,
        nowMs: Long
    ): JackpotEventSnapshot {
        val current = reconcile(claimedSteps, cycleEndAtMs, nowMs)
        if (current.allClaimed) return current
        return current.copy(
            claimedSteps = (current.claimedSteps + 1).coerceAtMost(TOTAL_STEPS)
        )
    }

    fun formatCountdownTimer(remainingMs: Long): String {
        val clampedMs = remainingMs.coerceAtLeast(0L)
        val totalSeconds = (clampedMs + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun load(prefs: SharedPreferences, nowMs: Long = System.currentTimeMillis()): JackpotEventSnapshot {
        val savedSteps = prefs.getInt(PREF_CLAIMED_STEPS, 0)
        val savedCycleEnd = prefs.getLong(PREF_CYCLE_END_AT_MS, 0L)
        val snapshot = reconcile(savedSteps, savedCycleEnd, nowMs)
        if (snapshot.claimedSteps != savedSteps || snapshot.cycleEndAtMs != savedCycleEnd) {
            save(prefs, snapshot)
        }
        return snapshot
    }

    fun save(prefs: SharedPreferences, snapshot: JackpotEventSnapshot) {
        prefs.edit()
            .putInt(PREF_CLAIMED_STEPS, snapshot.claimedSteps)
            .putLong(PREF_CYCLE_END_AT_MS, snapshot.cycleEndAtMs)
            .apply()
    }
}

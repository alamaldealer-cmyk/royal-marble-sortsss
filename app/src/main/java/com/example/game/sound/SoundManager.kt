package com.example.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlin.random.Random

object SoundManager {
    var enabled: Boolean = true
    var musicEnabled: Boolean = true
        private set

    private var initialized = false
    private var appContext: Context? = null
    private var soundPool: SoundPool? = null
    private var bgmPlayer: MediaPlayer? = null

    private var sfxTap = 0
    private var sfxPopBurst = 0
    private var sfxBallClink = 0
    private var sfxDrop0 = 0
    private var sfxDrop1 = 0
    private var sfxDrop2 = 0
    private var sfxBrickClear = 0
    private var sfxPowerup = 0
    private var sfxCoinCollect = 0
    private var sfxBlocked = 0
    private var sfxFluffyCheer = 0
    private var sfxWin = 0
    private var sfxUfoArrive = 0
    private var sfxUfoBeam = 0
    private var sfxStoryWhoosh = 0
    private var activeStoryStreamId = 0

    // Combo pitch escalation (Candy Crush style rising cascade)
    private var lastPopTimeMs = 0L
    private var popComboIndex = 0
    private val popComboRates = floatArrayOf(0.96f, 1.04f, 1.12f, 1.22f, 1.32f, 1.44f)

    // Subtle throttle for rapid belt feeds
    private var lastClinkTimeMs = 0L

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val ctx = context.applicationContext
        appContext = ctx

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()

        fun loadSfx(fileName: String): Int {
            return try {
                ctx.assets.openFd("sounds/$fileName").use { afd ->
                    pool.load(afd, 1)
                }
            } catch (_: Exception) {
                0
            }
        }

        sfxTap = loadSfx("sfx_tap.wav")
        sfxPopBurst = loadSfx("sfx_pop_burst.wav")
        sfxBallClink = loadSfx("sfx_ball_clink.wav")
        sfxDrop0 = loadSfx("sfx_drop_hole_0.wav")
        sfxDrop1 = loadSfx("sfx_drop_hole_1.wav")
        sfxDrop2 = loadSfx("sfx_drop_hole_2.wav")
        sfxBrickClear = loadSfx("sfx_brick_clear.wav")
        sfxPowerup = loadSfx("sfx_powerup.wav")
        sfxCoinCollect = loadSfx("sfx_coin_collect.wav")
        sfxBlocked = loadSfx("sfx_blocked.wav")
        sfxFluffyCheer = loadSfx("sfx_fluffy_cheer.wav")
        sfxWin = loadSfx("sfx_win.wav")
        sfxUfoArrive = loadSfx("sfx_ufo_arrive.wav")
        sfxUfoBeam = loadSfx("sfx_ufo_beam.wav")
        sfxStoryWhoosh = loadSfx("sfx_story_whoosh.wav")

        soundPool = pool
        initialized = true
    }

    private fun playSample(sampleId: Int, volume: Float = 0.85f, rate: Float = 1.0f, priority: Int = 1) {
        if (!enabled || sampleId == 0) return
        val pool = soundPool ?: return
        try {
            pool.play(sampleId, volume, volume, priority, 0, rate.coerceIn(0.5f, 2.0f))
        } catch (_: Exception) {
        }
    }

    fun setMusicEnabled(context: Context, isEnabled: Boolean) {
        init(context)
        musicEnabled = isEnabled
        if (isEnabled) {
            startOrResumeBgm()
        } else {
            pauseBgm()
        }
    }

    fun onAppPaused() {
        pauseBgm()
    }

    fun onAppResumed(context: Context) {
        init(context)
        if (musicEnabled) {
            startOrResumeBgm()
        }
    }

    @Synchronized
    private fun startOrResumeBgm() {
        val ctx = appContext ?: return
        try {
            if (bgmPlayer == null) {
                val player = MediaPlayer()
                ctx.assets.openFd("sounds/bgm_floofies.ogg").use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                player.isLooping = true
                player.setVolume(0.24f, 0.24f)
                player.prepare()
                bgmPlayer = player
            }
            if (bgmPlayer?.isPlaying == false) {
                bgmPlayer?.start()
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    private fun pauseBgm() {
        try {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
            }
        } catch (_: Exception) {
        }
    }

    fun playTap() {
        playSample(sfxTap, volume = 0.72f, rate = 1.0f)
    }

    fun playCoinCollect(pitchOffset: Float = 0f) {
        // Maps flying coin progression (0..180) smoothly up a sparkling crystal chime scale
        val rate = (0.94f + (pitchOffset / 380f)).coerceIn(0.90f, 1.48f)
        playSample(sfxCoinCollect, volume = 0.78f, rate = rate)
    }

    fun playBlocked() {
        playSample(sfxBlocked, volume = 0.75f, rate = 1.0f)
    }

    fun playPopBurst() {
        val now = System.currentTimeMillis()
        if (now - lastPopTimeMs <= 1400L) {
            popComboIndex = (popComboIndex + 1).coerceAtMost(popComboRates.lastIndex)
        } else {
            popComboIndex = 0
        }
        lastPopTimeMs = now
        val rate = popComboRates[popComboIndex]
        playSample(sfxPopBurst, volume = 0.90f, rate = rate, priority = 2)
    }

    fun playBallClink() {
        val now = System.currentTimeMillis()
        if (now - lastClinkTimeMs < 45L) return
        lastClinkTimeMs = now
        val rate = 0.95f + Random.nextFloat() * 0.14f
        playSample(sfxBallClink, volume = 0.48f, rate = rate)
    }

    fun playDropHole(holeIndex: Int) {
        val id = when (holeIndex.coerceIn(0, 2)) {
            0 -> sfxDrop0
            1 -> sfxDrop1
            else -> sfxDrop2
        }
        playSample(id, volume = 0.85f, rate = 1.0f, priority = 2)
    }

    fun playBrickClear() {
        playSample(sfxBrickClear, volume = 0.95f, rate = 1.0f, priority = 3)
    }

    fun playPowerup() {
        playSample(sfxPowerup, volume = 0.88f, rate = 1.0f, priority = 2)
    }

    fun playFluffyCheer() {
        val rate = 0.96f + Random.nextFloat() * 0.08f
        playSample(sfxFluffyCheer, volume = 0.85f, rate = rate, priority = 2)
    }

    fun playWin() {
        playSample(sfxWin, volume = 0.95f, rate = 1.0f, priority = 4)
    }

    fun stopStorySfx() {
        val streamId = activeStoryStreamId
        if (streamId != 0) {
            try {
                soundPool?.stop(streamId)
            } catch (_: Exception) {
            }
            activeStoryStreamId = 0
        }
    }

    private fun playStorySample(sampleId: Int, volume: Float = 0.92f) {
        stopStorySfx()
        if (!enabled || sampleId == 0) return
        val pool = soundPool ?: return
        try {
            activeStoryStreamId = pool.play(sampleId, volume, volume, 4, 0, 1.0f)
        } catch (_: Exception) {
        }
    }

    fun playUfoArrive() {
        playStorySample(sfxUfoArrive, volume = 0.95f)
    }

    fun playUfoBeam() {
        playStorySample(sfxUfoBeam, volume = 0.95f)
    }

    fun playStoryWhoosh() {
        playStorySample(sfxStoryWhoosh, volume = 0.90f)
    }
}

package com.example.game.story

import androidx.compose.ui.graphics.Color
import com.example.game.model.MarbleColor

object StoryCatalog {

    val chapters: List<StoryEpisode> = listOf(
        // CHAPTER 1 — Before Level 1 (Levels 1–10)
        StoryEpisode(
            chapterNumber = 1,
            triggerLevel = 1,
            levelRangeLabel = "LEVELS 1 – 10",
            title = "Peaceful Island",
            subtitle = "The Great Floofie Rescue Begins!",
            backdrop = StoryBackdrop.PEACEFUL_ISLAND,
            accentColor = Color(0xFF38BDF8),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • PEACEFUL ISLAND",
                    speaker = "Baby Floofies",
                    dialogue = "What a sunny morning! The baby Floofies are bouncing happily outside their island cottage.",
                    choreography = StoryStageChoreography.PEACEFUL_PLAY,
                    heroSprite = "story/hero_happy.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.YELLOW, MarbleColor.LIME, MarbleColor.PURPLE, MarbleColor.ORANGE),
                    sfxCue = StorySfxCue.CHEER
                ),
                StoryBeat(
                    sceneLabel = "2 • VILLAIN'S ARRIVAL",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"Hahaha! These cute colorful Floofies will power my Royal UFO Fleet!\"",
                    choreography = StoryStageChoreography.VILLAIN_ARRIVAL,
                    heroSprite = "story/hero_shocked.png",
                    villainSprite = "story/villain_grin.png",
                    ufoSprite = "story/ufo_villain.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.YELLOW, MarbleColor.LIME),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "3 • CAPTURING THE FLOOFIES",
                    speaker = "Hero Blue",
                    dialogue = "Oh no! Shadow Puff's tractor beam is trapping our friends inside glowing marble bubbles!",
                    choreography = StoryStageChoreography.ABDUCTION_BEAM,
                    heroSprite = "story/hero_scared.png",
                    villainSprite = "story/villain_point.png",
                    ufoSprite = "story/ufo_beam.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.YELLOW, MarbleColor.LIME, MarbleColor.ORANGE),
                    sfxCue = StorySfxCue.UFO_BEAM
                ),
                StoryBeat(
                    sceneLabel = "4 • HERO'S PROMISE",
                    speaker = "Hero Blue",
                    dialogue = "\"Hold on, everyone! I'll burst every locked crate and rescue our friends!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.YELLOW, MarbleColor.LIME),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 2 — After Level 10 (Levels 11–20)
        StoryEpisode(
            chapterNumber = 2,
            triggerLevel = 11,
            levelRangeLabel = "LEVELS 11 – 20",
            title = "Glowing Crystal Caves",
            subtitle = "Deep Underground Trail",
            backdrop = StoryBackdrop.CRYSTAL_CAVES,
            accentColor = Color(0xFF2DD4BF),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • INTO THE CAVERNS",
                    speaker = "Hero Blue",
                    dialogue = "We cleared the beach! Shadow Puff's UFO dropped glowing cages deep inside these crystal caves.",
                    choreography = StoryStageChoreography.PEACEFUL_PLAY,
                    heroSprite = "story/hero_wink.png",
                    babyColors = listOf(MarbleColor.CYAN, MarbleColor.ICE),
                    sfxCue = StorySfxCue.POP
                ),
                StoryBeat(
                    sceneLabel = "2 • CAVE AMBUSH",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"Try getting through my hidden Cave Dispensers, little hero!\"",
                    choreography = StoryStageChoreography.CAVE_TRAP,
                    heroSprite = "story/hero_worried.png",
                    villainSprite = "story/villain_angry.png",
                    ufoSprite = "story/ufo_tilted.png",
                    babyColors = listOf(MarbleColor.PURPLE, MarbleColor.CYAN, MarbleColor.ICE),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "3 • READY TO DASH",
                    speaker = "Hero Blue",
                    dialogue = "\"Sort the marbles into the matching buckets and we'll unlock every cave cage!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.CYAN, MarbleColor.ICE, MarbleColor.PURPLE),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 3 — After Level 20 (Levels 21–30)
        StoryEpisode(
            chapterNumber = 3,
            triggerLevel = 21,
            levelRangeLabel = "LEVELS 21 – 30",
            title = "Twilight Floating Falls",
            subtitle = "Mystery Crates Unleashed",
            backdrop = StoryBackdrop.TWILIGHT_VORTEX,
            accentColor = Color(0xFFC084FC),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • FLOATING ISLANDS",
                    speaker = "Hero Blue",
                    dialogue = "The UFO escaped above the twilight waterfalls! Look at those purple sky portals opening!",
                    choreography = StoryStageChoreography.VILLAIN_ARRIVAL,
                    heroSprite = "story/hero_shocked.png",
                    ufoSprite = "story/ufo_tilted.png",
                    babyColors = listOf(MarbleColor.YELLOW, MarbleColor.LIME, MarbleColor.RED),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "2 • MYSTERY LOCKDOWN",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"I've disguised the crates with mystery locks and added Red Floofies to the mix!\"",
                    choreography = StoryStageChoreography.VORTEX_SHOWDOWN,
                    heroSprite = "story/hero_scared.png",
                    villainSprite = "story/villain_grin.png",
                    babyColors = listOf(MarbleColor.RED, MarbleColor.YELLOW, MarbleColor.LIME),
                    sfxCue = StorySfxCue.POP
                ),
                StoryBeat(
                    sceneLabel = "3 • FRIENDS TOGETHER",
                    speaker = "Hero Blue",
                    dialogue = "\"Mystery boxes can't stop us! Let's free the Red and Lime Floofies!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.RED, MarbleColor.LIME, MarbleColor.YELLOW),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 4 — After Level 30 (Levels 31–40)
        StoryEpisode(
            chapterNumber = 4,
            triggerLevel = 31,
            levelRangeLabel = "LEVELS 31 – 40",
            title = "Tropical Bay Resort",
            subtitle = "Return to the Harbor",
            backdrop = StoryBackdrop.TROPICAL_RESORT,
            accentColor = Color(0xFFFB923C),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • RESORT UNDER SIEGE",
                    speaker = "Pink Floofie",
                    dialogue = "\"Look! Shadow Puff circled back to the Tropical Harbor to grab our golden coins!\"",
                    choreography = StoryStageChoreography.PEACEFUL_PLAY,
                    heroSprite = "story/hero_laugh.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.ORANGE, MarbleColor.YELLOW),
                    sfxCue = StorySfxCue.CHEER
                ),
                StoryBeat(
                    sceneLabel = "2 • BEAM OVER THE BAY",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"Faster conveyor belts! Let's see you keep up with 24 marbles at once!\"",
                    choreography = StoryStageChoreography.ABDUCTION_BEAM,
                    heroSprite = "story/hero_worried.png",
                    villainSprite = "story/villain_point.png",
                    ufoSprite = "story/ufo_beam.png",
                    babyColors = listOf(MarbleColor.ORANGE, MarbleColor.PINK, MarbleColor.PURPLE),
                    sfxCue = StorySfxCue.UFO_BEAM
                ),
                StoryBeat(
                    sceneLabel = "3 • HARBOR RESCUE",
                    speaker = "Hero Blue",
                    dialogue = "\"Use Magnets and Shuffles wisely — onward to Level 31!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.ORANGE, MarbleColor.CYAN),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 5 — After Level 40 (Levels 41–50)
        StoryEpisode(
            chapterNumber = 5,
            triggerLevel = 41,
            levelRangeLabel = "LEVELS 41 – 50",
            title = "The Vortex Storm",
            subtitle = "Halfway Through the Quest!",
            backdrop = StoryBackdrop.TWILIGHT_VORTEX,
            accentColor = Color(0xFFE879F9),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • HALFWAY MILESTONE",
                    speaker = "Hero Blue",
                    dialogue = "40 levels cleared! Half of our island friends are already safe — keep pushing upward!",
                    choreography = StoryStageChoreography.PEACEFUL_PLAY,
                    heroSprite = "story/hero_wink.png",
                    babyColors = listOf(MarbleColor.CYAN, MarbleColor.PURPLE, MarbleColor.LIME, MarbleColor.PINK),
                    sfxCue = StorySfxCue.CHEER
                ),
                StoryBeat(
                    sceneLabel = "2 • PORTAL SURGE",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"Impossible! How did you solve so many boards?! Eat my Twin Vortex Trap!\"",
                    choreography = StoryStageChoreography.VORTEX_SHOWDOWN,
                    heroSprite = "story/hero_shocked.png",
                    villainSprite = "story/villain_angry.png",
                    ufoSprite = "story/ufo_villain.png",
                    babyColors = listOf(MarbleColor.PURPLE, MarbleColor.CYAN, MarbleColor.ORANGE),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "3 • UNSTOPPABLE TEAM",
                    speaker = "Hero Blue",
                    dialogue = "\"We're coming right through that portal, Shadow Puff!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.CYAN, MarbleColor.PURPLE, MarbleColor.YELLOW),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 6 — After Level 50 (Levels 51–60)
        StoryEpisode(
            chapterNumber = 6,
            triggerLevel = 51,
            levelRangeLabel = "LEVELS 51 – 60",
            title = "Frosty Aurora Ridge",
            subtitle = "Chasing Into the Snow",
            backdrop = StoryBackdrop.FROST_PEAKS,
            accentColor = Color(0xFF67E8F9),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • FROZEN TRAIL",
                    speaker = "Hero Blue",
                    dialogue = "Brrr! The vortex spat us out on the Aurora Peaks. Look at those glittering ice crystals!",
                    choreography = StoryStageChoreography.PEACEFUL_PLAY,
                    heroSprite = "story/hero_worried.png",
                    babyColors = listOf(MarbleColor.ICE, MarbleColor.CYAN, MarbleColor.PINK),
                    sfxCue = StorySfxCue.POP
                ),
                StoryBeat(
                    sceneLabel = "2 • ICE CAGE LOCKUP",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"Freeze in the snow! Your Floofie friends are locked tight in my frosty cages!\"",
                    choreography = StoryStageChoreography.CAVE_TRAP,
                    heroSprite = "story/hero_scared.png",
                    villainSprite = "story/villain_grin.png",
                    ufoSprite = "story/ufo_tilted.png",
                    babyColors = listOf(MarbleColor.ICE, MarbleColor.CYAN, MarbleColor.RED),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "3 • WARM HEART, FAST PAWS",
                    speaker = "Hero Blue",
                    dialogue = "\"No frost can chill our courage! Let's melt these puzzles!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.ICE, MarbleColor.CYAN, MarbleColor.YELLOW),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 7 — After Level 60 (Levels 61–70)
        StoryEpisode(
            chapterNumber = 7,
            triggerLevel = 61,
            levelRangeLabel = "LEVELS 61 – 70",
            title = "Glacier Spire Citadel",
            subtitle = "Breaking the Frost Vault",
            backdrop = StoryBackdrop.FROST_PEAKS,
            accentColor = Color(0xFF38BDF8),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • NORTHERN LIGHTS",
                    speaker = "Ice Floofie",
                    dialogue = "\"Look up! Shadow Puff's UFO is recharging under the aurora sky!\"",
                    choreography = StoryStageChoreography.VILLAIN_ARRIVAL,
                    heroSprite = "story/hero_shocked.png",
                    ufoSprite = "story/ufo_beam.png",
                    babyColors = listOf(MarbleColor.ICE, MarbleColor.LIME, MarbleColor.ORANGE),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "2 • ROYAL RESCUE PLAN",
                    speaker = "Hero Blue",
                    dialogue = "If we clear the exit rows cleanly and watch the funnel limit, his shields will shatter!",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.ICE, MarbleColor.CYAN, MarbleColor.PURPLE),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 8 — After Level 70 (Levels 71–80)
        StoryEpisode(
            chapterNumber = 8,
            triggerLevel = 71,
            levelRangeLabel = "LEVELS 71 – 80",
            title = "Candy Cloud Sky Arena",
            subtitle = "Ascending to the Clouds",
            backdrop = StoryBackdrop.CANDY_CLOUD_ARENA,
            accentColor = Color(0xFFFACC15),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • ABOVE THE CLOUDS",
                    speaker = "Hero Blue",
                    dialogue = "We made it to the Sky Arena! Every rescued Floofie is cheering us on!",
                    choreography = StoryStageChoreography.PEACEFUL_PLAY,
                    heroSprite = "story/hero_laugh.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.YELLOW, MarbleColor.LIME, MarbleColor.CYAN),
                    sfxCue = StorySfxCue.CHEER
                ),
                StoryBeat(
                    sceneLabel = "2 • MOTHERSHIP APPROACHES",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"Enough games! Meet my Mothership fleet above the clouds!\"",
                    choreography = StoryStageChoreography.ABDUCTION_BEAM,
                    heroSprite = "story/hero_shocked.png",
                    villainSprite = "story/villain_point.png",
                    ufoSprite = "story/ufo_villain.png",
                    babyColors = listOf(MarbleColor.RED, MarbleColor.ORANGE, MarbleColor.PURPLE),
                    sfxCue = StorySfxCue.UFO_BEAM
                ),
                StoryBeat(
                    sceneLabel = "3 • SKYWARD LEAP",
                    speaker = "Hero Blue",
                    dialogue = "\"Next stop: Shadow Puff's Mothership! Hang tight, friends!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.YELLOW, MarbleColor.PINK, MarbleColor.CYAN),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 9 — After Level 80 (Levels 81–90)
        StoryEpisode(
            chapterNumber = 9,
            triggerLevel = 81,
            levelRangeLabel = "LEVELS 81 – 90",
            title = "Inside the Mothership",
            subtitle = "Infiltrating the Neon Deck",
            backdrop = StoryBackdrop.UFO_MOTHERSHIP,
            accentColor = Color(0xFFA855F7),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • ABOARD THE UFO",
                    speaker = "Hero Blue",
                    dialogue = "We're inside the Shadow Mothership! The remaining Floofies are right behind these energy doors!",
                    choreography = StoryStageChoreography.CAVE_TRAP,
                    heroSprite = "story/hero_worried.png",
                    babyColors = listOf(MarbleColor.PURPLE, MarbleColor.PINK, MarbleColor.RED, MarbleColor.ORANGE),
                    sfxCue = StorySfxCue.POP
                ),
                StoryBeat(
                    sceneLabel = "2 • SHADOW PUFF PANICS",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"N-no! How did a tiny blue Floofie sneak onto my flagship?! Lock all 8 colors!\"",
                    choreography = StoryStageChoreography.VORTEX_SHOWDOWN,
                    heroSprite = "story/hero_wink.png",
                    villainSprite = "story/villain_angry.png",
                    ufoSprite = "story/ufo_tilted.png",
                    babyColors = listOf(MarbleColor.RED, MarbleColor.PURPLE, MarbleColor.CYAN),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "3 • BREAK THE LOCKS",
                    speaker = "Hero Blue",
                    dialogue = "\"Almost at the core! Let's clear Levels 81 to 90!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.LIME, MarbleColor.YELLOW),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        ),

        // CHAPTER 10 — After Level 90 (Levels 91–100)
        StoryEpisode(
            chapterNumber = 10,
            triggerLevel = 91,
            levelRangeLabel = "LEVELS 91 – 100",
            title = "The Grand Final Showdown",
            subtitle = "Bring Every Floofie Home!",
            backdrop = StoryBackdrop.UFO_MOTHERSHIP,
            accentColor = Color(0xFFFFD700),
            beats = listOf(
                StoryBeat(
                    sceneLabel = "1 • THE CORE CHAMBER",
                    speaker = "Lord Shadow Puff",
                    dialogue = "\"You've come far, Hero Blue — but my final 10 gauntlets will keep the last Floofies forever!\"",
                    choreography = StoryStageChoreography.VILLAIN_ARRIVAL,
                    heroSprite = "story/hero_shocked.png",
                    villainSprite = "story/villain_grin.png",
                    ufoSprite = "story/ufo_villain.png",
                    babyColors = listOf(MarbleColor.RED, MarbleColor.ORANGE, MarbleColor.PURPLE, MarbleColor.YELLOW),
                    sfxCue = StorySfxCue.UFO_ARRIVE
                ),
                StoryBeat(
                    sceneLabel = "2 • LAST TRAPPED FRIENDS",
                    speaker = "Baby Floofies",
                    dialogue = "\"Hero Blue! We know you can sort us free — just 10 more levels!\"",
                    choreography = StoryStageChoreography.ABDUCTION_BEAM,
                    heroSprite = "story/hero_worried.png",
                    ufoSprite = "story/ufo_beam.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.LIME, MarbleColor.CYAN, MarbleColor.ICE),
                    sfxCue = StorySfxCue.UFO_BEAM
                ),
                StoryBeat(
                    sceneLabel = "3 • FINAL CHARGE",
                    speaker = "Hero Blue",
                    dialogue = "\"For Peaceful Island and all our friends — let's finish this once and for all!\"",
                    choreography = StoryStageChoreography.HERO_CHARGE,
                    heroSprite = "story/hero_dash.png",
                    babyColors = listOf(MarbleColor.PINK, MarbleColor.YELLOW, MarbleColor.LIME, MarbleColor.PURPLE),
                    sfxCue = StorySfxCue.HERO_DASH
                )
            )
        )
    )

    val grandFinale: StoryEpisode = StoryEpisode(
        chapterNumber = 11,
        triggerLevel = 101,
        isFinaleEpilogue = true,
        levelRangeLabel = "ALL 100 LEVELS COMPLETE!",
        title = "Island Fiesta Finale",
        subtitle = "Every Floofie Rescued!",
        backdrop = StoryBackdrop.PEACEFUL_ISLAND,
        accentColor = Color(0xFF4ADE80),
        beats = listOf(
            StoryBeat(
                sceneLabel = "1 • MOTHERSHIP DEFEATED",
                speaker = "Lord Shadow Puff",
                dialogue = "\"Nooo! My UFO engines overloaded! I'm blasting off into space!\"",
                choreography = StoryStageChoreography.VORTEX_SHOWDOWN,
                heroSprite = "story/hero_laugh.png",
                villainSprite = "story/villain_angry.png",
                ufoSprite = "story/ufo_tilted.png",
                babyColors = listOf(MarbleColor.PINK, MarbleColor.YELLOW, MarbleColor.LIME),
                sfxCue = StorySfxCue.UFO_ARRIVE
            ),
            StoryBeat(
                sceneLabel = "2 • HOME SWEET ISLAND",
                speaker = "All Floofies",
                dialogue = "\"Hooray! Every single Floofie is back home safe on Peaceful Island! Thank you, Hero!\"",
                choreography = StoryStageChoreography.CELEBRATION_FIESTA,
                heroSprite = "story/hero_happy.png",
                babyColors = listOf(
                    MarbleColor.PINK,
                    MarbleColor.YELLOW,
                    MarbleColor.LIME,
                    MarbleColor.PURPLE,
                    MarbleColor.ORANGE,
                    MarbleColor.CYAN,
                    MarbleColor.ICE,
                    MarbleColor.RED
                ),
                sfxCue = StorySfxCue.WIN
            )
        )
    )

    fun storyForStartingLevel(level: Int): StoryEpisode? {
        return chapters.firstOrNull { it.triggerLevel == level }
    }
}

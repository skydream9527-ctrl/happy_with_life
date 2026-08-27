package com.xiaoquexing.app.data.entity

enum class PlantStage(val displayName: String, val minGp: Int, val maxGp: Int) {
    SEED("种子", 0, 50),
    SPROUT("发芽", 50, 200),
    SEEDLING("幼苗", 200, 500),
    GROWING("成长", 500, 1500),
    FLOURISH("茂盛", 1500, 4000),
    MATURE("成熟", 4000, 10000),
    DIVINE("神木", 10000, Int.MAX_VALUE);

    companion object {
        fun fromGp(gp: Int): PlantStage {
            return entries.firstOrNull { gp in it.minGp until it.maxGp } ?: DIVINE
        }

        fun progressInStage(gp: Int): Float {
            val stage = fromGp(gp)
            if (stage == DIVINE) return 1f
            val range = stage.maxGp - stage.minGp
            return ((gp - stage.minGp).toFloat() / range).coerceIn(0f, 1f)
        }

        fun nextStageGp(gp: Int): Int {
            val stage = fromGp(gp)
            return if (stage == DIVINE) gp else stage.maxGp
        }
    }
}

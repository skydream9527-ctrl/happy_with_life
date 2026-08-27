package com.xiaoquexing.app.data.entity

enum class PlantType(val displayName: String, val emoji: String, val unlockGp: Int, val description: String) {
    TREE("小确幸之树", "🌳", 0, "最经典的幸福树，陪伴你记录每一个小确幸"),
    SAKURA("樱花树", "🌸", 500, "粉嫩浪漫的樱花树，飘落的花瓣带来温柔心情"),
    SUNFLOWER("向日葵", "🌻", 1000, "永远朝向阳光，象征积极与希望"),
    CACTUS("仙人掌", "🌵", 1500, "坚强独立的仙人掌，在干旱中也能绽放花朵"),
    SUCCULENT("多肉", "🪴", 2000, "Q弹可爱的多肉植物，治愈系萌物"),
    VINE("藤蔓", "🌿", 2500, "柔韧绵长的藤蔓，代表情感的延续"),
    ROSE("玫瑰花丛", "🌹", 3000, "热烈浪漫的玫瑰，带刺却美丽"),
    BAMBOO("竹林", "🎋", 4000, "清雅挺拔的竹子，节节高升"),
    MUSHROOM("蘑菇", "🍄", 5000, "童话般的蘑菇圈，充满奇幻色彩")
}

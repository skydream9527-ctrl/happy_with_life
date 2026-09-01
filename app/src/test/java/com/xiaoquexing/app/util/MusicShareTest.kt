package com.xiaoquexing.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MusicShareTest {
    @Test
    fun parseNeteaseLink() {
        val song = MusicShare.parse("https://music.163.com/song?id=123&s=晴天", MusicPlatform.NETEASE)
        assertNotNull(song)
        assertEquals(MusicPlatform.NETEASE, song!!.platform)
        assertEquals("晴天", song.title)
    }

    @Test
    fun parsePlainTitle() {
        val song = MusicShare.parse("海阔天空", MusicPlatform.QQ)
        assertNotNull(song)
        assertEquals("海阔天空", song!!.title)
        assertEquals("QQ音乐", song.artist)
    }
}

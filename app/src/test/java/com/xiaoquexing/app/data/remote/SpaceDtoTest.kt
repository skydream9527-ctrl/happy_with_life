package com.xiaoquexing.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceDtoTest {
    @Test
    fun inviteDto_keepsTokenAndLink() {
        val invite = InviteDto(
            inviteId = "inv_1",
            token = "tok_abc",
            link = "https://xqx.app/i/tok_abc",
            maxUses = 6,
        )
        assertEquals("tok_abc", invite.token)
        assertEquals(6, invite.maxUses)
    }

    @Test
    fun spaceDto_sharedType() {
        val space = SpaceDto(
            id = "spc_shared",
            name = "我们的小日子",
            spaceType = "COUPLE",
            totalGp = 30,
            plantStage = "SPROUT",
        )
        assertEquals("COUPLE", space.spaceType)
        assertEquals("我们的小日子", space.name)
    }
}

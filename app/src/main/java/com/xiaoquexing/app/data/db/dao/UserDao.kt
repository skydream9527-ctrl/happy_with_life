package com.xiaoquexing.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xiaoquexing.app.data.db.entity.SpaceMemberEntity
import com.xiaoquexing.app.data.db.entity.UserEntity

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("SELECT * FROM users WHERE deleted_at IS NULL ORDER BY local_id LIMIT 1")
    suspend fun getFirstUser(): UserEntity?
}

@Dao
interface SpaceMemberDao {

    @Insert
    suspend fun insert(member: SpaceMemberEntity): Long

    @Query("SELECT COUNT(*) FROM space_members WHERE space_id = :spaceId")
    suspend fun countMembers(spaceId: Long): Int
}

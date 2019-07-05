package com.transcodium.wirepurse.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.transcodium.wirepurse.db.entities.Notifications

@Dao
abstract class NotificationDao {

    //get new notification count
    @Query("SELECT COUNT(id) FROM notifications WHERE seen = 0 AND user_id = :userId")
    abstract fun getTotalUnseenCount(userId: String): Int

    //get new notification count, live data
    @Query("SELECT COUNT(id) FROM notifications WHERE seen = 0 AND user_id = :userId")
    abstract fun getTotalUnseenCountLive(userId: String):LiveData<Int>

    //select one
    @Query("Select * From notifications WHERE id = :id AND user_id = :userId ORDER BY id DESC LIMIT 1")
    abstract fun findOne(id: Long,userId: String): Notifications

    //findOneLive
    @Query("Select * From notifications WHERE id = :id AND user_id = :userId ORDER BY id DESC LIMIT 1")
    abstract fun findOneLive(id: Long,userId: String): LiveData<Notifications>

    //fetch all
    @Query("Select * From notifications WHERE user_id = :userId ORDER BY created_at DESC")
    abstract fun fetchAllLive(userId: String): LiveData<List<Notifications>>

    //update Seen
    @Query("UPDATE notifications SET seen=1 WHERE user_id = :userId AND remote_id IN (:ids)")
    abstract fun setSeenByRemoteIds(userId: String, ids: List<String>)


    @Query("SELECT 1 FROM notifications  WHERE remote_id = :remoteId AND user_id = :userId")
    abstract fun exists(userId: String,remoteId: String) : Int

    //insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(data: Notifications)

    //deleteOne
    @Query("DELETE FROM notifications WHERE id = :id")
    abstract fun deleteOne(id: Long)

    //delete all
    @Query("DELETE FROM notifications")
    abstract fun deleteAll()
}
package com.transcodium.wirepurse.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
        tableName = "notifications",
        indices = [
            Index("user_id"),
            Index(value = arrayOf("remote_id"), unique = true)
        ]

)

data class Notifications(
     @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) var id: Long? = null,
     @ColumnInfo(name = "user_id") var userId: String,
     @ColumnInfo(name = "remote_id") var remoteId: String,
     @ColumnInfo(name = "seen") var seen: Int,
     @ColumnInfo(name = "created_at") var createdAt: Long,
     @ColumnInfo(name = "data") var data: String
)
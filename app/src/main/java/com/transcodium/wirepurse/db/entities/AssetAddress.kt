package com.transcodium.wirepurse.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
   tableName = "asset_addresses",
   indices = [Index("chain")]

)

data class AssetAddress(
        @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = false) var id: Long? = null,
        @ColumnInfo(name = "chain") var chain: String,
        @ColumnInfo(name = "address") var address: String
)

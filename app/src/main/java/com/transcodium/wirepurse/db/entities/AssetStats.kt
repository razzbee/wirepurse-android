/**
# Copyright 2018 - Transcodium Ltd.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the  Apache License v2.0 which accompanies this distribution.
#
#  The Apache License v2.0 is available at
#  http://www.opensource.org/licenses/apache2.0.php
#
#  You are required to redistribute this code under the same licenses.
#
#  Project TNSMoney
#  @author Razak Zakari <razak@transcodium.com>
#  https://transcodium.com
#  created_at 20/09/2018
 **/

package com.transcodium.wirepurse.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "asset_stats",
        indices = [Index("type")]
)
data class AssetStats(
        @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) var id: Long? = null,
        @ColumnInfo(name = "type") var type: String? = "",
        @ColumnInfo(name = "data") var data: String? = ""
)
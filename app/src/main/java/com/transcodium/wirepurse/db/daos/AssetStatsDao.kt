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

package com.transcodium.wirepurse.db.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.transcodium.wirepurse.db.entities.AssetStats

@Dao
abstract class AssetStatsDao {

    //livedata
    @get:Query("Select * FROM asset_stats")
    abstract val allLive: LiveData<List<AssetStats>>

    //non live data
    @get:Query("Select * FROM asset_stats")
    abstract val all: LiveData<List<AssetStats>>

    /**
     * fetch  by type
     * for LiveData
     */
    @Query("Select * From asset_stats WHERE type = :type")
    abstract fun findByTypeLive(type: String): LiveData<AssetStats>

    //none live data direct access
    @Query("Select * From asset_stats WHERE type = :type")
    abstract fun findByType(type: String): AssetStats



    @Insert(onConflict = REPLACE)
    abstract fun updateData(data: AssetStats)
}
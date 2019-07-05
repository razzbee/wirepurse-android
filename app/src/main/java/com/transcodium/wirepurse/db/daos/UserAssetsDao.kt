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
#  created_at 22/09/2018
 **/

package com.transcodium.wirepurse.db.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.transcodium.wirepurse.db.entities.UserAssets

@Dao
abstract class UserAssetsDao {

    @get:Query("Select * FROM user_assets")
    abstract val all: List<UserAssets>

    @get:Query("Select * FROM user_assets")
    abstract val allLive: LiveData<List<UserAssets>>

    /**
     * fetch  by type
     */
    @Query("Select * From user_assets Limit 1")
    abstract fun getData(): LiveData<List<UserAssets>>

    @Query("Select * From user_assets Limit 1")
    abstract fun getDataLive(): List<UserAssets>

    /**
     * update asset data
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateData(data: UserAssets)

    //delete all
    @Query("DELETE FROM user_assets")
    abstract fun deleteAll()
}
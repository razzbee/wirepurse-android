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

package com.transcodium.wirepurse.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.transcodium.wirepurse.db.AppDB
import com.transcodium.wirepurse.db.entities.AssetStats
import com.transcodium.wirepurse.db.entities.UserAssets
import com.transcodium.wirepurse.launchIO
import java.lang.Exception

class HomeViewModel(application: Application) : AndroidViewModel(application){


    private val appDB: AppDB = AppDB.getInstance(application)


    private val userAssets: LiveData<List<UserAssets>> = appDB.userAssetsDao().allLive

    private val assetStats: LiveData<List<AssetStats>> = appDB.assetStatsDao().allLive

    private val cryptoAssetStats = appDB.assetStatsDao().findByTypeLive("crypto")


    /**
     * getAllUserAssets
     */
    fun getUserAssets() = userAssets

    fun getAllAssetStats() = assetStats

    fun getCryptoAssetStats() = cryptoAssetStats

}//end class
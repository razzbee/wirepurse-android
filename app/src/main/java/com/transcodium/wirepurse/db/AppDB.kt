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

package com.transcodium.wirepurse.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.transcodium.wirepurse.APP_DB_NAME
import com.transcodium.wirepurse.DB_VERSION_NO
import com.transcodium.wirepurse.db.daos.AssetAddressDao
import com.transcodium.wirepurse.db.daos.AssetStatsDao
import com.transcodium.wirepurse.db.daos.NotificationDao
import com.transcodium.wirepurse.db.daos.UserAssetsDao
import com.transcodium.wirepurse.db.entities.AssetAddress
import com.transcodium.wirepurse.db.entities.AssetStats
import com.transcodium.wirepurse.db.entities.Notifications
import com.transcodium.wirepurse.db.entities.UserAssets

@Database(
        entities = [
            AssetStats::class,
            UserAssets::class,
            AssetAddress::class,
            Notifications::class
        ],
        exportSchema = false,
        version = 	DB_VERSION_NO
)

abstract class AppDB : RoomDatabase() {

    abstract fun assetStatsDao(): AssetStatsDao
    abstract fun userAssetsDao(): UserAssetsDao
    abstract fun assetAddressDao(): AssetAddressDao
    abstract fun notificationDao(): NotificationDao

    companion object {

        private var instance: AppDB? = null


        /**
         * singleton class
         * @return AppDB
         */
        @Synchronized
        fun getInstance(context: Context): AppDB {

            if(instance == null){
                instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDB::class.java,
                                APP_DB_NAME
                )
                 .fallbackToDestructiveMigration()
                 .build()
            }


            return instance!!
        }//end fun

    }

}
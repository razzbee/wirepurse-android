package com.transcodium.wirepurse.classes


data class ListItemModel(
        val tagName: String,
        val icon: Int,
        val title: String,
        val targetActivity: Class<*>)
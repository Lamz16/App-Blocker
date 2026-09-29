package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_network_rules")
data class AppNetworkRuleEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val blockWifi: Boolean = false,
    val blockMobileData: Boolean = false,
    val blockBackground: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

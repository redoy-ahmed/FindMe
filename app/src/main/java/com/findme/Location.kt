package com.findme

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Location(

    val name: String = "",
    val time: String = "",
    val lat: String = "",
    val lng: String = "",
    val location: String = ""
)
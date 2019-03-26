package com.findme

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(

    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val lat: String = "",
    val lng: String = "",
    val location: String = ""
)


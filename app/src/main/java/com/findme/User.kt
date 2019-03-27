package com.findme

import com.google.firebase.database.IgnoreExtraProperties
import java.util.ArrayList

@IgnoreExtraProperties
data class User(

    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val lat: String = "",
    val lng: String = "",
    val location: String = "",
    val locationLog: ArrayList<Location> = ArrayList()
)


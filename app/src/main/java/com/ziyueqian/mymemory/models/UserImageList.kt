package com.ziyueqian.mymemory.models

import com.google.firebase.firestore.PropertyName

data class UserImageList (
        //how firebase will know that the key is called images, and maps to this nullable list
    @PropertyName("images") val images: List<String>? = null,
    @PropertyName("avgMoves") val avgMoves: Double? = null,
    @PropertyName("timesPlayed") val timesPlayed: Double? = null
)

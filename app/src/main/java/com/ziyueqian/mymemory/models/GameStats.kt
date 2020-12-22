package com.ziyueqian.mymemory.models

import com.google.firebase.firestore.PropertyName

data class GameStats (
    val identifier: Int,
    val gameName : String? = null,
    val gameLevel : String? = "Easy",
    val avgMoves : Double? = null
)

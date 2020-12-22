package com.ziyueqian.mymemory.models

data class MemoryCard (
    val identifier : Int,   //val cannot be changed
    val imageUrl : String? = null,
    var isFaceUp : Boolean = false, //var can be changed
    var isMatched : Boolean = false
)
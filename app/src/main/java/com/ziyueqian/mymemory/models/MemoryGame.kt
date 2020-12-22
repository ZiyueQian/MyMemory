package com.ziyueqian.mymemory.models

import com.ziyueqian.mymemory.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize, customImages: List<String>?) {


    val cards: List<MemoryCard>
    var numPairsFound = 0
    private var numFlips = 0
    private var indexOfSingleSelectedCard : Int? = null
    init {
        if (customImages == null) {
            val chosenImages : List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomizedImages : List<Int> = (chosenImages + chosenImages).shuffled() //make pairs
            cards = randomizedImages.map{ MemoryCard(it)}
        } else {
            val randomizedImages = (customImages + customImages).shuffled()
            cards = randomizedImages.map{ MemoryCard(it.hashCode(),it)} //use hashCode to translate image string to identifier int
        }

    }

    fun flipCard(position: Int): Boolean {
        numFlips++
        val card = cards[position]
        var foundMatch = false
        if (indexOfSingleSelectedCard == null) {
            //0 or 2 cards previously flipped over => make them face down, flip current
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            //1 card previously flipped over => flip current, check if match
            foundMatch = checkForMatch(position, indexOfSingleSelectedCard!!)    //!!: force to int
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) {
            //not a match
            return false
        }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound ++
        return true
    }

    private fun restoreCards() {
        for (card in cards) {   //make unmatched to default state
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        //2 card flips is 1 move
        return numFlips / 2
    }
}
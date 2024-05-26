package com.example.finalproject

import androidx.lifecycle.ViewModel


class LiarsDiceViewModel() : ViewModel() {
    private var allowedBets: Array<Int> = arrayOf<Int>(
        1,2,3,4,5,6,7,8,9,10,11,12,13,14,15
    )

    var game = LiarsDiceGame(strategies = listOf(Conservative, Random))

    fun getAllowedBets() : Array<Int> {
        return allowedBets
    }

    fun setMinBet(minBet: Int) {
        val tmp: MutableList<Int> = mutableListOf()
        for (i in minBet..15) {
            tmp.add(i)
        }
        allowedBets = tmp.toTypedArray()
    }

    fun getMinBet(): Int {
        if (game.getCurrentBet() == null) {
            return 1
        }
        return game.getCurrentBet()!!.num + 1
    }
}
package com.example.finalproject

import android.util.Log
import kotlin.math.pow

interface Strategy {
    /**
     * @param game  A LiarsDiceGame object representing the state of the game
     * @return  The bet of the current turn player (must not be null if first player).
     *          bet.betMaker also must be the current turn player. Null represents a challenge,
     *          and any bet must increase the quantity of the previous bet by 1.
     */
    fun getBet(game: LiarsDiceGame): Bet?

    /**
     * @return The name of the class
     */
    fun getName(): String
}
// could make a modal fun that includes allBets at 0.5 weighting of those beyond 1
// could do another that uses all previous bets as 1


abstract class Random : Strategy {
    companion object : Strategy {
        override fun getName() = "Random"
        override fun getBet(game: LiarsDiceGame): Bet? {
            val tmp = getModeAndFreq(game.getCurrentDie())
            val freq = tmp.second
            val mode = tmp.first
            if (game.getCurrentBet() != null &&
                (!betIsPossible(game.getCurrentBet()!!, freq, game.dicePerPlayer *
                        (game.totalPlayers - 1)) || game.getCurrentBet()!!.num > 7)) {
                return null
            }
            val randomNum = (1..100).random()
            if (game.getCurrentBet() != null && randomNum <= 40) {
                return null
            }
            // again ignoring current and past bets
            var rando = (0..2).random() // EV: 1.25
            if (rando == 2) {
                rando += (0..1).random()
                if (rando == 3)  {
                    rando += (0..1).random()
                }
            }
            if (game.getCurrentBet() == null) {
                return Bet(mode, freq[mode]!! + rando, game.getTurn())
            } else {
                if (freq[mode]!! + rando > game.getCurrentBet()!!.num) {
                    return Bet(mode, freq[mode]!! + rando, game.getTurn())
                }
                rando = if (rando < 1) 1 else rando
                return Bet(mode, game.getCurrentBet()!!.num + rando, game.getTurn())
            }

        }
    }
}

abstract class Conservative : Strategy {
    companion object : Strategy {
        override fun getName() = "Conservative"

        override fun getBet(game: LiarsDiceGame): Bet? {
            val botPlayer = game.getTurn()
            val nextPlayerScore = game.getPlayersScore(game.getNextTurn())
            val botScore = game.getPlayersScore(botPlayer)
            val tmp = getModeAndFreq(game.getCurrentDie())
            val freq = tmp.second
            val mode = tmp.first
            val numOtherDie = (game.totalPlayers - 1) * game.dicePerPlayer
            val p = 1.0 / game.sidesPerDie.toDouble()
            val expectedFloored = (numOtherDie.toDouble() * p).toInt()
            val q = (game.sidesPerDie - 1).toDouble() / (game.sidesPerDie).toDouble()
            if (game.getCurrentBet() != null)
                Log.d("asdf", ""+betIsPossible(game.getCurrentBet()!!, freq, game.dicePerPlayer *
                    (game.totalPlayers - 1)))
            if (game.getCurrentBet() != null && // necessary to avoid negative factorial
                !betIsPossible(game.getCurrentBet()!!, freq, game.dicePerPlayer *
                        (game.totalPlayers - 1))) {
                return null
            }
            if (game.getCurrentBet() == null) {
                var bias = 0
                // takes more risk if about to win and less if they are about to
                // takes more risk if the number we got is unlikely to begin with
                // (3 is the number that drops below 50%)
                if (botScore == game.winningScore - 1 || freq[mode]!! >= 3) {
                    bias = 1
                }
                // This is probably the most conservative part (taking no risk when they're about
                // to win)
                if (nextPlayerScore == game.winningScore - 1) {
                    bias -= 1
                }
                // really not a good strategy, as it reveals what you have
                val betNum: Int = freq[mode]!! + expectedFloored + bias
                return Bet(mode, betNum, botPlayer)
            } else {
                // missing out on info from other people's bets (including currentBet)
                // TODO: Add assumption that one die of each previous bet exists
                val currentBet = game.getCurrentBet()!!
                val prevPlayerScore = game.getPlayersScore(game.getPrevTurn()!!)
                val dieNeededForTheirBet = currentBet.num - freq[currentBet.die]!!
                // subtract one because they definitely have at least have 1 of them
                val probCurrentBetHolds = 1.0 -
                        cumulativeBinomialProb(dieNeededForTheirBet - 1,
                            numOtherDie - 1, p, q)
                // challenge if about to win (and they aren't) and more likely to win than not
                // also for an aggressive one could remove our winning check here
                if (botScore == game.winningScore - 1 && probCurrentBetHolds < 0.50
                    && prevPlayerScore != game.winningScore - 1) {
                    return null
                }
                val dieNeededToRaise = currentBet.num + 1 - freq[mode]!!
                val probRaisingHolds = 1.0 -
                        cumulativeBinomialProb(dieNeededToRaise, numOtherDie, p, q)
                // great place for behavior tweaking: I think this is conservative?
                return if (probRaisingHolds < probCurrentBetHolds) {
                    Bet(mode, currentBet.num + 1, botPlayer)
                } else {
                    null
                }
            }
        }
    }
}

/**
 * Returns whether or the provided bet is possible given the freq provided
 * @param   bet         The bet
 * @param   freq        Hash map of each die face to its known frequency in our roll
 * @param   unknownDice The total of all dice we don't know about
 * @return If the provided bet is possible
 */
fun betIsPossible(bet: Bet, freq: HashMap<Int, Int>, unknownDice: Int): Boolean {
    return (bet.num - freq[bet.die]!! <= unknownDice)
}

/**
 * @param list An Int list of die rolled
 * @return The modal die (randomly selected of all modal die) followed
 * by a hashMap of <dice, frequency of the die>.
 * Pair<modal die, HashMap<die, frequency>>
 */
fun getModeAndFreq(list: List<Int>): Pair<Int, HashMap<Int, Int>> {
    val freq = hashMapOf<Int, Int>(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)
    for (i in list) {
        freq[i] = freq[i]!! + 1
    }
    val max = freq.maxOf{it.value}
    val maxes = mutableListOf<Int>()
    for ((die, num) in freq) {
        if (num == max) {
            maxes.add(die)
        }
    }
    return Pair(maxes.random(), freq)
}

/**
 * Factorial
 * @param   n   Must be <= 20
 * @return  n!
 */
fun fact(n: Int): Long {
    if (n > 20)
        throw IllegalArgumentException("Cannot compute factorial for numbers greater than 20.")
    val range = mutableListOf<Long>()
    for (i in 1..n) {
        range.add(i.toLong())
    }
    return if (n == 0)
        1.toLong()
    else
        range.reduce{acc: Long, x: Long -> x * acc}
}


/**
 * Computes a combination
 * @param   n   The number of objects to choose from j (must be <= 20)
 * @param   r   The number of objects to choose (must be <= 20)
 * @return nCr
 */
fun choose(n: Int, r: Int): Int {
    if (n > 20 || r > 20)
        throw IllegalArgumentException("Cannot choose with arguments greater than 20.")
    return (fact(n) / (fact(r) * fact(n - r))).toInt()
}


/**
 * Binomial distribution
 * @param   x   number of times for a specific outcome within n trials (must be <=20)
 * @param   n   number of trials (must be <=20)
 * @param   p   probability of success on a single trial
 * @param   q   probability of failure on a single trial
 * @return The binomial probability
 */
fun binomialProb(x: Int, n: Int, p: Double, q: Double): Double {
    return choose(n, x) * p.pow(x) * q.pow(n - x)
}

/**
 * Cumulative binomial distribution (LESS THAN here, not <=)
 * @param   x   number of times for a specific outcome within n trials (must be <=20)
 * @param   n   number of trials (must be <=20)
 * @param   p   probability of success on a single trial
 * @param   q   probability of failure on a single trial
 * @return Probability we get < x successes in n trials
 */
fun cumulativeBinomialProb(x: Int, n: Int, p: Double, q: Double): Double {
    var tot = 0.0
    if (x == 0) {
        return 0.0
    }
    for (i in (0..<x)) {
        tot += binomialProb(i, n, p, q)
    }
    return tot
}

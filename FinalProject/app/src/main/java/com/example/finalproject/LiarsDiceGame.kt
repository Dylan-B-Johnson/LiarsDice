package com.example.finalproject

data class Bet (val die: Int, val num: Int, val betMaker: Player)
data class Player (val botOrPlayerNumber: Int, val isHuman: Boolean, val strategy: Strategy?)
class ImproperGetBetImplementationException(message:String): Exception(message)
enum class ReasonForReturn() {
    CHALLENGE_OCCURRED_AND_GAME_OVER,
    HUMAN_TURN,
    CHALLENGE_OCCURRED_BUT_NO_VICTOR
}

class LiarsDiceGame(val winningScore: Int = 3,
                    val bots: Int = 2,
                    val humans: Int = 1,
                    val manualIteration: Boolean = false,
                    val strategies: List<Strategy> = listOf(Conservative, Random)
) {
    val totalPlayers = bots + humans
    val dicePerPlayer = 5
    val sidesPerDie = 6
    private var allBets: MutableList<Bet> = mutableListOf() // we clear this after a re-roll
    private lateinit var turn: Player
    private lateinit var winner: Player
    private lateinit var players: List<Player>
    private lateinit var score: HashMap<Player, Int>
    private lateinit var dice: HashMap<Player, List<Int>>
    private var lastRoundDice: HashMap<Player, List<Int>>? = null
    private var transcript = ""
    private var gameOver: Boolean = false
    private var reasonForReturn: ReasonForReturn? = null
    fun isGameOver() = gameOver
    fun getTurn() = turn
    fun getTranscript() = transcript
    fun clearTranscript() = run{transcript = ""}
    fun getPlayersScore(player: Player) = score[player]
    fun getCurrentDie(): List<Int> = dice[turn]!!
    fun getCurrentBet(): Bet? = if (allBets.size == 0) null else allBets[allBets.size - 1]
    /**
     * Gets a player's die from the previous round of an unfinished game.
     * Used for when the game is not over but we may want to display the results of the last
     * round for a bit.
     * @param player    The player to get the die for.
     * @return  The player's dice or null if it is not appropriate at this time (should only be
     * non-null when someone just challenged but no one won the game)
     */
    fun getLastRoundDice(player: Player): List<Int>? {
        if (lastRoundDice == null) {
            return null
        }
        return lastRoundDice!![player]
    }

    /**
     * Gets the reason that control has been returned to the driver. Should never be null if
     * manualIteration = false.
     */
    fun getReasonForReturn() = reasonForReturn

    fun getIthHuman(i: Int): Player? {
        for (player in players) {
            if (player.isHuman && player.botOrPlayerNumber == i) {
                return player
            }
        }
        return null
    }

    fun getIthBot(i: Int): Player? {
        for (player in players) {
            if (!player.isHuman && player.botOrPlayerNumber == i) {
                return player
            }
        }
        return null
    }

    /**
     * @return The players whose turn it was previously. null if first player's turn.
     */
    fun getPrevTurn(): Player? = if (allBets.size == 0) null else allBets[allBets.size - 1].betMaker

    fun getNextTurn(): Player {
        val playerIndex = players.indexOf(turn)
        return if (playerIndex == players.size - 1) players[0]
        else players[playerIndex + 1]
    }

    /**
     * Call to make the current (human) player raise the current bet or make the first bet.
     * State will update fully until the game is over, it is a human player's turn again,
     * or the dice have been revealed due to a challenge.
     * If manualIteration is true takes the turn and immediately returns (allowing
     * you to control iteration instead of skipping through bot turns).
     * @param   die Side of the die that the human guessed
     * @param   num The number of dice displaying <die> the player guessed
     */
    fun raise(die: Int, num: Int) {
        if (manualIteration) {
            simulateBet(null)
            return
        }
        if (simulateBet(Bet(die, num, turn)))
            return
        while (!simulateBet(turn.strategy!!.getBet(this))) {}
    }

    /**
     * Call to make the current (human) player challenge the current bet.
     * State will update fully until the game is over, it is a human player's turn again,
     * or the dice have been revealed due to a challenge.
     * If manualIteration is true, takes the turn and immediately returns (allowing
     * you to control iteration instead of skipping through bot turns).
     */
    fun challenge() {
        if (manualIteration) {
            simulateBet(null)
            return
        }
        if (simulateBet(null))
            return
        while (!simulateBet(turn.strategy!!.getBet(this))) {}
    }

    /**
     * Continues the game after control was returned due to CHALLENGE_OCCURRED_BUT_NO_VICTOR.
     * State will update fully until the game is over, it is a human player's turn again,
     * or the dice have been revealed due to a challenge.
     */
    fun continueAfterChallenge() {
        if (turn.isHuman) {
            reasonForReturn = ReasonForReturn.HUMAN_TURN
        } else {
            while(!simulateBet(turn.strategy!!.getBet(this))) {}
        }
    }

    /**
     * For use in conjunction with If manualIteration = true. Performs the current player
     * (must be a bot)'s turn and then returns. Does nothing when manualIteration = false.
     */
    fun takeBotTurn() {
        if (!manualIteration) {
            return
        }
        simulateBet(turn.strategy!!.getBet(this))
    }

    init {
        val tmpPlayers = mutableListOf<Player>()
        for (i in 1..humans) {
            tmpPlayers.add(Player(i, true, null))
        }
        for (i in 1..bots) {
            tmpPlayers.add(Player(i, false, strategies.random()))
        }
        val tmpPlayersRandomized = mutableListOf<Player>()
        score = hashMapOf<Player, Int>()
        dice = hashMapOf<Player, List<Int>>()
        for (i in 0..<tmpPlayers.size) {
            val tmpPlayer = tmpPlayers.random()
            tmpPlayers.remove(tmpPlayer)
            tmpPlayersRandomized.add(tmpPlayer)
            score[tmpPlayer] = 0
            dice[tmpPlayer] = roll()
        }
        players = tmpPlayersRandomized.toList()
        turn = players[0]
        transcript += getPlayerName(turn) + " goes first.\n"
        if (!manualIteration) {
            if (!turn.isHuman) {
                while (!simulateBet(turn.strategy!!.getBet(this))) {}
            } else {
                reasonForReturn = ReasonForReturn.HUMAN_TURN
            }
        }
    }

    /**
     * Performs everything necessary to update the state after the turn player returns their bet
     * @param   bet The bet of the current turn player (must not be null if first player).
     *              bet.betMaker also must be the current turn player. Null represents a challenge,
     *              and any bet must increase the quantity of the previous bet by 1.
     * @return  True iff game is over, it is another human player's turn, or the dice have been
     *
     */
    private fun simulateBet(bet: Bet?): Boolean {
        val prefix = if (!turn.isHuman) "The " + turn.strategy.toString() + "strategy " else "Human "
        if (bet == null && getCurrentBet() == null) {
            throw ImproperGetBetImplementationException(prefix + "challenged when " +
                    "they were the first player.")
        }
        if (bet != null && bet.betMaker != turn) {
            throw ImproperGetBetImplementationException(prefix +
                    "returned a bet with someone else as the betMaker.")
        }
        if (bet != null && getCurrentBet() != null && bet.num <= getCurrentBet()!!.num) {
            throw ImproperGetBetImplementationException(prefix + "failed to raise the bet.")
        }
        if (bet != null && (bet.die > sidesPerDie || bet.die < 1)) {
            throw ImproperGetBetImplementationException(prefix + "bet a side that doesn't exist.")
        }
        if (bet == null) {
            val freq = getFreq()
            val betNum = getCurrentBet()!!.num
            val betDie = getCurrentBet()!!.die
            // challenger looses
            if (freq[betDie]!! >= betNum) {
                score[getCurrentBet()!!.betMaker] = score[getCurrentBet()!!.betMaker]!! + 1
                transcript += getPlayerName(turn) + " unsuccessfully challenges " +
                        getPlayerName(getCurrentBet()!!.betMaker) + "'s bet of " +
                        betToString(getCurrentBet()!!) + ".\n" +
                        getPlayerName(getCurrentBet()!!.betMaker) + "'s score is now " +
                        score[getCurrentBet()!!.betMaker] + ".\n"
                if (score[getCurrentBet()!!.betMaker] == winningScore) {
                    transcript += getPlayerName(getCurrentBet()!!.betMaker) + " wins.\n"
                    gameOver = true
                    winner = getCurrentBet()!!.betMaker
                    reasonForReturn = ReasonForReturn.CHALLENGE_OCCURRED_AND_GAME_OVER
                    lastRoundDice = cloneDice()
                    return true
                }
            } else { // challenger wins
                score[turn] = score[turn]!! + 1
                transcript += getPlayerName(turn) + " successfully challenges " +
                        getPlayerName(getCurrentBet()!!.betMaker) + "'s bet of " +
                        betToString(getCurrentBet()!!) + ".\n" +
                        getPlayerName(turn) + "'s score is now " +
                        score[turn] + ".\n"
                if (score[turn] == winningScore) {
                    transcript += getPlayerName(turn) + " wins.\n"
                    gameOver = true
                    winner = turn
                    reasonForReturn = ReasonForReturn.CHALLENGE_OCCURRED_AND_GAME_OVER
                    lastRoundDice = cloneDice()
                    return true
                }
            } // someone challenged but no one won, so we return control with true
            lastRoundDice = cloneDice()
            betReset()
            turn = getNextTurn()
            reasonForReturn = ReasonForReturn.CHALLENGE_OCCURRED_BUT_NO_VICTOR
            return true
        } else { // no one challenged, so we return control (with true) if the new player is human
            transcript += getPlayerName(turn) + " bets " + betToString(bet) + ".\n"
            allBets.add(bet)
            turn = getNextTurn()
            reasonForReturn = if (turn.isHuman) ReasonForReturn.HUMAN_TURN else null
            return turn.isHuman
        }
    }

    private fun cloneDice(): HashMap<Player, List<Int>> {
        val rtn = hashMapOf<Player, List<Int>>()
        for (player in players) {
            rtn[player] = dice[player]!!
        }
        return rtn
    }

    private fun betToString(bet: Bet): String {
        val die = when(bet.die) {
            1 -> {"\u2680"}
            2 -> {"\u2681"}
            3 -> {"\u2682"}
            4 -> {"\u2683"}
            5 -> {"\u2684"}
            6 -> {"\u2685"}
            else -> {bet.die}
        }
        return "" + die + " x " + bet.num
    }

    private fun getPlayerName(player: Player): String {
        return if (player.isHuman) {
            "Human " + player.botOrPlayerNumber
        } else {
            "Bot " + player.botOrPlayerNumber
        }
    }

    /**
     * @return A player's set of rolls assuming fair die
     */
    private fun roll(): List<Int> {
        val tmp: MutableList<Int> = mutableListOf()
        for (i in 1..dicePerPlayer) {
            tmp.add((1..sidesPerDie).random())
        }
        return tmp.toList()
    }

    private fun getFreq(): HashMap<Int, Int> {
        val freq = hashMapOf<Int, Int>()
        for (i in 1.. sidesPerDie) {
            freq[i] = 0
        }
        for ((player, list) in dice) {
            for (i in list) {
                freq[i] = freq[i]!! + 1
            }
        }
        return freq
    }

    /**
     * Re-rolls dice and clears bet history
     */
    private fun betReset() {
        for (player in players) {
            dice[player] = roll()
        }
        allBets.clear()
    }
}
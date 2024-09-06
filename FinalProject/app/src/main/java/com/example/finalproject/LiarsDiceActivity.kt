package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity


class LiarsDiceActivity : AppCompatActivity() {
    private val viewModel: LiarsDiceViewModel by viewModels()
    private lateinit var numSpinner: Spinner
    private lateinit var diceSpinner: Spinner
    private lateinit var challengeButton: Button
    private lateinit var raiseButton: Button
    private lateinit var transcript: TextView
    private lateinit var diceLabelAndScores: TextView
    private lateinit var theirBetLabel: TextView
    private lateinit var yourBetLabel: TextView
    private lateinit var yourDice: List<ImageView>
    private lateinit var bot1Dice: List<ImageView>
    private lateinit var bot2Dice: List<ImageView>
    private lateinit var betDie: ImageView
    private lateinit var betNum: TextView
    private lateinit var linearLayoutOpponentsBet: LinearLayout
    private lateinit var linearLayoutYou: LinearLayout
    private lateinit var linearLayoutDiceBot1: LinearLayout
    private lateinit var linearLayoutDiceBot2: LinearLayout
    private lateinit var linearLayoutYourBet:  LinearLayout
    private lateinit var continueButton: Button
    private lateinit var backButton: Button

    private val diceSpinnerImages = arrayOf<Int>(
        R.drawable.dice1,
        R.drawable.dice2,
        R.drawable.dice3,
        R.drawable.dice4,
        R.drawable.dice5,
        R.drawable.dice6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liars_dice)
        performStatelessWork()
        matchViewModel()

    }

    private fun setChallengeGroupVisibility(visibility: Int) {
        linearLayoutOpponentsBet.visibility = visibility
        theirBetLabel.visibility = visibility
        challengeButton.visibility = visibility
        if (visibility == View.VISIBLE) {
            val label = "Bot " + viewModel.game.getCurrentBet()!!.betMaker.botOrPlayerNumber +
                    "'s bet:"
            theirBetLabel.text = label
            betDie.setImageResource(diceSpinnerImages[viewModel.game.getCurrentBet()!!.die - 1])
            val numString = "" + viewModel.game.getCurrentBet()!!.num
            betNum.text = numString
        }
    }

    private fun setRaiseGroupVisibility(raise: Boolean, visibility: Int) {
        linearLayoutYourBet.visibility = visibility
        yourBetLabel.visibility = visibility
        raiseButton.visibility = visibility
        raiseButton.text = if (raise) "Raise" else "Place Bet"
    }

    private fun setChallengeOccurredGroupVisibility(visibility: Int) {
        linearLayoutDiceBot1.visibility = visibility
        linearLayoutDiceBot2.visibility = visibility
        continueButton.visibility = visibility
        val whoseDie: String = if (visibility == View.INVISIBLE) {
            "\nYour dice:"

        } else {
            "\nEveryone's dice:"

        }
        val diceLabelAndScoresText =  "Your score: \t\t\t" + viewModel.game.getPlayersScore(
            viewModel.game.getIthHuman(1)!!) + "\nBot 1's Score: \t" +
                viewModel.game.getPlayersScore(viewModel.game.getIthBot(1)!!) +
                "\nBot 2's Score: \t" +
                viewModel.game.getPlayersScore(viewModel.game.getIthBot(2)!!) +
                whoseDie
        diceLabelAndScores.text = diceLabelAndScoresText
        if (viewModel.game.isGameOver()) {
            continueButton.text = "Restart"
        } else {
            continueButton.text = "Continue"
        }
    }

    /**
     * Updates the UI to match the state of the ViewModel (which persists between configuration
     * changes)
     */
    private fun matchViewModel() {
        setMinBet(viewModel.getMinBet())
        transcript.text = viewModel.game.getTranscript()
        val currentBetExists = viewModel.game.getCurrentBet() != null
        when (viewModel.game.getReasonForReturn()!!) {
            ReasonForReturn.HUMAN_TURN -> {
                updateDie(true)
                if (currentBetExists) { // can raise or challenge
                    setRaiseGroupVisibility(true, View.VISIBLE)
                    setChallengeGroupVisibility(View.VISIBLE)
                } else { // first player of game or it's a new round
                    setRaiseGroupVisibility(false, View.VISIBLE)
                    setChallengeGroupVisibility(View.INVISIBLE)
                }
                setChallengeOccurredGroupVisibility(View.INVISIBLE)
            }
            ReasonForReturn.CHALLENGE_OCCURRED_BUT_NO_VICTOR -> {
                updateDie(false)
                setRaiseGroupVisibility(false, View.INVISIBLE)
                setChallengeGroupVisibility(View.INVISIBLE)
                setChallengeOccurredGroupVisibility(View.VISIBLE)
            }
            ReasonForReturn.CHALLENGE_OCCURRED_AND_GAME_OVER -> {
                updateDie(false)
                setRaiseGroupVisibility(false, View.INVISIBLE)
                setChallengeGroupVisibility(View.INVISIBLE)
                setChallengeOccurredGroupVisibility(View.VISIBLE)
            }
        }
    }

    private fun performStatelessWork() {
        diceSpinner = findViewById<Spinner>(R.id.bet_dice_spinner) as Spinner
        numSpinner = findViewById<Spinner>(R.id.bet_number_spinner) as Spinner
        raiseButton = findViewById<Button>(R.id.raiseButton) as Button
        backButton = findViewById<Button>(R.id.backButton) as Button
        challengeButton = findViewById<Button>(R.id.challengeButton) as Button
        continueButton = findViewById<Button>(R.id.continueButton) as Button
        transcript = findViewById<TextView>(R.id.transcript) as TextView
        theirBetLabel = findViewById<TextView>(R.id.theirBetLabel) as TextView
        yourBetLabel = findViewById<TextView>(R.id.yourBetLabel) as TextView
        diceLabelAndScores = findViewById<TextView>(R.id.diceLabelAndScores) as TextView
        betDie = findViewById<ImageView>(R.id.oponentBetDie) as ImageView
        betNum = findViewById<TextView>(R.id.opponentsBetNumber) as TextView
        linearLayoutOpponentsBet = findViewById<LinearLayout>(R.id.linearLayoutOpponentsBet) as LinearLayout
        linearLayoutYou = findViewById<LinearLayout>(R.id.linearLayoutYou) as LinearLayout
        linearLayoutDiceBot1 = findViewById<LinearLayout>(R.id.linearLayoutDiceBotA) as LinearLayout
        linearLayoutDiceBot2 = findViewById<LinearLayout>(R.id.linearLayoutDiceBotB) as LinearLayout
        linearLayoutYourBet = findViewById<LinearLayout>(R.id.linearLayoutYourBet) as LinearLayout
        yourDice = listOf(findViewById<ImageView>(R.id.yourDice1) as ImageView,
            findViewById<ImageView>(R.id.yourDice2) as ImageView,
            findViewById<ImageView>(R.id.yourDice3) as ImageView,
            findViewById<ImageView>(R.id.yourDice4) as ImageView,
            findViewById<ImageView>(R.id.yourDice5) as ImageView)
        bot1Dice = listOf(findViewById<ImageView>(R.id.botADice1) as ImageView,
            findViewById<ImageView>(R.id.botADice2) as ImageView,
            findViewById<ImageView>(R.id.botADice3) as ImageView,
            findViewById<ImageView>(R.id.botADice4) as ImageView,
            findViewById<ImageView>(R.id.botADice5) as ImageView)
        bot2Dice = listOf(findViewById<ImageView>(R.id.botBDice1) as ImageView,
            findViewById<ImageView>(R.id.botBDice2) as ImageView,
            findViewById<ImageView>(R.id.botBDice3) as ImageView,
            findViewById<ImageView>(R.id.botBDice4) as ImageView,
            findViewById<ImageView>(R.id.botBDice5) as ImageView)
        raiseButton.setOnClickListener {raiseButtonCallback()}
        challengeButton.setOnClickListener {challengeButtonCallback()}
        continueButton.setOnClickListener {continueButtonCallback()}
        diceSpinner.adapter = SpinnerAdapter(this, diceSpinnerImages)
        backButton.setOnClickListener {backButtonCallback()}
    }

    private fun backButtonCallback() {
        val myIntent = Intent(this, MainActivity::class.java)
        startActivity(myIntent)
    }

    private fun updateSetOfDie(setOfDie: List<ImageView>, useCurrentDice: Boolean, player: Player) {
        for ((i, die) in setOfDie.withIndex()) {
            val switchBy = if (useCurrentDice) {
                viewModel.game.getCurrentDie()[i]
            } else {
                viewModel.game.getLastRoundDice(player)!![i]
            }
            when (switchBy) {
                1 -> {die.setImageResource(R.drawable.dice1)}
                2 -> {die.setImageResource(R.drawable.dice2)}
                3 -> {die.setImageResource(R.drawable.dice3)}
                4 -> {die.setImageResource(R.drawable.dice4)}
                5 -> {die.setImageResource(R.drawable.dice5)}
                6 -> {die.setImageResource(R.drawable.dice6)}
            }
        }
    }

    private fun updateDie(useCurrentDice: Boolean) {
        updateSetOfDie(yourDice, useCurrentDice, viewModel.game.getIthHuman(1)!!)
        if (!useCurrentDice) {
            updateSetOfDie(bot1Dice, false, viewModel.game.getIthBot(1)!!)
            updateSetOfDie(bot2Dice, false, viewModel.game.getIthBot(2)!!)
        }
    }

    private fun getSelectedDie() : Int {
        for (i in 1..6) {
            if (diceSpinner.selectedItem.hashCode() == diceSpinnerImages[i - 1].hashCode()) {
                return i
            }
        }
        return -1
    }

    private fun getSelectedNum() : Int {
        for (i in 0..<viewModel.getAllowedBets().size) {
            if (numSpinner.selectedItem.hashCode() == viewModel.getAllowedBets()[i].hashCode()) {
                return viewModel.getAllowedBets()[i]
            }
        }
        return -1
    }

    private fun setMinBet(minBet: Int) {
        viewModel.setMinBet(minBet)
        // figured this out from: https://www.geeksforgeeks.org/spinner-in-android-using-java-with-example/
        numSpinner.adapter = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_item,
            viewModel.getAllowedBets()
        )
    }

    private fun raiseButtonCallback() {
        viewModel.game.raise(getSelectedDie(), getSelectedNum())
        matchViewModel()
    }

    private fun challengeButtonCallback() {
        viewModel.game.challenge()
        matchViewModel()
    }

    private fun continueButtonCallback() {
        viewModel.game.clearTranscript()
        if (viewModel.game.isGameOver()) {
            viewModel.game = LiarsDiceGame()
        } else {
            viewModel.game.continueAfterChallenge()
        }
        matchViewModel()
    }

}
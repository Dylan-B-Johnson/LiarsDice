package com.example.finalproject

// Dylan Johnson 2024
// All trivia is from The Open Trivia Database
// This file is straight from my graduate student lab, except I added a back button
// and a viewModel and made the UI match the state of that.

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class Token(
    val response_code: Int,
    val response_message: String,
    val token: String
)

data class Question(
    val type: String,
    val difficulty: String,
    val category: String,
    val question: String,
    val correct_answer: String,
    val incorrect_answers: List<String>
)

data class Questions(
    val response_code: Int,
    val results: List<Question>
)

class TriviaActivity : AppCompatActivity() {
    private val viewModel: TriviaViewModel by viewModels()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://opentdb.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service: TriviaService = retrofit.create(TriviaService::class.java)
    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var button4: Button
    private lateinit var backButton: Button
    private lateinit var questionTextBox: TextView
    private lateinit var nextButton: Button
    private lateinit var correctnessTextBox: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trivia)
        button1 = findViewById<View>(R.id.button1) as Button
        button2 = findViewById<View>(R.id.button2) as Button
        button3 = findViewById<View>(R.id.button3) as Button
        button4 = findViewById<View>(R.id.button4) as Button
        nextButton = findViewById<View>(R.id.nextButton) as Button
        nextButton.setBackgroundColor(getColor(R.color.button))
        nextButton.setOnClickListener {next()}
        backButton = findViewById<Button>(R.id.backButton) as Button
        backButton.setOnClickListener {backButtonCallback()}
        correctnessTextBox = findViewById<View>(R.id.correctnessText) as TextView
        resetButtonColors()
        questionTextBox = findViewById<View>(R.id.questionBox) as TextView
        // enables answering the question
        button1.setOnClickListener {answered(1)}
        button2.setOnClickListener {answered(2)}
        button3.setOnClickListener {answered(3)}
        button4.setOnClickListener {answered(4)}
        if (viewModel.questions.isEmpty()) {
            if (!isOnline()) {
                Toast.makeText(baseContext,
                    "Unable to connect to API. Please try again later with an internet connection.",
                    Toast.LENGTH_LONG).show()
                backButtonCallback()
            }
            else {
                lifecycleScope.launch {
                    // gets a session token and then gets 50 questions using that token using a coroutine
                    // (they are suspend functions)
                    try {
                        viewModel.sessionToken = service.getSessionToken().token
                        viewModel.questions = service.getQuestions(viewModel.sessionToken).results
                    } catch (e: Exception) {

                    }
                    // displays first question
                    updateQuestion()
                }
            }
        } else {
            matchViewModel()
        }

    }

    // Assign the button-th button in reading order to the answer provided
    // Removes HTML4 escaping from answer before assinging
    private fun assignButton(button: Int, answer: String) {
        when(button) {
            1 -> {button1.text = StringEscapeUtils.unescapeHtml4(answer)}
            2 -> {button2.text = StringEscapeUtils.unescapeHtml4(answer)}
            3 -> {button3.text = StringEscapeUtils.unescapeHtml4(answer)}
            4 -> {button4.text = StringEscapeUtils.unescapeHtml4(answer)}
        }
    }

    // Updates the question text box to show the current question (based on currentQuestion attr)
    // (After removing HTML4 escaping from the question String)
    // Also randomly assigns answers to the question to the answer buttons and stores the
    // correct answer's button in correctAnswerButton
    private fun updateQuestion() {
        resetButtonColors()
        viewModel.questionTextBoxText = StringEscapeUtils.unescapeHtml4(viewModel.questions[viewModel.currentQuestion].question)
        questionTextBox.text = viewModel.questionTextBoxText
        val options = mutableListOf<Int>(1, 2, 3, 4)
        viewModel.correctAnswerButton = options.random()
        options.remove(viewModel.correctAnswerButton)
        assignButton(viewModel.correctAnswerButton,
            viewModel.questions[viewModel.currentQuestion].correct_answer)
        viewModel.buttonAssignments = mutableListOf<Pair<Int, String>>()
        for (i in 0..2) {
            val button = options.random()
            options.remove(button)
            viewModel.buttonAssignments.add(Pair(button,
                    viewModel.questions[viewModel.currentQuestion].incorrect_answers[i]))
            assignButton(button,
                viewModel.questions[viewModel.currentQuestion].incorrect_answers[i])
        }
    }

    // Changes the color of the correct answer to be green and the rest to be red
    private fun answeredButtonColors() {
        when(viewModel.correctAnswerButton) {
            1 -> {button1.setBackgroundColor(getColor(R.color.green))}
            2 -> {button2.setBackgroundColor(getColor(R.color.green))}
            3 -> {button3.setBackgroundColor(getColor(R.color.green))}
            4 -> {button4.setBackgroundColor(getColor(R.color.green))}
        }
        val incorrectButtons = mutableListOf<Int>(1, 2, 3, 4)
        incorrectButtons.remove(viewModel.correctAnswerButton)
        for (i in incorrectButtons) {
            when(i) {
                1 -> {button1.setBackgroundColor(getColor(R.color.red))}
                2 -> {button2.setBackgroundColor(getColor(R.color.red))}
                3 -> {button3.setBackgroundColor(getColor(R.color.red))}
                4 -> {button4.setBackgroundColor(getColor(R.color.red))}
            }
        }
    }

    // Called when an answer button has been pressed
    // button contains the number (1-4) of the answer in reading order
    // Displays whether or not the answer was correct, makes the next
    // button appear, and changes button colors to represent the
    // correct and incorrect answers
    private fun answered(button: Int) {
        if (button == viewModel.correctAnswerButton) {
            viewModel.correctnessTextBoxText = getString(R.string.correct)
        } else {
            viewModel.correctnessTextBoxText = getString(R.string.incorrect)
        }
        correctnessTextBox.text = viewModel.correctnessTextBoxText
        viewModel.answered = true
        answeredButtonColors()
        nextButton.visibility = View.VISIBLE
        correctnessTextBox.visibility = View.VISIBLE
    }

    // Resets button colors back to their teal original state
    // Also makes the next
    private fun resetButtonColors() {
        button1.setBackgroundColor(getColor(R.color.button))
        button2.setBackgroundColor(getColor(R.color.button))
        button3.setBackgroundColor(getColor(R.color.button))
        button4.setBackgroundColor(getColor(R.color.button))
        nextButton.visibility = View.INVISIBLE
        correctnessTextBox.visibility = View.INVISIBLE
    }

    // Replaces questions with 50 questions not seen by the current sessionToken before
    // If the sessionToken is out of date or we have seen all questions, we get a new
    // session token
    private fun getMoreQuestions() {
        if (!isOnline()) {
            Toast.makeText(baseContext,
                "Unable to connect to API. Please try again later with an internet connection.",
                Toast.LENGTH_LONG).show()
            backButtonCallback()
        }
        lifecycleScope.launch {
            var response: Questions = service.getQuestions(viewModel.sessionToken)
            if (response.response_code == 3 || response.response_code == 4) {
                viewModel.sessionToken = service.getSessionToken().token
                response = service.getQuestions(viewModel.sessionToken)
            }
            viewModel.questions = response.results
        }
    }

    // Callback for the next button
    // Changes state to the next question
    private fun next() {
        nextButton.visibility = View.INVISIBLE
        correctnessTextBox.visibility = View.INVISIBLE
        if (viewModel.currentQuestion == viewModel.questions.size - 1) {
            getMoreQuestions()
            viewModel.currentQuestion = 0
        } else {
            viewModel.currentQuestion++
        }
        updateQuestion()
        viewModel.answered = false
    }

    // From gar's answer here (and converted to Kotlin):
    // https://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-times-out
    // checks for an internet connection
    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }



    private fun matchViewModel() {
        questionTextBox.text = viewModel.questionTextBoxText
        assignButton(viewModel.correctAnswerButton,
            viewModel.questions[viewModel.currentQuestion].correct_answer)
        for (i in viewModel.buttonAssignments) {
            assignButton(i.first, i.second)
        }
        if (viewModel.answered) {
            nextButton.visibility = View.VISIBLE
            correctnessTextBox.visibility = View.VISIBLE
            answeredButtonColors()
            correctnessTextBox.text = viewModel.correctnessTextBoxText
        } else {
            resetButtonColors()
        }
    }

    private fun backButtonCallback() {
        val myIntent = Intent(this, MainActivity::class.java)
        startActivity(myIntent)
    }
}
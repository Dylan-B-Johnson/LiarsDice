package com.example.finalproject

import androidx.lifecycle.ViewModel

class TriviaViewModel() : ViewModel() {
    var sessionToken: String = ""
    var currentQuestion = 0
    var correctAnswerButton = 0
    var answered = false
    var questions: List<Question> = listOf<Question>()
    var correctnessTextBoxText = ""
    var questionTextBoxText = ""
    var buttonAssignments = mutableListOf<Pair<Int, String>>()
}
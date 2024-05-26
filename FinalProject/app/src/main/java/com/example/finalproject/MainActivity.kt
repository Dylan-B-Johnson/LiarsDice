package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var liarsDiceButton: Button
    private lateinit var sensorButton: Button
    private lateinit var triviaButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        liarsDiceButton = findViewById<Button>(R.id.liarsDiceButton)
        liarsDiceButton.setOnClickListener() {liarsDiceButtonCallback()}
        sensorButton = findViewById<Button>(R.id.levelButton)
        sensorButton.setOnClickListener() {sensorButtonCallback()}
        triviaButton = findViewById<Button>(R.id.triviaButton)
        triviaButton.setOnClickListener() {triviaButtonCallback()}
    }

    private fun liarsDiceButtonCallback() {
        val myIntent = Intent(this, LiarsDiceActivity::class.java)
        startActivity(myIntent)
    }

    private fun sensorButtonCallback() {
        val myIntent = Intent(this, SensorActivity::class.java)
        startActivity(myIntent)
    }

    private fun triviaButtonCallback() {
        val myIntent = Intent(this, TriviaActivity::class.java)
        startActivity(myIntent)
    }
}
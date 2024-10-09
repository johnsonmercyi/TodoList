package com.example.todolist.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.todolist.R
import com.example.todolist.api.RetrofitInstance
import com.example.todolist.entity.QuoteResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.awaitResponse

class QuoteActivity : AppCompatActivity() {

    private lateinit var quoteTextView: TextView
    private lateinit var quoteAuthorTextView: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quote)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Quote of The Day"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.quoteActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /**
         * Initialize UI components and other implementations below
         */
        initComponents()

        // Fetch quote from API
        loadQuoteOfTheDay()
    }

    private fun loadQuoteOfTheDay() {
        // Show loading indicator
        progressBar.visibility = ProgressBar.VISIBLE

        // Accessing the API through the RetrofitInstance
        val quoteApiService = RetrofitInstance.api
        val apiKey = "FHktESVcIRc3EBcztL1RdGNY16iqtw5UdFReVHvTdc75d691"

        lifecycleScope.launch {
            var retryCount = 0
            val maxRetries = 5

            while (retryCount < maxRetries) {
                try {
                    // Fetching the quote of the day from the API
                    val quoteResponse = quoteApiService.getQuoteOfTheDay(apiKey)
                    val quote = quoteResponse.contents.quotes.firstOrNull()

                    if (quote != null) {
                        quoteTextView.text = quote.quote
                        "~ ${quote.author}".also { quoteAuthorTextView.text = it }
                    } else {
                        "Failed to load quote.".also { quoteTextView.text = it }
                    }
                    break // Exit the loop on success
                } catch (e: Exception) {
                    // Handle any exceptions, including HTTP 429
                    progressBar.visibility = ProgressBar.GONE

                    when (e) {
                        is HttpException -> {
                            if (e.code() == 429) {
                                "Rate limit exceeded. Please wait a moment and try again.".also { quoteTextView.text = it }
                            } else {
                                "Error fetching quote: ${e.message}".also { quoteTextView.text = it }
                            }
                        }
                        else -> {
                            "Error fetching quote: ${e.message}".also { quoteTextView.text = it }
                        }
                    }

                    // Retry logic
                    retryCount++
                    if (retryCount < maxRetries) {
                        // Implement a delay before retrying
                        delay(900_000L * retryCount) // Increase delay with each retry
                    } else {
                        "Failed to fetch quote after $maxRetries attempts.".also { quoteTextView.text = it }
                    }
                } finally {
                    // Hide the loading indicator
                    progressBar.visibility = ProgressBar.GONE
                }
            }
        }
    }

    /**
     * Initialize UI components.
     */
    private fun initComponents() {
        quoteTextView = findViewById(R.id.quoteText)
        quoteAuthorTextView = findViewById(R.id.quoteAuthor)
        progressBar = findViewById(R.id.loadingIndicator)

        // Show the loading indicator
        progressBar.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Handle back button press
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
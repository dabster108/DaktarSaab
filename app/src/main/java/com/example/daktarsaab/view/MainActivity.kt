package com.example.daktarsaab.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.daktarsaab.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
<<<<<<< HEAD
        enableEdgeToEdge()
        setContent {
            DaktarSaabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DaktarSaabTheme {
        Greeting("Android")
    }
=======
        window.statusBarColor = getColor(R.color.black)

        // Immediately redirect to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
>>>>>>> d72206d09717a8581f6e5129dc331eae0a61bccc
}
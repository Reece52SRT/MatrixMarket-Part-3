package com.matrixmarket.app

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.matrixmarket.app.ui.navigation.MatrixMarketNavGraph
import com.matrixmarket.app.ui.theme.MatrixMarketTheme

// Single-activity app; every screen is a Jetpack Compose destination.
// AppCompatActivity (a FragmentActivity) is required by BiometricPrompt and makes
// the in-app language switcher re-apply translations automatically.
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate (restored=${savedInstanceState != null})")
        setContent {
            MatrixMarketTheme {
                MatrixMarketNavGraph()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy (changingConfigurations=$isChangingConfigurations)")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

package com.example.scrolltrek.ui.sharecard

import android.content.Context
import android.content.Intent
import com.example.scrolltrek.data.model.Landmark

object ShareCardRenderer {

    fun shareLandmark(context: Context, landmark: Landmark) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, landmark.shareMessage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share scrollTrek Landmark"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

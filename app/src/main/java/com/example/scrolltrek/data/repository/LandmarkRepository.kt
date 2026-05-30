package com.example.scrolltrek.data.repository

import android.content.Context
import com.example.scrolltrek.data.model.Landmark
import com.example.scrolltrek.data.model.LandmarkOrientation
import com.example.scrolltrek.data.model.LandmarkTier
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LandmarkRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val landmarks = mutableListOf<Landmark>()

    init {
        loadLandmarks()
    }

    private fun loadLandmarks() {
        try {
            val inputStream = context.assets.open("landmarks.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }
            reader.close()

            val jsonArray = JSONArray(stringBuilder.toString())
            val rawList = mutableListOf<Landmark>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val landmark = Landmark(
                    id = jsonObject.getString("id"),
                    name = jsonObject.getString("name"),
                    location = jsonObject.getString("location"),
                    distanceMeters = jsonObject.getDouble("distanceMeters"),
                    tier = LandmarkTier.valueOf(jsonObject.getString("tier")),
                    orientation = LandmarkOrientation.valueOf(jsonObject.getString("orientation")),
                    funFact = jsonObject.getString("funFact"),
                    shareMessage = jsonObject.getString("shareMessage"),
                    illustrationRes = jsonObject.getString("illustrationRes"),
                    cardGradientStart = jsonObject.getString("cardGradientStart"),
                    cardGradientEnd = jsonObject.getString("cardGradientEnd")
                )
                rawList.add(landmark)
            }
            
            // Sort rawList by distance
            rawList.sortBy { it.distanceMeters }
            
            // Filter: starting with >= 1.0m and ensuring consecutive distance gap >= 1.0m
            var lastDistance = -999.0
            for (landmark in rawList) {
                if (landmark.distanceMeters >= 1.0 && (landmark.distanceMeters - lastDistance) >= 1.0) {
                    landmarks.add(landmark)
                    lastDistance = landmark.distanceMeters
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets all loaded landmarks, sorted by distance.
     */
    fun getAll(): List<Landmark> {
        return landmarks
    }

    /**
     * Finds a landmark by its unique ID.
     */
    fun getById(id: String): Landmark? {
        return landmarks.find { it.id == id }
    }

    /**
     * Returns the closest locked landmark representing the next goal.
     */
    fun getNextLandmark(currentDistanceM: Double): Landmark? {
        return landmarks.firstOrNull { it.distanceMeters > currentDistanceM }
    }

    /**
     * Returns the closest unlocked landmark.
     */
    fun getCurrentLandmark(currentDistanceM: Double): Landmark? {
        return landmarks.lastOrNull { it.distanceMeters <= currentDistanceM }
    }
}

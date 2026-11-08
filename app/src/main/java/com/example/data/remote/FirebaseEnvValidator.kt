package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Validates Firebase initialization and provides resilient fallbacks when running
 * in clean CI/CD or external developer environments where Google Services configuration
 * may not yet be provisioned.
 */
object FirebaseEnvValidator {
    private const val TAG = "FirebaseEnvValidator"

    fun isFirebaseConfigured(context: Context): Boolean {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                true
            } else {
                FirebaseApp.initializeApp(context) != null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase is not configured in this environment: ${e.message}")
            false
        }
    }

    fun getAuthOrNull(context: Context): FirebaseAuth? {
        if (!isFirebaseConfigured(context)) return null
        return try {
            Firebase.auth
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
            null
        }
    }

    fun getFirestoreOrNull(context: Context): FirebaseFirestore? {
        if (!isFirebaseConfigured(context)) return null
        return try {
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            if (databaseId.isBlank() || databaseId == "(default)") {
                FirebaseFirestore.getInstance()
            } else {
                FirebaseFirestore.getInstance(databaseId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore unavailable: ${e.message}")
            null
        }
    }
}

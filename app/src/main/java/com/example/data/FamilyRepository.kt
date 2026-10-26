package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Family
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FamilyRepository(private val context: Context? = null) {

    private fun ensureFirebaseInitialized() {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:248460314025:android:feae4bcd4fb5f7050fb0b8")
                    .setApiKey("AIzaSyAoPn8qfSI98DvPPmJZyHNVJQTkXgbRzrE")
                    .setProjectId("pocket-home-3d327")
                    .setStorageBucket("pocket-home-3d327.firebasestorage.app")
                    .setGcmSenderId("248460314025")
                    .setDatabaseUrl("https://pocket-home-3d327-default-rtdb.firebaseio.com")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        } catch (e: Throwable) {
            Log.w("FamilyRepo", "FirebaseApp init check: ${e.message}")
        }
    }

    private val db: FirebaseDatabase? by lazy {
        try {
            context?.let { ensureFirebaseInitialized() }
            FirebaseDatabase.getInstance()
        } catch (e: Throwable) {
            Log.e("FamilyRepo", "Failed to get RTDB instance", e)
            null
        }
    }

    suspend fun createFamily(family: Family): String {
        val database = db ?: return family.id
        val familyRef = database.reference.child("families").child(family.id)
        familyRef.setValue(family).await()
        return family.id
    }

    suspend fun getFamily(familyId: String): Family? {
        val database = db ?: return null
        val snapshot = database.reference.child("families").child(familyId).get().await()
        return snapshot.getValue(Family::class.java)
    }

    suspend fun addMemberToFamily(familyId: String, userId: String) {
        val database = db ?: return
        val family = getFamily(familyId) ?: return
        val updatedMembers = family.members.toMutableList()
        if (!updatedMembers.contains(userId)) {
            updatedMembers.add(userId)
            database.reference.child("families").child(familyId).child("members").setValue(updatedMembers).await()
        }
    }
}

package com.example.data

import com.example.model.Family
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FamilyRepository {
    private val db = FirebaseDatabase.getInstance()

    suspend fun createFamily(family: Family): String {
        val familyRef = db.reference.child("families").child(family.id)
        familyRef.setValue(family).await()
        return family.id
    }

    suspend fun getFamily(familyId: String): Family? {
        val snapshot = db.reference.child("families").child(familyId).get().await()
        return snapshot.getValue(Family::class.java)
    }

    suspend fun addMemberToFamily(familyId: String, userId: String) {
        val family = getFamily(familyId) ?: return
        val updatedMembers = family.members.toMutableList()
        if (!updatedMembers.contains(userId)) {
            updatedMembers.add(userId)
            db.reference.child("families").child(familyId).child("members").setValue(updatedMembers).await()
        }
    }
}

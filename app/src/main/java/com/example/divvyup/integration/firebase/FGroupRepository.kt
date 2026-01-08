package com.example.divvyup.integration.firebase

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.User
import com.example.divvyup.domain.repository.GroupRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlin.text.get

class FGroupRepository(private val db: FirebaseFirestore) : GroupRepository {
    companion object {
        private const val GROUP_COLLECTION = "group"
        private const val USERS_LIST = "users"
        private const val SPENDS_LIST = "spends"
    }

    override suspend fun createOrUpdateGroup(group: Group) {
        try {
            db.collection(GROUP_COLLECTION)
                .document(group.id).set(group)
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception("Error al crear o actualizar el grupo: ${e.message}", e)
        } catch (e: Exception) {
            throw Exception("Error inesperado al guardar el grupo", e)
        }
    }

    override suspend fun getGroups(): List<Group> {
        try {
            val groups = db.collection(GROUP_COLLECTION)
                .get()
                .await()

            return groups.documents.mapNotNull { document ->
                document.toObject(Group::class.java)
            }
        } catch (e: FirebaseFirestoreException) {
            throw Exception("Error al obtener los grupos: ${e.message}", e)
        } catch (e: Exception) {
            throw Exception("Error inesperado al obtener los grupos", e)
        }
    }

    override suspend fun getGroup(groupId: String): Group? {
        try {
            val group = db.collection(GROUP_COLLECTION)
                .document(groupId)
                .get()
                .await()

            return group.toObject(Group::class.java)
        } catch (e: FirebaseFirestoreException) {
            throw Exception("Error al obtener el grupo: ${e.message}", e)
        } catch (e: Exception) {
            throw Exception("Error inesperado al obtener el grupo", e)
        }
    }

    override suspend fun addUser(groupId: String, user: User) {
        try {
            db.collection(GROUP_COLLECTION)
                .document(groupId)
                .update(USERS_LIST, FieldValue.arrayUnion(user))
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception("Error al añadir el usuario al grupo: ${e.message}", e)
        } catch (e: Exception) {
            throw Exception("Error inesperado al añadir usuario", e)
        }
    }

    override suspend fun removeUser(groupId: String, user: User) {
        try {
            db.collection(GROUP_COLLECTION)
                .document(groupId)
                .update(USERS_LIST, FieldValue.arrayRemove(user))
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception("Error al eliminar el usuario del grupo: ${e.message}", e)
        } catch (e: Exception) {
            throw Exception("Error inesperado al eliminar usuario", e)
        }
    }

    override suspend fun addOrUpdateSpend(groupId: String, spend: Spend) {
        try {
            db.collection(GROUP_COLLECTION)
                .document(groupId)
                .update(SPENDS_LIST, FieldValue.arrayUnion(spend))
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception("Error al añadir o actualizar el gasto: ${e.message}", e)
        } catch (e: Exception) {
            throw Exception("Error inesperado al guardar el gasto", e)
        }
    }

    override suspend fun removeSpend(groupId: String, spend: Spend) {
        try {
            db.collection(GROUP_COLLECTION)
                .document(groupId)
                .update(SPENDS_LIST, FieldValue.arrayRemove(spend))
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception("Error al eliminar el gasto: ${e.message}", e)
        } catch (e: Exception) {
            throw Exception("Error inesperado al eliminar el gasto", e)
        }
    }
}

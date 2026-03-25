// src/test/java/.../FirestoreAnnotationsContractTest.kt
package com.browntowndev.liftlab.core.data.remote.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import java.util.Date

class FirestoreAnnotationsContractTest {

    @Test
    fun `ProgramRemoteDto exposes @DocumentId and @ServerTimestamp via inheritance or accessors`() {
        val clazz = ProgramRemoteDto::class.java

        // Resolve backing fields, walking superclass chain (BaseRemoteDto holds them)
        val remoteIdField = findFieldDeep(clazz, "remoteId")
        val lastUpdatedField = findFieldDeep(clazz, "lastUpdated")
        val deletedField = findFieldDeep(clazz, "deleted")
        val syncedField = findFieldDeep(clazz, "synced")

        // Accept annotation either on field OR on getter method — Firestore supports both
        val hasDocumentId =
            remoteIdField?.getAnnotation(DocumentId::class.java) != null ||
                    findGetter(clazz, "remoteId")?.getAnnotation(DocumentId::class.java) != null

        val hasServerTimestamp =
            lastUpdatedField?.getAnnotation(ServerTimestamp::class.java) != null ||
                    findGetter(clazz, "lastUpdated")?.getAnnotation(ServerTimestamp::class.java) != null

        assertTrue(hasDocumentId, "@DocumentId missing on remoteId (field or getter)")
        assertTrue(hasServerTimestamp, "@ServerTimestamp missing on lastUpdated (field or getter)")

        // Verify mutability (Firestore sets via reflection; we can set via field or setter)
        val dto = ProgramRemoteDto(
            id = 1L,
            name = "P1",
            deloadWeek = 0,
            isActive = true,
            currentMicrocycle = 0,
            currentMicrocyclePosition = 0,
            currentMesocycle = 0,
        )

        // Prefer fields if present; otherwise use setters
        if (remoteIdField != null && lastUpdatedField != null) {
            remoteIdField.isAccessible = true
            lastUpdatedField.isAccessible = true
            deletedField?.isAccessible = true
            syncedField?.isAccessible = true

            remoteIdField.set(dto, "abc123")
            val now = Date()
            lastUpdatedField.set(dto, now)
            deletedField?.setBoolean(dto, true)
            syncedField?.setBoolean(dto, false)
        } else {
            // fallback to setters via JavaBeans convention
            findSetter(clazz, "remoteId", String::class.java)?.invoke(dto, "abc123")
            val now = Date()
            findSetter(clazz, "lastUpdated", Date::class.java)?.invoke(dto, now)
            findSetter(clazz, "deleted", java.lang.Boolean.TYPE)?.invoke(dto, true)
            findSetter(clazz, "synced", java.lang.Boolean.TYPE)?.invoke(dto, false)

        }

        assertEquals("abc123", dto.remoteId)
        assertTrue(dto.deleted)
        assertFalse(dto.synced)
        assertNotNull(dto.lastUpdated) // set either by us or later by Firestore server
    }
}

/** Walks the class hierarchy to find a declared field. */
private fun findFieldDeep(type: Class<*>, name: String): Field? {
    var c: Class<*>? = type
    while (c != null) {
        try {
            return c.getDeclaredField(name)
        } catch (_: NoSuchFieldException) {
            c = c.superclass
        }
    }
    return null
}

private fun findGetter(type: Class<*>, prop: String) =
    try { type.getMethod("get${prop.replaceFirstChar { it.uppercase() }}") } catch (_: Exception) { null }

private fun findSetter(type: Class<*>, prop: String, param: Class<*>) =
    try { type.getMethod("set${prop.replaceFirstChar { it.uppercase() }}", param) } catch (_: Exception) { null }

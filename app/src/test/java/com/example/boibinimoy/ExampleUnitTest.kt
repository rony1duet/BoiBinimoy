package com.example.boibinimoy

import com.example.boibinimoy.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun userProfile_hasPasswordHashOnly_noPlainPassword() {
        val fieldNames = UserProfile::class.java.declaredFields.map { it.name }
        assertTrue("UserProfile must contain passwordHash", fieldNames.contains("passwordHash"))
        assertFalse("UserProfile must NEVER contain plain password field", fieldNames.contains("password"))
    }

    @Test
    fun bookCover_dataUriPrefix() {
        val rawBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        val dataUrl = "data:image/jpeg;base64,$rawBase64"
        assertTrue("dataUrl must start with data:image", dataUrl.startsWith("data:image"))
        val extracted = dataUrl.substringAfter("base64,", "")
        assertEquals(rawBase64, extracted)
    }
}
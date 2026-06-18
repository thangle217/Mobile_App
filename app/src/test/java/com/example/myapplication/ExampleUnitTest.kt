package com.example.myapplication

import com.example.myapplication.domain.util.isValidEmail
import com.example.myapplication.domain.util.isValidPhone
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun emailValidation_isCorrect() {
        assertTrue(isValidEmail("test@example.com"))
        assertTrue(isValidEmail("user.name+tag@domain.co.uk"))
        assertFalse(isValidEmail("invalid-email"))
        assertFalse(isValidEmail("user@"))
        assertFalse(isValidEmail("user@domain"))
    }

    @Test
    fun phoneValidation_isCorrect() {
        assertTrue(isValidPhone("0912345678"))
        assertTrue(isValidPhone("+84912345678"))
        assertTrue(isValidPhone("0312345678"))
        assertFalse(isValidPhone("123456789"))
        assertFalse(isValidPhone("091234567"))
        assertFalse(isValidPhone("0412345678"))
    }
}
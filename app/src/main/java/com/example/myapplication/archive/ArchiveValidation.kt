package com.example.myapplication.archive

object ArchiveValidation {
    fun isValidEmail(email: String): Boolean = email.contains("@")
}
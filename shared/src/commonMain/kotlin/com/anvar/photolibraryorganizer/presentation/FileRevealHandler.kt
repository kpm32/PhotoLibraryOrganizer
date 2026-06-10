package com.anvar.photolibraryorganizer.presentation

interface FileRevealHandler {
    fun reveal(path: String): Boolean
}

object PreviewFileRevealHandler : FileRevealHandler {
    override fun reveal(path: String): Boolean = false
}

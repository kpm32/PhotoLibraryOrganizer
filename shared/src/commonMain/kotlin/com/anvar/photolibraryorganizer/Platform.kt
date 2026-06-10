package com.anvar.photolibraryorganizer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
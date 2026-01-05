package org.nv.vinylmanager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
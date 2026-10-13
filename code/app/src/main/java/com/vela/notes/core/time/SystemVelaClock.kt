package com.vela.notes.core.time

import javax.inject.Inject

class SystemVelaClock @Inject constructor() : VelaClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

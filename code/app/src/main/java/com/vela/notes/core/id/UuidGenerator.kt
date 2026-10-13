package com.vela.notes.core.id

import java.util.UUID
import javax.inject.Inject

class UuidGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}

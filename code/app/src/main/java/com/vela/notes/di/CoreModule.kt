package com.vela.notes.di

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.id.UuidGenerator
import com.vela.notes.core.time.SystemVelaClock
import com.vela.notes.core.time.VelaClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {
    @Binds
    abstract fun bindClock(implementation: SystemVelaClock): VelaClock

    @Binds
    abstract fun bindIdGenerator(implementation: UuidGenerator): IdGenerator
}

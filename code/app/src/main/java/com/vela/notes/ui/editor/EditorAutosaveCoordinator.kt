package com.vela.notes.ui.editor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EditorAutosaveCoordinator<T>(
    scope: CoroutineScope,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val onError: (Throwable) -> Unit = {},
    private val save: suspend (T) -> Unit,
) {
    private val commands = Channel<Command<T>>(Channel.UNLIMITED)
    private val worker = scope.launch { processCommands() }

    fun submit(value: T) {
        check(commands.trySend(Command.Submit(value)).isSuccess) {
            "Autosave coordinator is closed"
        }
    }

    suspend fun flush() {
        val completion = CompletableDeferred<Unit>()
        commands.send(Command.Flush(completion))
        completion.await()
    }

    fun close() {
        commands.close()
        worker.cancel()
    }

    private suspend fun processCommands() = coroutineScope {
        var pending: T? = null
        var revision = 0L
        var timer: Job? = null

        for (command in commands) {
            when (command) {
                is Command.Submit -> {
                    pending = command.value
                    revision += 1
                    timer?.cancel()
                    val scheduledRevision = revision
                    timer = launch {
                        delay(debounceMillis)
                        commands.send(Command.Save(scheduledRevision))
                    }
                }
                is Command.Save -> {
                    if (command.revision == revision) {
                        timer = null
                        pending?.let { value ->
                            try {
                                save(value)
                                pending = null
                            } catch (error: Throwable) {
                                if (error is CancellationException) throw error
                                onError(error)
                            }
                        }
                    }
                }
                is Command.Flush -> {
                    timer?.cancel()
                    timer = null
                    pending?.let { value ->
                        try {
                            save(value)
                            pending = null
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            onError(error)
                            command.completion.completeExceptionally(error)
                            return@let
                        }
                    }
                    if (!command.completion.isCompleted) command.completion.complete(Unit)
                }
            }
        }
    }

    private sealed interface Command<out T> {
        data class Submit<T>(val value: T) : Command<T>
        data class Save(val revision: Long) : Command<Nothing>
        data class Flush(val completion: CompletableDeferred<Unit>) : Command<Nothing>
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 500L
    }
}

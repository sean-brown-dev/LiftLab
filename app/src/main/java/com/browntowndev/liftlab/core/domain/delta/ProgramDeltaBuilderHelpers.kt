package com.browntowndev.liftlab.core.domain.delta

// -----------------------------------------------
// Tiny DSL / Builders for ergonomic use-case code
// -----------------------------------------------

/** Top-level DSL entry to build a ProgramDelta. */
fun programDelta(build: ProgramDeltaBuilder.() -> Unit): ProgramDelta =
    ProgramDeltaBuilder().apply(build).build()

/** Top-level DSL entry to build a ProgramDelta. */
suspend fun programDeltaSuspend(suspendBuild: suspend ProgramDeltaBuilder.() -> Unit): ProgramDelta =
    ProgramDeltaBuilder().apply {
        suspendBuild()
    }.build()

package com.browntowndev.liftlab.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class TestDispatchers(
    override val io: CoroutineDispatcher = Dispatchers.Main.immediate,
    override val main: CoroutineDispatcher = Dispatchers.Main.immediate,
    override val default: CoroutineDispatcher = Dispatchers.Main.immediate,
) : AppDispatchers

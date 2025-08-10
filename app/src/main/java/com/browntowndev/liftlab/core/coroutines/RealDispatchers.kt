package com.browntowndev.liftlab.core.coroutines

import kotlinx.coroutines.Dispatchers

class RealDispatchers : AppDispatchers {
    override val io = Dispatchers.IO
    override val default = Dispatchers.Default
    override val main = Dispatchers.Main.immediate
}
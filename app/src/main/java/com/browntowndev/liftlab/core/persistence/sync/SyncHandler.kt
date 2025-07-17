package com.browntowndev.liftlab.core.persistence.sync

import com.browntowndev.liftlab.core.persistence.dtos.firestore.BaseFirestoreDto

data class SyncHandler<T : BaseFirestoreDto>(
    val getMany: suspend (List<Long>) -> List<T>,
    val upsertMany: suspend (List<T>) -> Unit
)

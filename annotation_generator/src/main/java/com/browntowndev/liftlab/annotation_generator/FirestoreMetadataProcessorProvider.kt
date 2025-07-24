package com.browntowndev.liftlab.annotation_generator

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class FirestoreMetadataProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FirestoreMetadataProcessor(
            environment.codeGenerator,
            environment.logger
        )
    }
}

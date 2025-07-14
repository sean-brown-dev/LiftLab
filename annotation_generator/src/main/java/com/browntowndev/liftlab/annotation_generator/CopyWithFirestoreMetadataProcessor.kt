package com.browntowndev.liftlab.annotation_generator

import com.browntowndev.liftlab.annotations.GenerateCopyWithFirestoreMetadata
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class CopyWithFirestoreMetadataProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("🔥 KSP Processor is running!")
        val symbols = resolver.getSymbolsWithAnnotation(GenerateCopyWithFirestoreMetadata::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        logger.info("Found ${symbols.count()} classes annotated with @GenerateCopyWithMetadata")

        for (symbol in symbols) {
            val className = symbol.simpleName.asString()
            val packageName = symbol.packageName.asString()

            logger.info("Processing class: $packageName.$className")

            try {
                val file = codeGenerator.createNewFile(
                    Dependencies(false, symbol.containingFile!!),
                    packageName,
                    "${className}CopyWithMetadata"
                )

                file.bufferedWriter().use { writer ->
                    writer.write(
                        """
                        package $packageName
                        
                        import java.util.Date

                        fun $className.copyWithFirestoreMetadata(firestoreId: String?, lastUpdated: Date?, synced: Boolean): $className {
                            return this.copy().apply {
                                this.firestoreId = firestoreId
                                this.lastUpdated = lastUpdated
                                this.synced = synced
                            }
                        }
                        """.trimIndent()
                    )
                }

                logger.info("Generated extension for $className")

            } catch (e: Exception) {
                logger.error("Failed to generate copyWithMetadata for $className: ${e.message}", symbol)
            }
        }

        return emptyList()
    }
}

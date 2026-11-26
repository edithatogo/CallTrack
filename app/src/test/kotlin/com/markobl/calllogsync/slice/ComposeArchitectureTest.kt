// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.provider.KoContainingDeclarationProvider
import com.lemonappdev.konsist.api.provider.KoFullyQualifiedNameProvider
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeArchitectureTest {
    @Test
    fun repositoryHasNoXmlLayouts() {
        val layouts = files.filter { file ->
            val directory = file.parentFile
            file.extension == "xml" &&
                directory?.parentFile?.name == "res" &&
                (directory.name == "layout" || directory.name.startsWith("layout-"))
        }
        assertTrue("XML layouts: ${layouts.joinToString()}", layouts.isEmpty())
    }

    @Test
    fun repositoryUsesMaterial3Imports() {
        val kotlinImports =
            scope.imports.filter { it.name.startsWith(MATERIAL2) }.map { it.location }
        val javaImports =
            javaSources
                .filterValues { unit ->
                    unit.imports.any {
                        it.nameAsString == MATERIAL2.dropLast(1) ||
                            it.nameAsString.startsWith(MATERIAL2)
                    }
                }
                .keys
        val violations = kotlinImports + javaImports.map { it.relativeTo(root).path }
        assertTrue("Material 2 imports: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun repositoryHasNoViewOrFragmentSubclasses() {
        val kotlinClasses =
            scope
                .classesAndObjects()
                .filter { declaration ->
                    declaration.parents().any { parent ->
                        declaration.containingFile.hasLegacyParent(parent.name)
                    }
                }
                .map { it.location }
        val javaClasses =
            javaSources
                .filterValues { unit ->
                    val imports = unit.imports.filterNot { it.isStatic }
                    unit.findAll(ClassOrInterfaceDeclaration::class.java).any { declaration ->
                        declaration.extendedTypes.any { parent ->
                            val name = parent.nameWithScope
                            isLegacyType(name) ||
                                imports.any {
                                    val candidate =
                                        if (it.isAsterisk) {
                                            "${it.nameAsString}.$name"
                                        } else {
                                            it.nameAsString
                                        }
                                    candidate.substringAfterLast('.') == parent.nameAsString &&
                                        isLegacyType(candidate)
                                }
                        }
                    }
                }
                .keys
        val violations = kotlinClasses + javaClasses.map { it.relativeTo(root).path }
        assertTrue(
            "View or Fragment subclasses: ${violations.joinToString()}",
            violations.isEmpty(),
        )
    }

    @Test
    fun publicUnitComposablesHaveComponentNamesAndDefaultModifiers() {
        val violations =
            scope.functions(includeLocal = false).filter { function ->
                function.annotatedWith(COMPOSABLE) &&
                    function.isPublicApi() &&
                    !function.annotatedWith(PREVIEW) &&
                    (!function.name.first().isUpperCase() || !function.hasDefaultModifier()) &&
                    function.returnsUnit()
            }
        assertTrue(
            "Public Unit composables need an uppercase name and modifier: Modifier = Modifier: " +
                violations.joinToString { it.location },
            violations.isEmpty(),
        )
    }

    private fun KoFunctionDeclaration.hasDefaultModifier() = parameters.any {
        it.name == "modifier" &&
            MODIFIER in containingFile.candidates(it.type.text) &&
            MODIFIER in containingFile.candidates(it.defaultValue.orEmpty())
    }

    private fun KoFunctionDeclaration.annotatedWith(type: String) = annotations.any {
        val name = it.text.removePrefix("@").substringBefore('(').trim()
        type in containingFile.candidates(name)
    }

    private fun KoFunctionDeclaration.isPublicApi(): Boolean {
        val owners =
            generateSequence(containingDeclaration) {
                (it as? KoContainingDeclarationProvider)?.containingDeclaration
            }
        return hasPublicOrDefaultModifier &&
            owners
                .takeWhile { it !is KoFileDeclaration }
                .filterIsInstance<KoVisibilityModifierProvider>()
                .all { it.hasPublicOrDefaultModifier }
    }

    private fun KoFunctionDeclaration.returnsUnit(): Boolean =
        returnType?.let { it.text in setOf("Unit", "kotlin.Unit") }
            ?: (!hasReturnValue || compiledReturnsUnit())

    private fun KoFunctionDeclaration.compiledReturnsUnit(): Boolean {
        // Konsist cannot infer an expression body's type. Use the compiled Compose method.
        val owner =
            (containingDeclaration as? KoFullyQualifiedNameProvider)?.fullyQualifiedName
                ?: listOfNotNull(
                        containingFile.packagee?.name,
                        File(path).nameWithoutExtension + "Kt",
                    )
                    .joinToString(".")
        val methods =
            loadClass(owner)?.declaredMethods.orEmpty().filter {
                it.name.substringBefore('-') == name &&
                    it.parameterTypes.any { p -> p.name == COMPOSER }
            }
        require(methods.isNotEmpty()) {
            "Declare the return type for this uncompiled composable: $location"
        }
        return methods.any { it.returnType == Void.TYPE }
    }

    companion object {
        private const val MATERIAL2 = "androidx.compose.material."
        private const val MODIFIER = "androidx.compose.ui.Modifier"
        private const val COMPOSABLE = "androidx.compose.runtime.Composable"
        private const val COMPOSER = "androidx.compose.runtime.Composer"
        private const val PREVIEW = "androidx.compose.ui.tooling.preview.Preview"
        private val legacy =
            setOf("android.view.View", "android.view.ViewGroup", "androidx.fragment.app.Fragment")
        private val root = File(requireNotNull(System.getProperty("calltrack.repository")))
        private val files by lazy {
            root
                .walkTopDown()
                .onEnter {
                    it == root ||
                        (!it.name.startsWith('.') &&
                            it.name !in setOf("build", "target", "node_modules"))
                }
                .filter { it.isFile && it.extension in setOf("kt", "java", "xml") }
                .toList()
        }
        private val scope by lazy {
            Konsist.scopeFromFiles(
                files.filter { it.extension == "kt" }.map { it.relativeTo(root).path },
            )
        }
        private val javaSources by lazy {
            files.filter { it.extension == "java" }.associateWith { StaticJavaParser.parse(it) }
        }
        private val aliases by lazy { scope.typeAliases.associateBy { it.fullyQualifiedName } }

        private fun KoFileDeclaration.hasLegacyParent(name: String): Boolean =
            candidates(name.substringBefore('<')).any { candidate ->
                isLegacyType(candidate) ||
                    aliases[candidate]?.let { it.containingFile.hasLegacyParent(it.type.text) } ==
                        true
            }

        private fun KoFileDeclaration.candidates(name: String): List<String> =
            listOfNotNull(name, packagee?.let { "${it.name}.$name" }) +
                imports.mapNotNull { import ->
                    when {
                        import.alias != null -> import.name.takeIf { import.alias?.name == name }
                        import.isWildcard -> import.name.removeSuffix("*") + name
                        import.name.substringAfterLast('.') == name -> import.name
                        else -> null
                    }
                }

        private fun loadClass(name: String): Class<*>? =
            try {
                Class.forName(name, false, ComposeArchitectureTest::class.java.classLoader)
            } catch (_: ClassNotFoundException) {
                null
            }

        private fun isLegacyType(name: String): Boolean =
            name in legacy ||
                generateSequence(loadClass(name)) { it.superclass }.any { it.name in legacy }
    }
}

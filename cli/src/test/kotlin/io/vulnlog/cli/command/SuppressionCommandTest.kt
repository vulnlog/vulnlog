package io.vulnlog.cli.command

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.vulnlog.cli.output.OutputService
import io.vulnlog.cli.suppressions.SuppressionComposition
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.MockProvider
import org.koin.test.mock.declareMock

class SuppressionCommandTest :
    FunSpec(),
    KoinTest {
    override fun extensions() = listOf(KoinExtension(module { singleOf(::ServiceImpl) }))

    val service: Service by inject()
    val outputService: OutputService by inject(qualifier = named("file"))

    @JvmField
    @RegisterExtension
    val mockProvider: MockProviderExtension =
        MockProviderExtension.create { clazz ->
            mockkClass(clazz)
        }

    init {
        val vulnlog = tempfile()
        val template = tempfile()

        MockProvider.register { mockkClass(it) }

        test("test missing required template and file options") {

            val command = SuppressionCommand()

            val result = command.test("")

            result.statusCode shouldBe 1
            result.stdout shouldBe ""
            result.stderr shouldBe
                """
                Usage: supp [<options>]
                
                Error: missing option --templates
                Error: missing option --file
                
                """.trimIndent()
        }

        test("test missing required template option") {
            val command = SuppressionCommand()

            val result = command.test("--file test")

            result.statusCode shouldBe 1
            result.stdout shouldBe ""
            result.stderr shouldBe
                """
                Usage: supp [<options>]
                
                Error: missing option --templates
                
                """.trimIndent()
        }

        test("test missing required file option") {
            val command = SuppressionCommand()

            val result = command.test("--templates test")

            result.statusCode shouldBe 1
            result.stdout shouldBe ""
            result.stderr shouldBe
                """
                Usage: supp [<options>]
                
                Error: missing option --file
                
                """.trimIndent()
        }

        xtest("test with file and template option") {
            declareMock<Service> {
                every { service.action(any(), any()) }.returns(
                    listOf(
                        SuppressionComposition(
                            "",
                            "",
                            emptyList(),
                            emptyList(),
                            setOf(emptyList()),
                        ),
                    ),
                )
            }

            declareMock<OutputService> {
                every { outputService.write(any()) }.returns(Unit)
            }

            val command = SuppressionCommand()

            val result = command.test("--file ${vulnlog.path} --templates ${template.path}")

            result.statusCode shouldBe 0
        }
    }
}

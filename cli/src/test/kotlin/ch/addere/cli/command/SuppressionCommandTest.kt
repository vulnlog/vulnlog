package ch.addere.cli.command

import ch.addere.cli.suppressions.SuppressionComposition
import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.MockProvider
import org.koin.test.mock.declareMock

class SuppressionCommandTest : FunSpec(), KoinTest {
    override fun extensions() = listOf(KoinExtension(module { singleOf(::ServiceImpl) }))

    val mock: Service by inject()

    //    @JvmField
//    @RegisterExtension
//    val koinTestExtension =
//        KoinTestExtension.create {
//            modules(
//                module {
//                    singleOf(::ServiceImpl)
//                },
//            )
//        }
//
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

        test("test with file and template option") {
            declareMock<Service> {
                every { mock.action(any(), any()) }.returns(
                    SuppressionComposition(
                        emptyList(),
                        emptyList(),
                        setOf(emptyList()),
                    ),
                )
            }

            val command = SuppressionCommand()

            val result = command.test("--file ${vulnlog.path} --templates ${template.path}")

            result.statusCode shouldBe 0
        }
    }
}

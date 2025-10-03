package dev.vulnlog.dsl.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UtilsKtTest : FunSpec({

    test("empty string should return empty string") {
        "".toCamelCase() shouldBe ""
    }

    test("one word with all lowercase should not change") {
        "hello".toCamelCase() shouldBe "hello"
    }

    test("all capital word should change to camel case") {
        "HELLO".toCamelCase() shouldBe "hello"
    }

    test("two words with all lowercase should change to camel case") {
        "hello world".toCamelCase() shouldBe "helloWorld"
    }

    test("two all capital words should change to camel case") {
        "HELLO WORLD".toCamelCase() shouldBe "helloWorld"
    }

    test("two all capital words with punctuation should change to camel case") {
        "HELLO WORLD!".toCamelCase() shouldBe "helloWorld"
    }

    test("string with punctuation should change to camel case") {
        "Release Branch Version 1.0".toCamelCase() shouldBe "releaseBranchVersion10"
    }

    test("string with punctuation and no spaces should change to camel case") {
        "Release-Branch-Version-1_0".toCamelCase() shouldBe "releaseBranchVersion10"
    }
})

package io.vullog.rdslhost

// fun main() {
//    val (p1, p2, p3) = product("foo", "bar", "baz")
//    println(p1)
//    println(p2)
//    println(p3)
//
//    println()
//
//    val ap0: (String) -> AProduct = advancedProduct("foo")
//    println(ap0("bar"))
//
//    println()
//
//    val (ap1, ap2, ap3) = advancedProduct2("foo", "bar", "baz")
//    println(ap1("bla"))
//    println(ap2("bli"))
//    println(ap3("blup"))
//
//    val (ap4, ap5, ap6) = advancedProduct3("foo", "bar", "baz")
//    println(ap4("bla", 23))
//    println(ap5("bli", 42))
//    println(ap6("blup", 1337))
// }

fun product(vararg names: String): Array<Product> = names.map(::Product).toTypedArray()

fun advancedProduct(name: String): (String) -> AProduct = { realName -> AProduct(name, realName) }

fun advancedProduct2(vararg names: String): Array<(String) -> AProduct> =
    names.map { name -> { realName: String -> AProduct(name, realName) } }.toTypedArray()

fun advancedProduct3(vararg names: String): Array<(String, Int) -> AProduct> =
    names.map { name -> { realName: String, count: Int -> AProduct(name, realName, count) } }.toTypedArray()

data class Product(
    val name: String,
)

data class AProduct(
    val name: String,
    val realName: String = "default name",
    val count: Int = 0,
)

// reporterA("CVE-2024-2").filter("'*'").suppressUntilNextRelease(branch1)

data class Reporter(
    val name: String,
)

class SuppressionBuilder(
    val reporter: Reporter,
    val vulnId: String,
) {
    private var filter: String? = null
    private var branch: String? = null

    infix fun filter(filter: String): SuppressionBuilder {
        this.filter = filter
        return this
    }

    infix fun suppressUntilNetRelease(branch: String): SuppressionBuilder {
        this.branch = branch
        return this
    }

    override fun toString(): String {
        return "SuppressionBuilder(reporter=$reporter, vulnId='$vulnId', filter=$filter, branch=$branch)"
    }
}

fun createReporter(name: String): (String) -> SuppressionBuilder {
    return { vulnId: String -> SuppressionBuilder(Reporter(name), vulnId) }
}

fun createReporter(vararg names: String): Array<(String) -> SuppressionBuilder> =
    names
        .map { name: String -> { vulnId: String -> SuppressionBuilder(Reporter(name), vulnId) } }
        .toTypedArray()

class SuppressionBuilder2(
    val branch: String,
) {
    private var filter: String? = null

    infix fun filter(filter: String): SuppressionBuilder2 {
        this.filter = filter
        return this
    }

    override fun toString(): String = "SuppressionBuilder2(branch='$branch', filter=$filter)"
}

class ActionBlock {
    val actions = mutableListOf<SuppressionBuilder2>()

    fun suppressUntilNextReleaseIn(branch: String): SuppressionBuilder2 {
        val builder = SuppressionBuilder2(branch)
        actions.add(builder)
        return builder
    }
}

fun initActionBlock(block: ActionBlock.() -> Unit): ActionBlock {
    val actionBlock = ActionBlock()
    block.invoke(actionBlock)
    return actionBlock
}

fun main() {
    val rep0 = createReporter("demoReporter")
    val (rep1, rep2) = createReporter("demoReporter1", "demoReporter2")

    val sup0 = rep0("CVE-2024-2") filter "'*'" suppressUntilNetRelease "branch"
    val sup1 = rep1("CVE-2024-3") suppressUntilNetRelease "branch" filter "'*'"
    val sup2 = rep2("CVE-2024-4") suppressUntilNetRelease "branch"

    println(sup0)
    println(sup1)
    println(sup2)

    val actionBlock =
        initActionBlock {
            suppressUntilNextReleaseIn("branch") filter "'*'"
        }

    actionBlock.actions.forEach(::println)
}

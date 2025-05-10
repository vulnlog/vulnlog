package dev.vulnlog.dslinterpreter.repository

import dev.vulnlog.common.ReleaseBranchDataImpl
import dev.vulnlog.common.ReleaseVersionDataImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class BranchRepositoryImplTest : FunSpec({

    context("empty repository") {
        val branchRepository: BranchRepository = BranchRepositoryImpl()

        test("get all branches returns only the default branch") {
            branchRepository.getAllBranches() shouldBe setOf(DEFAULT_BRANCH)
        }

        test("get branches to release versions return only the default branch with no release versions") {
            branchRepository.getBranchesToReleaseVersions() shouldBe mapOf(DEFAULT_BRANCH to emptyList())
        }

        test("get release versions for any branch returns a failure") {
            val result = branchRepository.getReleaseVersions(ReleaseBranchDataImpl("any"))

            result shouldBeFailure BranchNotExisting("Release branch does not exist: any")
        }

        test("get next release version before for any branch returns a failure") {
            val result = branchRepository.getNextReleaseVersionBefore(ReleaseBranchDataImpl("any"), LocalDate.now())

            result shouldBeFailure BranchNotExisting("Release branch does not exist: any")
        }

        test("get next release version after for any branch returns a failure") {
            val result = branchRepository.getNextReleaseVersionAfter(ReleaseBranchDataImpl("any"), LocalDate.now())

            result shouldBeFailure BranchNotExisting("Release branch does not exist: any")
        }
    }

    context("fully populated repository") {
        val releaseBranchA = ReleaseBranchDataImpl("Release Branch A")
        val versionA1 = ReleaseVersionDataImpl("1", LocalDate.parse("2025-01-01"))
        val versionA2 = ReleaseVersionDataImpl("2", LocalDate.parse("2025-02-01"))
        val versionA3 = ReleaseVersionDataImpl("3", LocalDate.parse("2025-03-01"))
        val versionA4 = ReleaseVersionDataImpl("4", null)
        val versionA5 = ReleaseVersionDataImpl("5", null)
        val branchRepository: BranchRepository = BranchRepositoryImpl()
        branchRepository.add(
            releaseBranchA,
            listOf(versionA1, versionA2, versionA3, versionA4, versionA5),
        )

        test("get all branches returns the default branch and the explizit specified branch") {
            branchRepository.getAllBranches() shouldBe setOf(DEFAULT_BRANCH, releaseBranchA)
        }

        test("get branches to release versions return all branches and releases specified") {
            val expected = listOf(versionA1, versionA2, versionA3, versionA4, versionA5)

            val result = branchRepository.getBranchesToReleaseVersions()

            result shouldBe mapOf(DEFAULT_BRANCH to emptyList(), releaseBranchA to expected)
        }

        test("get release versions for the default branch returns empty") {
            val result = branchRepository.getReleaseVersions(DEFAULT_BRANCH)

            result shouldBeSuccess emptyList()
        }

        test("get release versions for the explicit branch returns empty") {
            val result = branchRepository.getReleaseVersions(releaseBranchA)

            result shouldBeSuccess listOf(versionA1, versionA2, versionA3, versionA4, versionA5)
        }

        test("get next release version before all returns a failure") {
            val result = branchRepository.getNextReleaseVersionBefore(releaseBranchA, LocalDate.parse("2024-01-01"))

            result shouldBeFailure ReleaseVersionNotExisting("No release version exist before: 2024-01-01")
        }

        test("get next release version before second entry returns first entry") {
            val result = branchRepository.getNextReleaseVersionBefore(releaseBranchA, LocalDate.parse("2025-01-20"))

            result shouldBeSuccess versionA1
        }

        test("get next release version before third entry returns second entry") {
            val result = branchRepository.getNextReleaseVersionBefore(releaseBranchA, LocalDate.parse("2025-02-20"))

            result shouldBeSuccess versionA2
        }

        test("get next release version before last entry returns second last entry") {
            val result = branchRepository.getNextReleaseVersionBefore(releaseBranchA, LocalDate.parse("2025-03-20"))

            result shouldBeSuccess versionA3
        }

        test("get next release version after all published returns next unpublished release") {
            val result = branchRepository.getNextReleaseVersionAfter(releaseBranchA, LocalDate.parse("2025-03-20"))

            result shouldBeSuccess versionA4
        }

        test("get next release version after second entry returns third entry") {
            val result = branchRepository.getNextReleaseVersionAfter(releaseBranchA, LocalDate.parse("2025-02-20"))

            result shouldBeSuccess versionA3
        }

        test("get next release version after first entry returns second entry") {
            val result = branchRepository.getNextReleaseVersionAfter(releaseBranchA, LocalDate.parse("2025-01-20"))

            result shouldBeSuccess versionA2
        }

        test("get next release version before all returns the first entry") {
            val result = branchRepository.getNextReleaseVersionAfter(releaseBranchA, LocalDate.parse("2024-01-01"))

            result shouldBeSuccess versionA1
        }

        test("get next release version before all returns the first entry") {
            val releaseBranchB = ReleaseBranchDataImpl("Release Branch B")
            val versionB1 = ReleaseVersionDataImpl("1", LocalDate.parse("2025-01-01"))
            val versionB2 = ReleaseVersionDataImpl("2", LocalDate.parse("2025-02-01"))
            val branchRepositoryB: BranchRepository = BranchRepositoryImpl()

            branchRepositoryB.add(releaseBranchB, listOf(versionB1, versionB2))
            val result = branchRepositoryB.getNextReleaseVersionAfter(releaseBranchB, LocalDate.parse("2025-02-20"))

            result shouldBeFailure ReleaseVersionNotExisting("No release version exist after: 2025-02-20")
        }

        test("get add release version to existent branch should merge") {
            val releaseBranchB = ReleaseBranchDataImpl("Release Branch B")
            val versionB1 = ReleaseVersionDataImpl("1", LocalDate.parse("2025-01-01"))
            val versionB2 = ReleaseVersionDataImpl("2", LocalDate.parse("2025-02-01"))
            val versionB3 = ReleaseVersionDataImpl("3", LocalDate.parse("2025-03-01"))
            val branchRepositoryB: BranchRepository = BranchRepositoryImpl()

            branchRepositoryB.add(releaseBranchB, listOf(versionB1, versionB2))
            branchRepositoryB.add(releaseBranchB, listOf(versionB3))
            val result = branchRepositoryB.getReleaseVersions(releaseBranchB)

            result shouldBeSuccess listOf(versionB1, versionB2, versionB3)
        }
    }
})

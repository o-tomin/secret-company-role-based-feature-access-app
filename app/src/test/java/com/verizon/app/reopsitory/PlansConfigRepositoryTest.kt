package com.verizon.app.reopsitory

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.verizon.app.api.ConfigApi
import com.verizon.app.di.NetworkModule
import com.verizon.app.di.PlansConfigSerializer
import com.verizon.app.model.AccessFlag
import com.verizon.app.model.Feature
import com.verizon.app.model.Plan
import com.verizon.app.model.PlanId
import com.verizon.app.model.PlansConfig
import com.verizon.app.model.Role
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class PlansConfigRepositoryTest {
    private lateinit var tmpDir: File
    private lateinit var storeFile: File
    private lateinit var store: DataStore<PlansConfig>
    private lateinit var repo: PlansConfigRepository

    private val testDispatcher = StandardTestDispatcher()
    private val ioDispatcher: CoroutineDispatcher = testDispatcher
    private val yamlMapper = NetworkModule.provideYamlMapper()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0

        tmpDir = Files.createTempDirectory("ds_").toFile()
        storeFile = File(tmpDir, "plans_config.json")
        store = DataStoreFactory.create(
            serializer = PlansConfigSerializer,
            produceFile = { storeFile }
        )
    }

    @After
    fun tearDown() {
        storeFile.delete()
        tmpDir.deleteRecursively()
    }

    // ---------- tests ----------

    @Test
    fun `fetchAndSet - saves network result to DataStore`() = runTest(testDispatcher) {
        val api = FakeYamlApi(loadYaml("plans_config.yml"))
        repo = PlansConfigRepository(api = api, store = store, ioDispatcher = ioDispatcher)

        val result = repo.fetchAndSet()

        assertThat(result).isEqualTo(EXPECTED_FROM_YAML_OK)
        assertThat(store.data.first()).isEqualTo(EXPECTED_FROM_YAML_OK)
    }

    @Test
    fun `fetchAndSet - returns cached when network fails`() = runTest(testDispatcher) {
        val cached = EXPECTED_FROM_YAML_OK.copy(version = 42)
        store.updateData { cached }

        val api = FailingApi()
        repo = PlansConfigRepository(api = api, store = store, ioDispatcher = ioDispatcher)

        val result = repo.fetchAndSet()
        assertThat(result).isEqualTo(cached)
        assertThat(store.data.first()).isEqualTo(cached)
    }

    @Test
    fun `fetchAndSet - when cache empty and network fails - returns DEFAULT_CONFIG`() =
        runTest(testDispatcher) {
            val api = FailingApi()
            repo = PlansConfigRepository(api = api, store = store, ioDispatcher = ioDispatcher)

            val result = repo.fetchAndSet()
            assertThat(result).isEqualTo(DEFAULT_CONFIG)
        }

    @Test
    fun `get - returns stored config`() = runTest(testDispatcher) {
        val saved = EXPECTED_FROM_YAML_OK.copy(version = 7)
        store.updateData { saved }

        val api = FailingApi()
        repo = PlansConfigRepository(api = api, store = store, ioDispatcher = ioDispatcher)

        val result = repo.get()
        assertThat(result).isEqualTo(saved)
    }

    @Test
    fun `set - overwrites existing config`() = runTest(testDispatcher) {
        val old = EXPECTED_FROM_YAML_OK.copy(version = 1)
        val new = EXPECTED_FROM_YAML_OK.copy(version = 2)
        store.updateData { old }

        val api = FailingApi()
        repo = PlansConfigRepository(api = api, store = store, ioDispatcher = ioDispatcher)

        repo.set(new)
        assertThat(store.data.first()).isEqualTo(new)
    }

    @Test
    fun `update - transforms and persists config`() = runTest(testDispatcher) {
        val original = EXPECTED_FROM_YAML_OK.copy(version = 3)
        store.updateData { original }

        val api = FailingApi()
        repo = PlansConfigRepository(api = api, store = store, ioDispatcher = ioDispatcher)

        // works for nullable or non-null version fields
        repo.update { it.copy(version = it.version + 1) }
        assertThat(store.data.first().version).isEqualTo(4)
    }

    private fun loadYaml(name: String): String =
        (javaClass.getResourceAsStream("/$name") ?: error("Resource not found: $name"))
            .bufferedReader().use { it.readText() }

    private inner class FakeYamlApi(private val yaml: String) : ConfigApi {
        override suspend fun fetchConfig(): PlansConfig =
            yamlMapper.readValue(yaml, PlansConfig::class.java)
    }

    private inner class FailingApi : ConfigApi {
        override suspend fun fetchConfig(): PlansConfig {
            throw RuntimeException("Network error")
        }
    }

    // ---------- constants ----------
    companion object {
        /** Non-empty default used by DataStore when there is no file or parse fails. */
        val DEFAULT_CONFIG = PlansConfig(
            version = 0,
            generatedAt = "",
            notes = listOf("Default fallback configuration (no remote data loaded)"),
            features = listOf(Feature.Calls), // minimal supported feature set
            plans = mapOf(
                PlanId.Free to Plan(setOf(Feature.Calls)),
                PlanId.Basic to Plan(setOf(Feature.Calls)),
                PlanId.Premium to Plan(setOf(Feature.Calls))
            ),
            roles = setOf(Role.Parent, Role.Child, Role.Member),
            access = mapOf(
                Role.Parent to emptyMap(),
                Role.Child to emptyMap(),
                Role.Member to emptyMap()
            )
        )

        /** Full object that the YAML should parse into for equality checks. */
        val EXPECTED_FROM_YAML_OK = PlansConfig(
            version = 1,
            generatedAt = "2025-10-04",
            notes = listOf(
                "R = allowed/accessible; N = not allowed.",
                "Matrix is explicit per acting_role → target_role → plan → feature.",
                "Features available under each plan are listed in `plans`.",
                "If a plan does not include a feature, it should remain N (even if an R is mistakenly set)."
            ),
            features = listOf(Feature.Calls, Feature.ScreenTime, Feature.Location),
            plans = mapOf(
                PlanId.Free to Plan(setOf(Feature.Calls)),
                PlanId.Basic to Plan(setOf(Feature.Calls, Feature.ScreenTime)),
                PlanId.Premium to Plan(setOf(Feature.Calls, Feature.ScreenTime, Feature.Location))
            ),
            roles = setOf(Role.Parent, Role.Child, Role.Member),
            access = mapOf(
                Role.Parent to mapOf(
                    Role.Self to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.R,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.R,
                            Feature.Location to AccessFlag.R
                        )
                    ),
                    Role.Parent to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    ),
                    Role.Child to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.R,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.R,
                            Feature.Location to AccessFlag.R
                        )
                    ),
                    Role.Member to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    )
                ),
                Role.Child to mapOf(
                    Role.Self to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.R,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.R,
                            Feature.Location to AccessFlag.R
                        )
                    ),
                    Role.Parent to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    ),
                    Role.Child to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    ),
                    Role.Member to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    )
                ),
                Role.Member to mapOf(
                    Role.Self to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.R,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    ),
                    Role.Parent to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    ),
                    Role.Child to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    ),
                    Role.Member to mapOf(
                        PlanId.Free to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Basic to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        ),
                        PlanId.Premium to mapOf(
                            Feature.Calls to AccessFlag.N,
                            Feature.ScreenTime to AccessFlag.N,
                            Feature.Location to AccessFlag.N
                        )
                    )
                )
            )
        )
    }
}
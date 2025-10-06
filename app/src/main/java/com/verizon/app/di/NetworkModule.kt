package com.verizon.app.di

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.verizon.app.api.ConfigApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for the **base URL** used to fetch the remote configuration.
 *
 * Apply this to a `String` dependency when you need the configuration host URL.
 *
 * Example:
 * ```kotlin
 * class Repo @Inject constructor(
 *   @ConfigBaseUrl private val baseUrl: String
 * )
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConfigBaseUrl

/**
 * Network stack for remote configuration:
 * - Provides the **base URL** for the config host.
 * - Configures **OkHttp** with a logging interceptor and sane timeouts.
 * - Configures a **Jackson YAML mapper** for Retrofit.
 * - Builds a **Retrofit** instance and exposes a typed **ConfigApi**.
 *
 * Installed in [SingletonComponent], so each provided dependency is a process-wide singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Base URL for hosting `plans_matrix.yml`.
     *
     * It is normalized to include a trailing slash when used by Retrofit (see [provideRetrofit]).
     * Replace this with your own host if needed (e.g., GitHub Pages, S3, CDN).
     */
    @Provides
    @Singleton
    @ConfigBaseUrl
    fun provideConfigBaseUrl(): String =
        "https://o-tomin.github.io/verizon-role-based-access-config"

    /**
     * Configures an OkHttp logging interceptor.
     *
     * **Security note:** `BODY` logs request/response payloads and should be avoided in production
     * builds if the payload can contain sensitive data. Consider downgrading to `BASIC` or `NONE`
     * for release variants.
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    /**
     * Provides a shared [OkHttpClient] with logging + timeouts.
     *
     * @param loggingInterceptor Interceptor configured by [provideHttpLoggingInterceptor].
     * @return A singleton OkHttp client.
     */
    @Provides
    @Singleton
    fun provideOkHttp(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

    /**
     * Provides a Jackson **YAML** mapper used by Retrofit converters.
     *
     * Configuration details:
     * - `SNAKE_CASE` → maps YAML `generated_at` to Kotlin `generatedAt`.
     * - `ACCEPT_CASE_INSENSITIVE_ENUMS` → allows `free`/`Free`/`FREE` for enums.
     * - `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE` → forwards-compat for new enum values.
     * - `FAIL_ON_UNKNOWN_PROPERTIES=false` → ignores extra fields added in future versions.
     * - Kotlin module → proper Kotlin data & nullability handling.
     */
    @Provides
    @Singleton
    fun provideYamlMapper(): ObjectMapper =
        YAMLMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(KotlinModule.Builder().build())
            .build()

    /**
     * Provides a Retrofit instance for the config service.
     *
     * - Ensures the base URL has a **trailing slash** (required by Retrofit).
     * - Uses the shared OkHttp client and **Jackson YAML** converter.
     *
     * @param baseUrl Qualified base URL (see [ConfigBaseUrl]).
     * @param client Shared OkHttp client.
     * @param yamlMapper Jackson YAML mapper from [provideYamlMapper].
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        @ConfigBaseUrl baseUrl: String,
        client: OkHttpClient,
        yamlMapper: ObjectMapper
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(yamlMapper))
            .build()

    /**
     * Typed Retrofit API for fetching the remote plans matrix.
     *
     * @see ConfigApi for endpoint details and expected model.
     */
    @Provides
    @Singleton
    fun provideConfigApi(retrofit: Retrofit): ConfigApi =
        retrofit.create(ConfigApi::class.java)
}

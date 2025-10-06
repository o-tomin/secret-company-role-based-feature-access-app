package com.otomin.app.api

import com.otomin.app.model.PlansConfig
import retrofit2.http.GET

/**
 * Retrofit API definition for fetching the remote configuration (plans matrix).
 *
 * ## Purpose
 * This interface defines a single endpoint that retrieves the YAML configuration file
 * (`plans_matrix.yml`) from a remote source such as GitHub Pages, S3, or another HTTP server.
 *
 * The configuration describes:
 * - Available **plans** (Free, Basic, Premium)
 * - Associated **features** (Calls, ScreenTime, Location)
 * - Supported **roles** (Parent, Child, Member, Self)
 * - A full **access matrix** defining which acting role can access which target role’s features
 *   across plan types.
 *
 * The returned YAML is parsed into a [PlansConfig] model by a custom Retrofit converter
 * (typically using Jackson’s `YAMLMapper`).
 *
 * ## Example
 * ```kotlin
 * val api: ConfigApi = retrofit.create(ConfigApi::class.java)
 * val config: PlansConfig = api.fetchConfig()
 * ```
 *
 * ## Expected YAML file format
 * The remote file must be named **plans_matrix.yml** and structured like:
 * ```yaml
 * version: 1
 * generated_at: 2025-10-04
 * features:
 *   - Calls
 *   - ScreenTime
 *   - Location
 * plans:
 *   Free: { features: [Calls] }
 *   Basic: { features: [Calls, ScreenTime] }
 *   Premium: { features: [Calls, ScreenTime, Location] }
 * roles: [Parent, Child, Member, Self]
 * access:
 *   Parent:
 *     self:
 *       Free: { Calls: R, ScreenTime: N, Location: N }
 * ```
 *
 * @see PlansConfig for the full data model representation.
 */
interface ConfigApi {
    @GET("plans_matrix.yml")
    suspend fun fetchConfig(): PlansConfig
}

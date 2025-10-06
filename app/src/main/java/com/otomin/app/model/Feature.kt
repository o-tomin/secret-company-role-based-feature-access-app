package com.otomin.app.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.Serializable

/**
 * Represents a functional capability or app feature that can be
 * enabled or restricted based on the user's plan and role.
 *
 * Each [Feature] corresponds directly to a key in the remote YAML
 * configuration file (`plans_matrix.yml`) under the `features` section.
 *
 * ## Enum values
 * | Value | Description | Typical use case |
 * |--------|--------------|------------------|
 * | **Calls** | Voice or in-app calling feature. | Allows users to initiate or receive calls. |
 * | **ScreenTime** | Screen time monitoring feature. | Allows parents to monitor and control device usage. |
 * | **Location** | Location tracking feature. | Enables viewing or reporting of device location. |
 * | **Unknown** | Fallback value for unrecognized features. | Used if YAML contains a new/unsupported feature name. |
 *
 * ## Serialization
 * - Annotated with [@Serializable] for **Kotlinx DataStore** persistence.
 * - Annotated with Jackson’s [@JsonValue] and [@JsonEnumDefaultValue] for **YAML parsing**.
 * - Uses a stable `wireName` (string key) for reading/writing configuration data.
 *
 * ## Example YAML
 * ```yaml
 * features:
 *   - Calls
 *   - ScreenTime
 *   - Location
 *
 * plans:
 *   Free: { features: [Calls] }
 *   Premium: { features: [Calls, ScreenTime, Location] }
 * ```
 *
 * Example deserialization:
 * ```kotlin
 * val f = Feature.Calls
 * println(f.wireName) // "Calls"
 * ```
 */
@Serializable
enum class Feature(

    /** The YAML/JSON field name used when serializing or deserializing this enum. */
    @get:JsonValue
    val wireName: String
) {
    /** Voice or in-app calling capability. */
    Calls("Calls"),

    /** Screen time monitoring and control functionality. */
    ScreenTime("ScreenTime"),

    /** Device or user location tracking capability. */
    Location("Location"),

    /**
     * Default fallback used when the feature name in YAML is unrecognized.
     *
     * Ensures forward compatibility when new features are introduced in future
     * configuration versions without breaking deserialization.
     */
    @JsonEnumDefaultValue
    Unknown("Unknown")
}
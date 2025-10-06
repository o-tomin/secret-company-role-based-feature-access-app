package com.verizon.app.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.Serializable

/**
 * Identifies a specific **plan tier** in the app’s subscription or access system.
 *
 * Each [PlanId] represents a unique service level, which determines
 * the set of available [Feature]s for users under that plan.
 *
 * These values directly correspond to the `plans:` section of the
 * remote YAML configuration file (`plans_matrix.yml`).
 *
 * ## Enum values
 * | Value | Description | Typical features |
 * |--------|--------------|------------------|
 * | **Free** | Entry-level plan with minimal features. | Calls only. |
 * | **Basic** | Mid-tier plan with standard access. | Calls, ScreenTime. |
 * | **Premium** | Highest-tier plan with all available features. | Calls, ScreenTime, Location. |
 * | **Unknown** | Fallback for unrecognized plan IDs in YAML. | Used to prevent deserialization errors. |
 *
 * ## Serialization
 * - Annotated with [@Serializable] for Kotlinx DataStore persistence.
 * - Annotated with Jackson’s [@JsonValue] and [@JsonEnumDefaultValue] for YAML parsing.
 * - Uses [wireName] to control how each value is written to and read from configuration files.
 * - Gracefully handles unknown plan values from newer configurations using [Unknown].
 *
 * ## Example YAML
 * ```yaml
 * plans:
 *   Free:
 *     features: [Calls]
 *   Basic:
 *     features: [Calls, ScreenTime]
 *   Premium:
 *     features: [Calls, ScreenTime, Location]
 * ```
 *
 * Example usage:
 * ```kotlin
 * val plan = PlanId.Basic
 * println(plan.wireName) // "Basic"
 * ```
 *
 * @see Plan for the features associated with each [PlanId].
 */
@Serializable
enum class PlanId(

    /** The string key used in YAML or JSON to represent this plan. */
    @get:JsonValue
    val wireName: String
) {
    /** Entry-level plan: includes essential communication features (e.g., Calls). */
    Free("Free"),

    /** Mid-tier plan: adds productivity and monitoring features (e.g., ScreenTime). */
    Basic("Basic"),

    /** Premium plan: unlocks all available features including location tracking. */
    Premium("Premium"),

    /**
     * Fallback plan type used when the YAML input contains an unrecognized or unsupported plan name.
     *
     * This ensures forward compatibility with future plan additions
     * without breaking deserialization.
     */
    @JsonEnumDefaultValue
    Unknown("Unknown")
}

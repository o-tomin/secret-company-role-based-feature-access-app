package com.verizon.app.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.Serializable

/**
 * Represents access permissions between user roles in the feature/plan matrix.
 *
 * Each [AccessFlag] encodes whether a specific **acting role**
 * is allowed to access a **target role’s** feature within a given plan.
 *
 * The flag values directly correspond to the YAML matrix values (`R` / `N`)
 * in the remote configuration file (`plans_matrix.yml`).
 *
 * ## Enum values
 * | Value | Meaning | Example usage |
 * |--------|----------|----------------|
 * | **R** | "Readable" / "Allowed" — The acting role has access to the target feature. | `Parent → Child → Calls = R` |
 * | **N** | "Not allowed" — Access is explicitly denied. | `Member → Parent → Calls = N` |
 * | **N_Default** | Fallback value when YAML input is missing or invalid. Treated as `N`. | `Unknown → N_Default` |
 *
 * ## Serialization
 * - Annotated with both [@Serializable] (for Kotlinx DataStore persistence) and Jackson annotations
 *   (for YAML deserialization from the remote configuration).
 * - The `wireName` property defines the exact string value written to or read from the YAML/JSON.
 * - [@JsonEnumDefaultValue] ensures that any unrecognized enum value in YAML (e.g., `"X"`)
 *   defaults safely to [N_Default].
 *
 * ## Example
 * ```yaml
 * access:
 *   Parent:
 *     Child:
 *       Free: { Calls: R, ScreenTime: N, Location: N }
 * ```
 *
 * In this case:
 * ```kotlin
 * AccessFlag.R  // allowed
 * AccessFlag.N  // denied
 * ```
 */
@Serializable
enum class AccessFlag(

    /** The string value used in serialized YAML or JSON (`"R"` or `"N"`). */
    @get:JsonValue
    val wireName: String
) {
    /** Access allowed / readable. */
    R("R"),

    /** Access denied. */
    N("N"),

    /**
     * Default fallback used when the YAML value is unrecognized or missing.
     *
     * Example: if a future configuration introduces `"X"` as a new flag,
     * this value ensures deserialization does not fail.
     */
    @JsonEnumDefaultValue
    N_Default("N")
}

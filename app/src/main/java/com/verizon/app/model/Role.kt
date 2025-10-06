package com.verizon.app.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.Serializable

/**
 * Represents a user’s **role** in the access-control hierarchy
 * that governs feature visibility and permissions within the app.
 *
 * Each [Role] identifies either:
 * - an **acting user** (the one performing an action, e.g. viewing another profile), or
 * - a **target user** (the one being acted upon, e.g. the viewed profile).
 *
 * Together, acting and target roles determine which [Feature]s are available
 * under a given [PlanId], as defined by the **access matrix** in [PlansConfig].
 *
 * ## Enum values
 * | Role | Description | Typical use |
 * |-------|--------------|--------------|
 * | **Parent** | Represents a parent user. | Can view or control Child features (e.g., Calls, ScreenTime). |
 * | **Child** | Represents a child user. | Can access their own limited features, often restricted. |
 * | **Member** | Represents a general or shared account. | Minimal permissions, often self-only. |
 * | **Self** | Special meta-role used when the acting and target user are the same. | Used for “own profile” access rules. |
 * | **Unknown** | Fallback when an unrecognized or unsupported role is encountered. | Used for forward compatibility. |
 *
 * ## Serialization
 * - Annotated with [@Serializable] for Kotlinx persistence (DataStore).
 * - Annotated with Jackson’s [@JsonValue] and [@JsonEnumDefaultValue] for YAML parsing.
 * - The lowercase `"self"` matches the remote YAML format, which differentiates it
 *   from other capitalized role names.
 *
 * ## Example YAML
 * ```yaml
 * roles:
 *   - Parent
 *   - Child
 *   - Member
 *   - self
 *
 * access:
 *   Parent:
 *     self:
 *       Free:    { Calls: R, ScreenTime: N, Location: N }
 *     Child:
 *       Premium: { Calls: R, ScreenTime: R, Location: R }
 * ```
 *
 * ## Example usage
 * ```kotlin
 * val acting = Role.Parent
 * val target = Role.Child
 * val plan = PlanId.Premium
 *
 * val allowed = config.access[acting]?.get(target)?.get(plan)
 * println(allowed?.get(Feature.Calls)) // R or N
 * ```
 *
 * @property wireName The exact string identifier used in YAML or JSON representations.
 */
@Serializable
enum class Role(

    /** The YAML/JSON key name associated with this role. */
    @get:JsonValue
    val wireName: String
) {
    /** A parent user — typically has full or supervisory access to child features. */
    Parent("Parent"),

    /** A child user — typically has restricted access to certain features. */
    Child("Child"),

    /** A general or shared member account — usually self-contained with minimal privileges. */
    Member("Member"),

    /**
     * Special meta-role used when the acting and target users are the same entity.
     *
     * In YAML, this is written in lowercase (`self`) to distinguish it from standard roles.
     * Example:
     * ```yaml
     * Parent:
     *   self:
     *     Free: { Calls: R, ScreenTime: N, Location: N }
     * ```
     */
    Self("self"),

    /**
     * Fallback for unrecognized or unsupported roles encountered during deserialization.
     * Ensures compatibility with newer configurations that might introduce additional roles.
     */
    @JsonEnumDefaultValue
    Unknown("Unknown")
}

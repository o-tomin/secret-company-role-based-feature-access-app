package com.otomin.app.model

import kotlinx.serialization.Serializable

/**
 * Represents a subscription or service tier that groups together
 * a defined set of [Feature]s available to users.
 *
 * Each [Plan] corresponds to one of the app’s pricing tiers
 * (e.g., **Free**, **Basic**, **Premium**) as defined in the remote YAML
 * configuration file (`plans_matrix.yml`).
 *
 * ## Structure
 * A plan acts as a **container of features** that determine which functionalities
 * are accessible to users under that plan type.
 *
 * Example YAML snippet:
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
 * In this example:
 * - `Free` → users can only access the **Calls** feature.
 * - `Basic` → users can access **Calls** and **ScreenTime**.
 * - `Premium` → all features are unlocked.
 *
 * ## Serialization
 * - Annotated with [@Serializable] for Kotlinx serialization support.
 * - Used both for local persistence via **DataStore** and remote configuration parsing.
 * - The default constructor defines an empty feature set to ensure the model
 *   remains valid even if the YAML source omits a plan or its features.
 *
 * ## Example usage
 * ```kotlin
 * val basicPlan = Plan(setOf(Feature.Calls, Feature.ScreenTime))
 * if (Feature.Location in basicPlan.features) {
 *     println("Location available")
 * } else {
 *     println("Location not included in Basic plan")
 * }
 * ```
 *
 * @property features The set of [Feature]s included in this plan.
 */
@Serializable
data class Plan(
    /** The list of features enabled under this plan. */
    val features: Set<Feature> = emptySet()
)

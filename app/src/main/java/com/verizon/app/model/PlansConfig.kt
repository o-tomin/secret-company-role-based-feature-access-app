package com.verizon.app.model

import kotlinx.serialization.Serializable

/**
 * Root configuration model describing **plans**, **features**, **roles**, and a full
 * **access matrix** that governs which acting role can access which target role’s features
 * under a given plan.
 *
 * This is the in-memory representation of the remote YAML file (`plans_matrix.yml`) and is also
 * used for local persistence via **Jetpack DataStore**.
 *
 * ## Structure
 * - [features]: the universe of features supported by the app.
 * - [plans]: the mapping of plan identifiers to the features they include.
 * - [roles]: the set of roles recognized by the system (including [Role.Self]).
 * - [access]: a 4-dimensional mapping: **actingRole → targetRole → plan → feature → R/N**.
 *
 * ## Invariants & semantics
 * - The **plan gating rule**: even if [access] says `R`, a feature **must also be included in the plan**
 *   ([plans]) to be effectively available. UIs should typically show features that are in the plan,
 *   then mark them as `R`/`N` per [access].
 * - Unknown enum values from future YAML revisions should be handled gracefully by your mapper
 *   (e.g., Jackson with `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE`) and default to safe values
 *   like [AccessFlag.N_Default], [PlanId.Unknown], etc.
 * - The [roles] set should include all roles referenced in [access] keys.
 *
 * ## Example YAML (excerpt)
 * ```yaml
 * version: 1
 * generated_at: 2025-10-04
 *
 * features:
 *   - Calls
 *   - ScreenTime
 *   - Location
 *
 * plans:
 *   Free:    { features: [Calls] }
 *   Basic:   { features: [Calls, ScreenTime] }
 *   Premium: { features: [Calls, ScreenTime, Location] }
 *
 * roles: [Parent, Child, Member, Self]
 *
 * access:
 *   Parent:
 *     self:
 *       Free:    { Calls: R, ScreenTime: N, Location: N }
 *       Basic:   { Calls: R, ScreenTime: R, Location: N }
 *       Premium: { Calls: R, ScreenTime: R, Location: R }
 *     Child:
 *       Premium: { Calls: R, ScreenTime: R, Location: R }
 *   Member:
 *     self:
 *       Free:    { Calls: R, ScreenTime: N, Location: N }
 * ```
 *
 * ## Typical usage
 * - Fetch remotely (Retrofit + Jackson YAML), persist via DataStore (kotlinx.serialization).
 * - At runtime, resolve allowed features for a selection `(actingRole, targetRole, plan)` by:
 *   1) taking the feature set from [plans][PlanId].features
 *   2) intersecting with flags in [access][acting][target][plan] (treat missing as `N`)
 *
 * @property version Configuration version number (useful for migrations/visibility in logs).
 * @property generatedAt ISO-like string indicating when the config was produced (for diagnostics).
 * @property notes Free-form notes shipped with the config (displayable in debug screens).
 * @property features Canonical list of all [Feature]s the configuration may reference.
 * @property plans Map from [PlanId] to [Plan], declaring which features each plan includes.
 * @property roles Set of supported [Role]s (e.g., Parent, Child, Member, Self).
 * @property access The rules matrix: **actingRole → targetRole → plan → feature → [AccessFlag]**.
 */
@Serializable
data class PlansConfig(
    val version: Int,
    val generatedAt: String,
    val notes: List<String>,
    val features: List<Feature>,
    val plans: Map<PlanId, Plan>,
    val roles: Set<Role>,
    val access: Map<Role, Map<Role, Map<PlanId, Map<Feature, AccessFlag>>>>
)
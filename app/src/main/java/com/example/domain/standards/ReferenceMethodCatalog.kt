package com.example.domain.standards

import com.example.domain.model.CableConstruction
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.ReferenceMethodSpec

object ReferenceMethodCatalog {

    val allMethods: List<ReferenceMethodSpec> = listOf(
        ReferenceMethodSpec(
            code = ReferenceMethodCode.A1,
            title = "Method A1 — Insulated Conductors in Conduit in Insulated Wall",
            shortSummary = "Single-core insulated conductors inside conduit embedded in a thermally insulated wall.",
            detailedDescription = "Conduit (metallic or non-metallic) installed within a thermally insulated stud or cavity wall where the inner wall face has a thermal conductance ≥ 10 W/(m²·K) and the outer face is insulated.",
            applicableConstructions = listOf(
                CableConstruction.SINGLE_CORE_NON_ARMOURED,
                CableConstruction.SOLAR_DC_DOUBLE_INSULATED
            ),
            conductorArrangements = listOf(
                "2 single-core conductors (1-Ph / DC) in single conduit",
                "3 or 4 single-core conductors (3-Ph) in single conduit"
            ),
            limitations = listOf(
                "Not applicable to multicore SWA or heavy armoured cables in standard wall conduit.",
                "If cable passes through ≥ 50 mm of continuous thermal insulation without wall contact, additional Ci derating applies."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method A1 (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.A2,
            title = "Method A2 — Multicore Cable in Conduit in Insulated Wall",
            shortSummary = "Multicore cable inside conduit or direct within a thermally insulated wall.",
            detailedDescription = "Sheathed multicore cable enclosed in conduit or directly embedded in a thermally insulated wall construction where heat dissipation is restricted by surrounding insulation.",
            applicableConstructions = listOf(
                CableConstruction.MULTICORE_NON_ARMOURED,
                CableConstruction.MULTICORE_ARMOURED_SWA
            ),
            conductorArrangements = listOf(
                "2-core multicore cable (1-Ph / DC)",
                "3-core, 4-core, or 5-core multicore cable (3-Ph)"
            ),
            limitations = listOf(
                "Lower ampacity than Method B2/C due to thermal insulation enclosure.",
                "Verify conduit fill factor and minimum bending radius for armoured multicore cables."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method A2 (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.B1,
            title = "Method B1 — Single-Core Conductors in Conduit / Trunking on Wall",
            shortSummary = "Insulated single-core conductors in surface-mounted or masonry-embedded conduit/trunking.",
            detailedDescription = "Single-core non-sheathed or sheathed conductors installed inside non-perforated conduit or cable trunking mounted on a wooden/masonry wall or embedded in masonry/concrete.",
            applicableConstructions = listOf(
                CableConstruction.SINGLE_CORE_NON_ARMOURED,
                CableConstruction.SOLAR_DC_DOUBLE_INSULATED
            ),
            conductorArrangements = listOf(
                "2 or 3/4 single-core conductors enclosed in circular conduit or rectangular trunking"
            ),
            limitations = listOf(
                "Applies to single-core conductors; for multicore cables in the same conduit/trunking use Method B2.",
                "Grouping correction factor Cg applies when multiple circuits share the same conduit or trunking."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method B1 (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.B2,
            title = "Method B2 — Multicore Cable in Conduit / Trunking on Wall",
            shortSummary = "Multicore cable in surface-mounted conduit, ducting, or cable trunking.",
            detailedDescription = "Multicore sheathed or armoured cable installed in conduit or non-perforated trunking fixed to a wall, ceiling, or embedded in plaster/masonry.",
            applicableConstructions = listOf(
                CableConstruction.MULTICORE_NON_ARMOURED,
                CableConstruction.MULTICORE_ARMOURED_SWA
            ),
            conductorArrangements = listOf(
                "Single multicore cable in conduit or trunking",
                "Multiple multicore cables grouped in trunking (apply Cg)"
            ),
            limitations = listOf(
                "Not used for spaced single-core free-air installations.",
                "Check space factor in trunking/conduit during mechanical design."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method B2 (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.C,
            title = "Method C — Clipped Direct to Non-Metallic Surface",
            shortSummary = "Cable clipped directly to wall, ceiling, masonry, or unperforated cable tray.",
            detailedDescription = "Sheathed multicore or armoured cables fixed directly to a wall, ceiling, or timber/masonry surface using cable clips/cleats, or resting on an unperforated tray with free top air convection.",
            applicableConstructions = listOf(
                CableConstruction.MULTICORE_NON_ARMOURED,
                CableConstruction.MULTICORE_ARMOURED_SWA,
                CableConstruction.SINGLE_CORE_ARMOURED_AWA,
                CableConstruction.SOLAR_DC_DOUBLE_INSULATED
            ),
            conductorArrangements = listOf(
                "Multicore cable clipped direct (flat/vertical)",
                "Single-core armoured cables touching on wall"
            ),
            limitations = listOf(
                "Non-armoured single-core insulated-only conductors (without sheath) must not be clipped direct without enclosure.",
                "Ensure minimum spacing ≥ 0.3×De if avoiding touching grouping derating."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method C (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.D1,
            title = "Method D1 — Multicore / Single-Core Cable in Underground Ducts",
            shortSummary = "Cables drawn into buried pipes, ducts, or underground conduits.",
            detailedDescription = "Power cables installed inside buried PVC/HDPE/clay underground ducts below ground level. Heat dissipation depends on ground ambient temperature, soil thermal resistivity, and burial depth.",
            applicableConstructions = listOf(
                CableConstruction.MULTICORE_ARMOURED_SWA,
                CableConstruction.MULTICORE_NON_ARMOURED,
                CableConstruction.SINGLE_CORE_ARMOURED_AWA,
                CableConstruction.SOLAR_DC_DOUBLE_INSULATED
            ),
            conductorArrangements = listOf(
                "Multicore cable in single underground duct",
                "Single-core cables in separate or shared underground ducts"
            ),
            limitations = listOf(
                "Requires ground temperature, burial depth (Cd), and soil thermal resistivity (Cs) factors.",
                "Duct thermal resistance lowers current capacity compared to direct burial (D2)."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method D1 (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.D2,
            title = "Method D2 — Cable Buried Direct in Ground",
            shortSummary = "Armoured cable buried directly in soil or sand bedding with warning tape/tiles.",
            detailedDescription = "Armoured cables (SWA/AWA) laid directly in an excavated trench surrounded by sifted sand or soil of defined thermal resistivity, protected by cover tiles and warning tape.",
            applicableConstructions = listOf(
                CableConstruction.MULTICORE_ARMOURED_SWA,
                CableConstruction.SINGLE_CORE_ARMOURED_AWA
            ),
            conductorArrangements = listOf(
                "Buried direct flat or trefoil formation"
            ),
            limitations = listOf(
                "Unarmoured non-sheathed single cores are prohibited for direct burial.",
                "Must apply ground temperature, depth of laying, soil resistivity, and trench grouping factors."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method D2 (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.E,
            title = "Method E — Multicore Cable in Free Air / Perforated Tray",
            shortSummary = "Multicore cable on perforated tray, cable ladder, cleats, or suspended in free air.",
            detailedDescription = "Multicore cables supported in free air where air circulates freely around the cable (clearance from wall ≥ 0.3×De), such as perforated cable trays, cable ladders, or wire mesh baskets.",
            applicableConstructions = listOf(
                CableConstruction.MULTICORE_ARMOURED_SWA,
                CableConstruction.MULTICORE_NON_ARMOURED,
                CableConstruction.SOLAR_DC_DOUBLE_INSULATED
            ),
            conductorArrangements = listOf(
                "Multicore cables on perforated tray (horizontal or vertical)",
                "Multicore cables on cable ladder / cleats"
            ),
            limitations = listOf(
                "Applies to multicore cables only; for single-core cables in free air use Method F (touching) or Method G (spaced).",
                "Minimum clearance from adjacent wall must be maintained for full free-air dissipation."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method E (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.F,
            title = "Method F — Single-Core Cables in Free Air / Tray (Touching)",
            shortSummary = "Single-core cables in trefoil or touching flat formation in free air or on tray/ladder.",
            detailedDescription = "Single-core sheathed or armoured cables arranged touching in flat horizontal, flat vertical, or trefoil formation on perforated trays, ladders, or cleats with free air circulation.",
            applicableConstructions = listOf(
                CableConstruction.SINGLE_CORE_NON_ARMOURED,
                CableConstruction.SINGLE_CORE_ARMOURED_AWA,
                CableConstruction.SOLAR_DC_DOUBLE_INSULATED
            ),
            conductorArrangements = listOf(
                "2 single-core cables touching (1-Ph / DC)",
                "3 single-core cables in trefoil or touching flat (3-Ph)"
            ),
            limitations = listOf(
                "Applies to single-core cables only; multicore cables use Method E.",
                "For 3-phase AC single-core circuits, use non-magnetic glands/armour (AWA) to avoid eddy-current heating."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method F (Verify against authorized edition)"
        ),
        ReferenceMethodSpec(
            code = ReferenceMethodCode.G,
            title = "Method G — Single-Core Cables in Free Air (Spaced ≥ 1 De)",
            shortSummary = "Single-core cables spaced by at least one cable outer diameter in free air.",
            detailedDescription = "Single-core cables mounted on cleats or brackets in horizontal or vertical formation with center-to-center spacing of at least 2×De (clear gap ≥ 1×De) allowing maximum convective cooling.",
            applicableConstructions = listOf(
                CableConstruction.SINGLE_CORE_NON_ARMOURED,
                CableConstruction.SINGLE_CORE_ARMOURED_AWA,
                CableConstruction.SOLAR_DC_DOUBLE_INSULATED
            ),
            conductorArrangements = listOf(
                "Single-core cables spaced horizontally or vertically (clearance ≥ 1×De)"
            ),
            limitations = listOf(
                "Higher inductive reactance in AC circuits due to wider phase spacing — verify reactance voltage drop.",
                "Applies to single-core cables only."
            ),
            standardReference = "BS 7671 Appendix 4 — Reference Method G (Verify against authorized edition)"
        )
    )

    fun getSpec(code: ReferenceMethodCode): ReferenceMethodSpec =
        allMethods.first { it.code == code }

    fun isCompatible(code: ReferenceMethodCode, construction: CableConstruction): Boolean =
        getSpec(code).applicableConstructions.contains(construction)
}

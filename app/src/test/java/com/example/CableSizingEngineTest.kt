package com.example

import com.example.domain.engine.CableSizingEngine
import com.example.domain.engine.DesignCurrentCalculator
import com.example.domain.engine.EngineeringQaRunner
import com.example.domain.model.AcPhaseConfiguration
import com.example.domain.model.CableConstruction
import com.example.domain.model.CableSpecInput
import com.example.domain.model.CandidateDecision
import com.example.domain.model.CheckStatus
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.ElectricalSystemInput
import com.example.domain.model.InsulationType
import com.example.domain.model.LoadInputMode
import com.example.domain.model.ProjectMetadata
import com.example.domain.model.ProtectionAndLimitsInput
import com.example.domain.model.ProtectiveDeviceType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.RouteSegment
import com.example.domain.model.SegmentConditions
import com.example.domain.model.SizingMode
import com.example.domain.standards.GenericStandardsDataset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CableSizingEngineTest {

    private val genericCatalog = GenericStandardsDataset.createGenericBaselineCatalog()

    @Test
    fun `qa suite all reference tests pass`() {
        val results = EngineeringQaRunner.runAll(genericCatalog)
        assertTrue(results.isNotEmpty())
        results.forEach { res ->
            assertTrue("QA test ${res.testId} failed: ${res.expectedResult} vs ${res.actualResult}", res.passed)
        }
    }

    @Test
    fun `three phase active power design current matches hand calculation`() {
        val out = DesignCurrentCalculator.calculate(
            ElectricalSystemInput(
                systemType = CurrentSystemType.AC,
                acPhaseConfig = AcPhaseConfiguration.THREE_PHASE_3W_OR_4W,
                nominalVoltageV = 400.0,
                loadInputMode = LoadInputMode.ACTIVE_POWER_KW,
                activePowerKw = 28.0,
                powerFactor = 0.85,
                efficiencyPct = 100.0
            )
        )
        assertNotNull(out.ibAmperes)
        assertEquals(47.55, out.ibAmperes!!, 0.05)
    }

    @Test
    fun `automatic sizing selects smallest passing cable and explains rejections`() {
        val project = CircuitDesignProject(
            metadata = ProjectMetadata(id = "t1", projectName = "Test Auto"),
            system = ElectricalSystemInput(
                systemType = CurrentSystemType.AC,
                acPhaseConfig = AcPhaseConfiguration.THREE_PHASE_3W_OR_4W,
                nominalVoltageV = 400.0,
                loadInputMode = LoadInputMode.DIRECT_CURRENT_IB,
                directDesignCurrentA = 45.0,
                powerFactor = 0.85
            ),
            cable = CableSpecInput(
                sizingMode = SizingMode.AUTOMATIC,
                conductorMaterial = ConductorMaterial.COPPER,
                insulationType = InsulationType.XLPE_90C,
                cableConstruction = CableConstruction.MULTICORE_ARMOURED_SWA,
                coreCount = 4
            ),
            segments = listOf(
                RouteSegment("s1", 0, "Tray E", 50.0, ReferenceMethodCode.E, SegmentConditions()),
                RouteSegment("s2", 1, "Conduit B2", 25.0, ReferenceMethodCode.B2, SegmentConditions())
            ),
            protection = ProtectionAndLimitsInput(
                deviceType = ProtectiveDeviceType.MCCB_THERMAL_MAGNETIC,
                ratedCurrentInA = 50.0,
                breakingCapacityKa = 15.0,
                prospectiveFaultCurrentIkKa = 6.0,
                voltageDropLimitPct = 5.0
            )
        )

        val result = CableSizingEngine.evaluate(project, genericCatalog)
        assertNotNull(result.recommendedCandidate)
        val selected = result.recommendedCandidate!!
        assertEquals(CandidateDecision.SELECTED, selected.decision)
        assertTrue(result.candidateComparisons.any { it.decision == CandidateDecision.REJECTED && it.rejectionReasons.isNotEmpty() })
        assertTrue(result.candidateComparisons.any { it.decision == CandidateDecision.ACCEPTED_ALTERNATIVE })
        assertEquals(75.0, result.totalRouteLengthM, 0.01)
        assertEquals(ReferenceMethodCode.B2, result.governingSegment?.methodCode)
    }

    @Test
    fun `dc solar pv circuit ignores ac reactance and calculates resistive drop`() {
        val project = CircuitDesignProject(
            metadata = ProjectMetadata(id = "t2", projectName = "PV DC String"),
            system = ElectricalSystemInput(
                systemType = CurrentSystemType.DC,
                nominalVoltageV = 600.0,
                loadInputMode = LoadInputMode.ACTIVE_POWER_KW,
                activePowerKw = 12.0,
                efficiencyPct = 100.0
            ),
            cable = CableSpecInput(
                sizingMode = SizingMode.MANUAL,
                conductorMaterial = ConductorMaterial.COPPER,
                insulationType = InsulationType.XLPE_90C,
                cableConstruction = CableConstruction.SOLAR_DC_DOUBLE_INSULATED,
                coreCount = 1,
                manualCableSizeMm2 = 6.0
            ),
            segments = listOf(
                RouteSegment("s1", 0, "Roof Conduit", 30.0, ReferenceMethodCode.B1)
            ),
            protection = ProtectionAndLimitsInput(
                deviceType = ProtectiveDeviceType.DC_FUSE_GPV,
                ratedCurrentInA = 25.0,
                overloadFactorI2Ratio = 1.35,
                prospectiveFaultCurrentIkKa = 1.0,
                breakingCapacityKa = 10.0,
                voltageDropLimitPct = 3.0
            )
        )
        val result = CableSizingEngine.evaluate(project, genericCatalog)
        assertEquals(20.0, result.designCurrentIbA!!, 0.01)
        assertEquals(CheckStatus.PASS, result.evaluatedManualCandidate?.voltageDropCheck)
    }
}

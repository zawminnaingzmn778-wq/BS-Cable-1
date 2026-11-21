package com.example.domain.model

data class AppliedFactorDetail(
    val symbol: String,
    val name: String,
    val value: Double,
    val tableReference: String,
    val dataSource: DataSourceType,
    val verificationStatus: VerificationStatus,
    val calculationNote: String
)

data class SegmentEvaluationResult(
    val segmentId: String,
    val label: String,
    val orderIndex: Int,
    val lengthMeters: Double,
    val methodCode: ReferenceMethodCode,
    val effectiveCableSizeMm2: Double,
    val effectiveParallelRuns: Int,
    val tabulatedItA: Double?,
    val appliedFactors: List<AppliedFactorDetail>,
    val totalCorrectionFactor: Double,
    val effectiveIzPerRunA: Double?,
    val effectiveTotalIzA: Double?,
    val segmentVoltageDropV: Double?,
    val segmentVoltageDropPct: Double?,
    val isGoverningAmpacity: Boolean,
    val isSupportedMethod: Boolean,
    val notes: String
)

data class CandidateEvaluation(
    val cableEntry: CableCatalogEntry,
    val sizeMm2: Double,
    val parallelRuns: Int,
    val tabulatedItA: Double?,
    val governingMethod: ReferenceMethodCode?,
    val governingSegmentLabel: String,
    val minTotalCorrectionFactor: Double,
    val correctedIzA: Double?,
    val designCurrentIbA: Double,
    val deviceRatedInA: Double,
    val voltageDropV: Double?,
    val voltageDropPct: Double?,
    val adiabaticMinCsaMm2: Double?,
    val ampacityCheck: CheckStatus,
    val protectionCheck: CheckStatus,
    val voltageDropCheck: CheckStatus,
    val shortCircuitCheck: CheckStatus,
    val decision: CandidateDecision,
    val rejectionReasons: List<RejectionReason>,
    val explanation: String
)

data class CheckItemResult(
    val id: String,
    val title: String,
    val standardRef: String,
    val requirementText: String,
    val calculatedText: String,
    val status: CheckStatus,
    val explanation: String
)

data class CalculationStepTrace(
    val stepNumber: Int,
    val calculationName: String,
    val formula: String,
    val variableDefinitions: List<String>,
    val inputValues: String,
    val substitution: String,
    val intermediateResult: String,
    val finalResult: String,
    val unit: String,
    val standardReference: String,
    val dataSource: DataSourceType,
    val verificationStatus: VerificationStatus,
    val outcome: CheckStatus
)

data class EngineeringCalculationResult(
    val designCurrentIbA: Double?,
    val requiredTabulatedItA: Double?,
    val totalRouteLengthM: Double,
    val governingSegment: SegmentEvaluationResult?,
    val segmentResults: List<SegmentEvaluationResult>,
    val recommendedCandidate: CandidateEvaluation?,
    val evaluatedManualCandidate: CandidateEvaluation?,
    val candidateComparisons: List<CandidateEvaluation>,
    val individualChecks: List<CheckItemResult>,
    val calculationTraces: List<CalculationStepTrace>,
    val overallStatus: CheckStatus,
    val dataVerificationLevel: VerificationStatus,
    val standardsVerificationNotice: String,
    val engineeringAssumptions: List<String>,
    val warnings: List<String>,
    val validationErrors: List<String>
)

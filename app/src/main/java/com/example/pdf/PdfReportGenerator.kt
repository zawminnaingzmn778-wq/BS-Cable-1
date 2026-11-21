package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.model.CandidateDecision
import com.example.domain.model.CheckStatus
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.EngineeringCalculationResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 72f

    fun generateToFile(
        context: Context,
        project: CircuitDesignProject,
        result: EngineeringCalculationResult
    ): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeCircuit = project.metadata.circuitId.ifBlank { "C1" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "BS7671_Report_${safeCircuit}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            writePdf(project, result, out)
        }
        return file
    }

    fun writePdf(
        project: CircuitDesignProject,
        result: EngineeringCalculationResult,
        outputStream: OutputStream
    ) {
        val doc = PdfDocument()
        val state = PageWriter(doc)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 9.5f
            isAntiAlias = true
        }
        val h2Paint = Paint().apply {
            color = Color.parseColor("#0F1E2E")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9f
            isAntiAlias = true
        }
        val monoPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8.5f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 8f
            isAntiAlias = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0F1E2E")
            style = Paint.Style.FILL
        }
        val accentPaint = Paint().apply {
            color = Color.parseColor("#D97706")
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
        }

        // Top Banner
        state.ensureSpace(90f)
        state.canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 78f, headerBgPaint)
        state.canvas.drawRect(0f, 75f, PAGE_WIDTH.toFloat(), 78f, accentPaint)
        state.canvas.drawText("BS 7671 CABLE SIZING & VERIFICATION REPORT", MARGIN_X, 30f, titlePaint)
        state.canvas.drawText(
            "Standard: ${project.standardEdition}  |  Mode: ${project.cable.sizingMode.label}  |  Overall: ${result.overallStatus.label}",
            MARGIN_X,
            48f,
            subtitlePaint
        )
        state.canvas.drawText(
            "Data Status: ${result.dataVerificationLevel.label}",
            MARGIN_X,
            63f,
            subtitlePaint
        )
        state.y = 94f

        // Standards Verification Banner
        val bannerBg = Paint().apply {
            color = if (result.overallStatus == CheckStatus.PASS && result.dataVerificationLevel.name == "VERIFIED") {
                Color.parseColor("#ECFDF5")
            } else {
                Color.parseColor("#FFFBEB")
            }
        }
        val bannerLines = wrapText(result.standardsVerificationNotice, bodyPaint, CONTENT_WIDTH - 16f)
        val bannerH = 14f + bannerLines.size * 12f
        state.ensureSpace(bannerH + 10f)
        state.canvas.drawRect(MARGIN_X, state.y, MARGIN_X + CONTENT_WIDTH, state.y + bannerH, bannerBg)
        var by = state.y + 13f
        bannerLines.forEach { ln ->
            state.canvas.drawText(ln, MARGIN_X + 8f, by, bodyPaint)
            by += 12f
        }
        state.y += bannerH + 12f

        // Section 1: Project Information
        state.drawSectionHeader("1. PROJECT & CIRCUIT INFORMATION", h2Paint, linePaint)
        state.drawKeyValue("Project Name", project.metadata.projectName.ifBlank { "Untitled Project" }, bodyPaint)
        state.drawKeyValue("Project Ref / Client", "${project.metadata.projectReference.ifBlank { "-" }}  |  ${project.metadata.client.ifBlank { "-" }}", bodyPaint)
        state.drawKeyValue("Site / Location", project.metadata.siteLocation.ifBlank { "-" }, bodyPaint)
        state.drawKeyValue("Engineer / Date", "${project.metadata.engineerName.ifBlank { "-" }}  |  ${project.metadata.designDate.ifBlank { "-" }}", bodyPaint)
        state.drawKeyValue("Circuit ID & Desc", "${project.metadata.circuitId.ifBlank { "C1" }} — ${project.metadata.circuitDescription.ifBlank { "-" }}", bodyPaint)
        if (project.metadata.designNotes.isNotBlank()) {
            state.drawWrapped("Design Notes: ${project.metadata.designNotes}", mutedPaint)
        }
        state.y += 6f

        // Section 2: Electrical System & Cable Characteristics
        state.drawSectionHeader("2. ELECTRICAL SYSTEM & CABLE SPECIFICATION", h2Paint, linePaint)
        val phaseOrDc = if (project.system.systemType.name == "AC") project.system.acPhaseConfig.label else project.system.dcConfig.label
        state.drawKeyValue("System", "${project.system.systemType.label} — $phaseOrDc (${fmt(project.system.nominalVoltageV)} V)", bodyPaint)
        state.drawKeyValue("Design Current (Ib)", "${result.designCurrentIbA?.let { fmt(it) } ?: "N/A"} A (${project.system.loadInputMode.label})", bodyPaint)
        state.drawKeyValue("Cable Spec", "${project.cable.conductorMaterial.label}, ${project.cable.insulationType.label}, ${project.cable.cableConstruction.label}", bodyPaint)
        state.drawKeyValue("Parallel Runs / Temp", "${project.cable.parallelRuns} run(s) per phase, Max Op Temp ${fmt(project.cable.maxOperatingTempC)}°C", bodyPaint)
        state.drawKeyValue("Protective Device", "${project.protection.deviceType.label}, In = ${fmt(project.protection.ratedCurrentInA)} A, Icu = ${fmt(project.protection.breakingCapacityKa)} kA, Ik = ${fmt(project.protection.prospectiveFaultCurrentIkKa)} kA", bodyPaint)
        state.drawKeyValue("VD Limit / Fault t", "Max ΔV = ${fmt(project.protection.voltageDropLimitPct)}%, Fault Duration t = ${fmt(project.protection.faultDurationS)} s", bodyPaint)
        state.y += 6f

        // Section 3: Summary Results & Individual Checks
        state.drawSectionHeader("3. SUMMARY VERIFICATION RESULTS", h2Paint, linePaint)
        val activeCand = result.recommendedCandidate ?: result.evaluatedManualCandidate
        state.drawKeyValue(
            "Selected / Evaluated Cable",
            activeCand?.let { "${it.sizeMm2} mm² (${it.parallelRuns}×) — ${it.decision.label}" } ?: "None compliant",
            bodyPaint
        )
        state.drawKeyValue("Total Route Length", "${fmt(result.totalRouteLengthM)} m (${result.segmentResults.size} segments)", bodyPaint)
        state.drawKeyValue("Governing Segment", result.governingSegment?.let { "${it.label} (Method ${it.methodCode.code}, C_total=${String.format(Locale.US, "%.3f", it.totalCorrectionFactor)})" } ?: "-", bodyPaint)
        state.drawKeyValue("Corrected Ampacity (Iz)", "${activeCand?.correctedIzA?.let { fmt(it) } ?: "N/A"} A vs Ib = ${result.designCurrentIbA?.let { fmt(it) } ?: "N/A"} A", bodyPaint)
        state.drawKeyValue("Total Voltage Drop", "${activeCand?.voltageDropV?.let { fmt(it) } ?: "N/A"} V (${activeCand?.voltageDropPct?.let { fmt(it) } ?: "N/A"}%)", bodyPaint)

        result.individualChecks.forEach { chk ->
            state.drawWrapped("[${chk.status.label}] ${chk.title}: ${chk.calculatedText} (Req: ${chk.requirementText}) — ${chk.standardRef}", bodyPaint)
        }
        state.y += 6f

        // Section 4: Route Segments & Correction Factors
        state.drawSectionHeader("4. ROUTE SEGMENTS & CORRECTION FACTORS", h2Paint, linePaint)
        result.segmentResults.forEach { seg ->
            val govTag = if (seg.isGoverningAmpacity) " [GOVERNING]" else ""
            val factorsStr = seg.appliedFactors.joinToString(", ") { "${it.symbol}=${String.format(Locale.US, "%.3f", it.value)}" }
            state.drawWrapped(
                "${seg.orderIndex + 1}. ${seg.label}$govTag | ${fmt(seg.lengthMeters)} m | Method ${seg.methodCode.code} | It=${seg.tabulatedItA?.let { fmt(it) } ?: "?"} A | $factorsStr | C_tot=${String.format(Locale.US, "%.3f", seg.totalCorrectionFactor)} | Iz=${seg.effectiveTotalIzA?.let { fmt(it) } ?: "?"} A | ΔV=${seg.segmentVoltageDropV?.let { fmt(it) } ?: "?"} V",
                monoPaint
            )
        }
        state.y += 6f

        // Section 5: Candidate Cable Comparison
        state.drawSectionHeader("5. CANDIDATE CABLE COMPARISON", h2Paint, linePaint)
        result.candidateComparisons.take(12).forEach { cand ->
            val dec = when (cand.decision) {
                CandidateDecision.SELECTED -> "SELECTED"
                CandidateDecision.ACCEPTED_ALTERNATIVE -> "ACCEPTED ALT"
                CandidateDecision.REJECTED -> "REJECTED"
            }
            val line = "${cand.sizeMm2} mm² | It=${cand.tabulatedItA?.let { fmt(it) } ?: "?"} A | Iz=${cand.correctedIzA?.let { fmt(it) } ?: "?"} A | ΔV=${cand.voltageDropPct?.let { fmt(it) } ?: "?"}% | [$dec] ${cand.explanation}"
            state.drawWrapped(line, monoPaint)
        }
        state.y += 6f

        // Section 6: Calculation Traceability
        state.drawSectionHeader("6. STEP-BY-STEP CALCULATION TRACEABILITY", h2Paint, linePaint)
        result.calculationTraces.forEach { tr ->
            state.drawWrapped("Step ${tr.stepNumber}: ${tr.calculationName} [${tr.outcome.label}]", h2Paint)
            state.drawWrapped("  Formula: ${tr.formula}", monoPaint)
            state.drawWrapped("  Inputs: ${tr.inputValues}", bodyPaint)
            state.drawWrapped("  Substitution: ${tr.substitution} = ${tr.finalResult} ${tr.unit}", monoPaint)
            state.drawWrapped("  Ref: ${tr.standardReference} (${tr.dataSource.label} — ${tr.verificationStatus.label})", mutedPaint)
            state.y += 4f
        }

        // Section 7: Assumptions & Warnings
        state.drawSectionHeader("7. ENGINEERING ASSUMPTIONS & VERIFICATION NOTES", h2Paint, linePaint)
        result.warnings.forEach { w -> state.drawWrapped("WARNING: $w", bodyPaint) }
        result.engineeringAssumptions.forEach { a -> state.drawWrapped("• $a", mutedPaint) }

        state.finish()
        doc.writeTo(outputStream)
        doc.close()
    }

    private class PageWriter(private val doc: PdfDocument) {
        private var pageIndex = 1
        private var page: PdfDocument.Page = doc.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
        )
        var canvas: Canvas = page.canvas
        var y: Float = 40f

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - 36f) {
                doc.finishPage(page)
                pageIndex++
                page = doc.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
                )
                canvas = page.canvas
                y = 40f
            }
        }

        fun drawSectionHeader(title: String, paint: Paint, linePaint: Paint) {
            ensureSpace(26f)
            canvas.drawText(title, MARGIN_X, y + 12f, paint)
            canvas.drawLine(MARGIN_X, y + 16f, MARGIN_X + CONTENT_WIDTH, y + 16f, linePaint)
            y += 24f
        }

        fun drawKeyValue(label: String, value: String, paint: Paint) {
            val text = "$label: $value"
            drawWrapped(text, paint)
        }

        fun drawWrapped(text: String, paint: Paint) {
            val lines = wrapText(text, paint, CONTENT_WIDTH)
            val lineH = paint.textSize + 4f
            lines.forEach { line ->
                ensureSpace(lineH)
                canvas.drawText(line, MARGIN_X, y + paint.textSize, paint)
                y += lineH
            }
        }

        fun finish() {
            doc.finishPage(page)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
}

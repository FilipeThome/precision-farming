package com.precisionfarming.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.PrescriptionDto
import com.precisionfarming.mobile.data.formatNumber
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S

/** Compact prescription facts for operation execution / inspector. Never fabricates mode/MoA/fraction. */
@Composable
fun PrescriptionSummary(
    prescription: PrescriptionDto?,
    modifier: Modifier = Modifier,
) {
    val modeLabel = prescription?.mode?.takeIf { it.isNotBlank() }?.let { mode ->
        when (mode.uppercase()) {
            "SPOT" -> S.t("prescription.mode.SPOT")
            "BROADCAST" -> S.t("prescription.mode.BROADCAST")
            else -> DomainLabels.label(mode)
        }
    } ?: "—"
    val doseLabel = prescription?.let { p ->
        val dose = p.plannedDose
        if (dose != null && dose.isFinite()) {
            "${formatNumber(dose)} ${p.unit}".trim()
        } else {
            "—"
        }
    } ?: "—"
    val moaLabel = prescription?.moaGroup?.takeIf { it.isNotBlank() }?.let { DomainLabels.label(it) } ?: "—"
    val fractionLabel = prescription?.treatedFraction?.takeIf { it.isFinite() }?.let { formatNumber(it) } ?: "—"
    val summary = if (prescription == null) {
        S.t("prescription.missing")
    } else {
        listOf(
            "${S.t("prescription.mode")}: $modeLabel",
            "${S.t("prescription.dose")}: $doseLabel",
            "${S.t("prescription.moa")}: $moaLabel",
            "${S.t("prescription.treatedFraction")}: $fractionLabel",
        ).joinToString(" · ")
    }
    AgCard(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(14.dp)
                .semantics(mergeDescendants = true) { contentDescription = summary },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                S.t("prescriptions.title"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (prescription == null) {
                Text("—", style = MaterialTheme.typography.titleMedium)
                Text(
                    S.t("prescription.missing"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                InspectorKpis(
                    listOf(
                        InspectorKpiItem(S.t("prescription.mode"), modeLabel),
                        InspectorKpiItem(S.t("prescription.dose"), doseLabel),
                        InspectorKpiItem(S.t("prescription.moa"), moaLabel),
                        InspectorKpiItem(S.t("prescription.treatedFraction"), fractionLabel),
                    ),
                )
            }
        }
    }
}

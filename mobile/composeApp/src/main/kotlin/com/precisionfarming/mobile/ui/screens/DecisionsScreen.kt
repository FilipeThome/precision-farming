package com.precisionfarming.mobile.ui.screens

import com.precisionfarming.mobile.i18n.LocalAppLocale
import com.precisionfarming.mobile.ui.LocalFarmId
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.DecisionFilter
import com.precisionfarming.mobile.data.DecisionItem
import com.precisionfarming.mobile.data.DecisionSource
import com.precisionfarming.mobile.data.DecisionSources
import com.precisionfarming.mobile.data.DecisionStatus
import com.precisionfarming.mobile.data.EntityNames
import com.precisionfarming.mobile.data.FarmFilter
import com.precisionfarming.mobile.data.InspectNav
import com.precisionfarming.mobile.data.MeDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.approvePrescription
import com.precisionfarming.mobile.data.byId
import com.precisionfarming.mobile.data.canManageFarmOps
import com.precisionfarming.mobile.data.chainSteps
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.filterDecisions
import com.precisionfarming.mobile.data.formatDecimal
import com.precisionfarming.mobile.data.insights
import com.precisionfarming.mobile.data.irrigationRecommendations
import com.precisionfarming.mobile.data.me
import com.precisionfarming.mobile.data.needsHumanReview
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.data.prescriptions
import com.precisionfarming.mobile.data.recommendations
import com.precisionfarming.mobile.data.sortByPriority
import com.precisionfarming.mobile.data.toDecisionItems
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.DetailSheet
import com.precisionfarming.mobile.ui.components.EntityCard
import com.precisionfarming.mobile.ui.components.LoadState
import com.precisionfarming.mobile.ui.components.ScreenHeader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class DecisionsBundle(
    val items: List<DecisionItem>,
    val operations: List<OperationDto>,
    val me: MeDto?,
    val meFailed: Boolean,
    val partialError: Boolean,
)

@Composable
fun DecisionsScreen(
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClearSelected: () -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit = {},
) {
    var state by remember { mutableStateOf<LoadState<DecisionsBundle>>(LoadState.Loading) }
    var filter by remember { mutableStateOf(DecisionFilter.PENDING) }
    var justification by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            if (state !is LoadState.Ok) state = LoadState.Loading
            state = runCatching {
                val farmId = FarmFilter.farmId.value
                coroutineScope {
                    val rxJob = async { runCatching { prescriptions(farmId) } }
                    val irrJob = async { runCatching { irrigationRecommendations(farmId) } }
                    val agroJob = async { runCatching { recommendations(farmId) } }
                    val insightJob = async { runCatching { insights(farmId) } }
                    val fieldJob = async { runCatching { fields(farmId) } }
                    val opJob = async { runCatching { operations(farmId) } }
                    val meJob = async { runCatching { me() } }
                    val rx = rxJob.await()
                    val irr = irrJob.await()
                    val agro = agroJob.await()
                    val insight = insightJob.await()
                    val field = fieldJob.await()
                    val op = opJob.await()
                    val meResult = meJob.await()
                    val partial = listOf(rx, irr, agro, insight, field, op).any { it.isFailure }
                    DecisionsBundle(
                        items = toDecisionItems(
                            DecisionSources(
                                prescriptions = rx.getOrDefault(emptyList()),
                                irrigation = irr.getOrDefault(emptyList()),
                                agronomy = agro.getOrDefault(emptyList()),
                                insights = insight.getOrDefault(emptyList()),
                                fields = field.getOrDefault(emptyList()),
                            ),
                        ),
                        operations = op.getOrDefault(emptyList()),
                        me = meResult.getOrNull(),
                        meFailed = meResult.isFailure,
                        partialError = partial,
                    )
                }
            }.fold(
                onSuccess = { LoadState.Ok(listOf(it)) },
                onFailure = { LoadState.Err(it.message ?: S.t("common.error")) },
            )
        }
    }

    LaunchedEffect(LocalFarmId.current, LocalAppLocale.current) { reload() }

    val bundle = (state as? LoadState.Ok)?.items?.firstOrNull()
    val filtered = bundle?.let { sortByPriority(filterDecisions(it.items, filter)) }.orEmpty()
    val selected = filtered.byId(selectedId) { it.id }
        ?: bundle?.items?.byId(selectedId) { it.id }

    LazyColumn(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader(S.t("decisions.title"), onBack) }
        item {
            Text(S.t("decisions.description"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    DecisionFilter.PENDING to "decisions.filter.pending",
                    DecisionFilter.APPROVED to "decisions.filter.approved",
                    DecisionFilter.ALL to "decisions.filter.all",
                ).forEach { (value, key) ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { filter = value },
                        label = { Text(S.t(key)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        when (val s = state) {
            is LoadState.Loading -> item { Text(S.t("common.loading")) }
            is LoadState.Err -> item { Text("${S.t("common.error")}: ${s.message}") }
            is LoadState.Ok -> {
                bundle?.takeIf { it.partialError }?.let {
                    item {
                        Text(S.t("decisions.partialError"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (filtered.isEmpty()) {
                    item { Text(S.t("decisions.empty")) }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        EntityCard(
                            headline = DomainLabels.label(item.title),
                            supporting = listOfNotNull(
                                S.t("decisions.source.${item.source.name}"),
                                item.quantity?.let { "${it.value} ${it.unit}" },
                                item.priority?.let { DomainLabels.label(it) },
                            ).joinToString(" · "),
                            status = DomainLabels.label(item.status.name),
                            onClick = { onSelect(item.id) },
                        )
                    }
                }
            }
        }
        item { TextButton(onClick = { reload() }) { Text(S.t("common.refresh")) } }
    }

    if (!selectedId.isNullOrBlank()) {
        val row = selected
        DetailSheet(
            title = row?.let { DomainLabels.label(it.title) } ?: S.t("inspector.notFound"),
            found = row != null,
            onDismiss = {
                justification = ""
                msg = null
                onClearSelected()
            },
        ) {
            if (row != null) {
                DecisionDetail(
                    item = row,
                    operations = bundle?.operations.orEmpty(),
                    me = bundle?.me,
                    meFailed = bundle?.meFailed == true,
                    justification = justification,
                    onJustification = { justification = it },
                    msg = msg,
                    onApprove = {
                        scope.launch {
                            runCatching { approvePrescription(row.rawId) }
                                .onSuccess {
                                    msg = S.t("decisions.approval.approved")
                                    justification = ""
                                    reload()
                                }
                                .onFailure { msg = it.message }
                        }
                    },
                    onOpenIrrigation = {
                        onOpen(InspectNav.IRRIGATION)
                    },
                )
            }
        }
    }
}

@Composable
private fun DecisionDetail(
    item: DecisionItem,
    operations: List<OperationDto>,
    me: MeDto?,
    meFailed: Boolean,
    justification: String,
    onJustification: (String) -> Unit,
    msg: String?,
    onApprove: () -> Unit,
    onOpenIrrigation: () -> Unit,
) {
    val canManage = canManageFarmOps(me?.role)
    val canApprove = item.source == DecisionSource.PRESCRIPTION && item.capabilities.approve && canManage
    val pending = item.status == DecisionStatus.PENDING
    val steps = chainSteps(item, operations)
    val dataLines = item.explanation?.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(item.summary)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(S.t("decisions.context.title"), style = MaterialTheme.typography.titleSmall)
        Text(
            item.summary?.let { DomainLabels.label(it) } ?: DomainLabels.label(item.title),
            style = MaterialTheme.typography.bodyMedium,
        )
        listOfNotNull(
            item.priority?.let { S.t("decisions.context.priority", "value" to DomainLabels.label(it)) },
            item.fieldId?.let { EntityNames.nameOf(it) ?: it },
            item.entityId?.takeIf { item.fieldId == null }?.let {
                S.t("decisions.context.entity", "id" to it)
            },
            item.model?.let { m ->
                buildString {
                    append(m)
                    item.modelVersion?.let { append(" v$it") }
                }
            },
        ).forEach { line ->
            Text(line, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(S.t("decisions.data.title"), style = MaterialTheme.typography.titleSmall)
        if (dataLines.isEmpty()) {
            Text(S.t("decisions.data.empty"), style = MaterialTheme.typography.bodyMedium)
        } else {
            dataLines.forEach { line ->
                Text(DomainLabels.label(line), style = MaterialTheme.typography.bodySmall)
            }
        }

        Text(S.t("decisions.confidence.title"), style = MaterialTheme.typography.titleSmall)
        val confidence = item.confidence
        if (confidence != null) {
            Text(
                "${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
            )
            item.score?.let {
                Text(
                    S.t("decisions.confidence.score", "value" to formatDecimal(it)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (needsHumanReview(item)) {
                Text(S.t("tower.queue.tag.review"), style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Text(S.t("decisions.confidence.none"), style = MaterialTheme.typography.bodyMedium)
        }

        Text(S.t("chain.label"), style = MaterialTheme.typography.titleSmall)
        steps.forEach { step ->
            val label = S.t("chain.${step.id.name.lowercase()}")
            val stateLabel = S.t("chain.state.${step.state.name.lowercase()}")
            Text(
                buildString {
                    append(label)
                    append(" · ")
                    append(stateLabel)
                    if (step.inferred) append(" · ${S.t("decisions.linkedOp.inferred")}")
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(S.t("decisions.approval.title"), style = MaterialTheme.typography.titleSmall)
        if (!pending) {
            Text(
                when (item.status) {
                    DecisionStatus.APPROVED, DecisionStatus.EXECUTED ->
                        S.t("decisions.approval.alreadyApproved", "when" to (item.approvedAt ?: "—"))
                    DecisionStatus.REJECTED -> S.t("decisions.approval.rejected")
                    else -> S.t("decisions.approval.noWorkflow")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (pending && canApprove) {
            OutlinedTextField(
                value = justification,
                onValueChange = onJustification,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("${S.t("decisions.approval.justification")} ${S.t("decisions.approval.required")}") },
                minLines = 3,
            )
            Button(
                onClick = onApprove,
                enabled = justification.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) { Text(S.t("decisions.approval.approve")) }
            Text(S.t("decisions.approval.justificationNotSent"), style = MaterialTheme.typography.labelSmall)
        }
        if (pending && item.source == DecisionSource.PRESCRIPTION && meFailed) {
            Text(S.t("auth.meLoadError"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (pending && item.source == DecisionSource.PRESCRIPTION && !meFailed && me != null && !canManage) {
            Text(S.t("decisions.approval.noPermission"), style = MaterialTheme.typography.bodyMedium)
        }
        if (pending && item.source == DecisionSource.IRRIGATION) {
            Text(S.t("decisions.approval.irrigationHint"), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenIrrigation) { Text(S.t("decisions.approval.openIrrigation")) }
        }
        if (pending && item.source == DecisionSource.AGRONOMY) {
            Text(S.t("decisions.approval.noWorkflow"), style = MaterialTheme.typography.bodyMedium)
        }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

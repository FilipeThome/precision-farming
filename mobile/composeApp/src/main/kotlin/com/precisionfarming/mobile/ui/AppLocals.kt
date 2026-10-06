package com.precisionfarming.mobile.ui

import androidx.compose.runtime.compositionLocalOf
import com.precisionfarming.mobile.data.FarmFilter

/** Farm selected in the shell. Provided from [FarmFilter.farmId] collected in [AppRoot]. */
val LocalFarmId = compositionLocalOf<String?> { FarmFilter.farmId.value }

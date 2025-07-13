package com.browntowndev.liftlab.core.persistence.dtos.firebase

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

@Keep
data class LiftMetricChartFirebaseDto(
    var id: Long = 0L,
    var liftId: Long? = null,
    var chartType: LiftMetricChartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX,
): BaseFirebaseDto()

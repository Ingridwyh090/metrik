package fourkeymetrics.metric.calculator

import fourkeymetrics.metric.model.Build
import fourkeymetrics.metric.model.LEVEL


interface MetricCalculator {
    fun calculateValue(allBuilds: List<Build>, startTimestamp: Long, endTimestamp: Long, targetStage: String): Double

    fun calculateLevel(value: Double): LEVEL
}
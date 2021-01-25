package fourkeymetrics.metric.calculator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fourkeymetrics.metric.model.Build
import fourkeymetrics.metric.model.LEVEL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Import(ChangeFailureRateCalculator::class, ObjectMapper::class)
class ChangeFailureRateCalculatorTest {
    @Autowired
    private lateinit var changeFailureRateCalculator: ChangeFailureRateCalculator

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * test file: builds-cfr.json
     * build 1 : 1606780800000, SUCCESS (deploy to prod)
     * build 2 : 1609459200000, FAILED (deploy to prod)
     * build 3 : 1610668800000, SUCCESS (deploy to prod)
     * build 4 : 1611964800000, SUCCESS (deploy to prod)
     * build 5 : 1611974800000, SUCCESS (deploy to uat)
     */
    @Test
    fun `should get change failure rate within time range given 3 valid build inside when calculate CFR`() {
        val targetStage = "deploy to prod"
        // build 2 - build 5
        val startTimestamp = 1609459200000L
        val endTimestamp = 1611994800000L

        val mockBuildList: List<Build> =
            objectMapper.readValue(this.javaClass.getResource("/service/builds-cfr.json").readText())

        assertThat(
            changeFailureRateCalculator.calculateValue(
                mockBuildList,
                startTimestamp,
                endTimestamp,
                targetStage
            )
        ).isEqualTo(1 / 3.0)
    }

    /**
     * test file: builds-cfr.json
     * build 1 : 1606780800000, SUCCESS (deploy to prod)
     * build 2 : 1609459200000, FAILED (deploy to prod)
     * build 3 : 1610668800000, SUCCESS (deploy to prod)
     * build 4 : 1611964800000, SUCCESS (deploy to prod)
     * build 5 : 1611974800000, SUCCESS (deploy to uat)
     */
    @Test
    fun `should get zero within time range given no valid build when calculate CFR`() {
        val targetStage = "deploy to prod"
        // build 5
        val startTimestamp = 1611974800000L
        val endTimestamp = 1611974800000L

        val mockBuildList: List<Build> =
            objectMapper.readValue(this.javaClass.getResource("/service/builds-cfr.json").readText())

        assertThat(
            changeFailureRateCalculator.calculateValue(
                mockBuildList,
                startTimestamp,
                endTimestamp,
                targetStage
            )
        ).isEqualTo(0.0)
    }

    @Test
    internal fun `should return level is low`() {
        assertEquals(LEVEL.LOW, changeFailureRateCalculator.calculateLevel(1.0))
    }
}
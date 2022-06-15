package metrik.metrics.domain.calculator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import metrik.metrics.domain.model.LEVEL
import metrik.project.domain.model.Execution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MeanTimeToRestoreCalculatorTest {
    private lateinit var meanTimeToRestoreCalculator: MeanTimeToRestoreCalculator

    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        meanTimeToRestoreCalculator = MeanTimeToRestoreCalculator()
    }

    /**
     * test file: builds-for-MTTR-case-1.json
     * build 1 : deploy to prod, SUCCESS, end at 2020-12-29
     * build 2 : deploy to prod, FAILED, end at 2020-12-30 *
     * build 3 : deploy to prod, FAILED, end at 2020-12-31 *
     * build 4 : deploy to prod, SUCCESS, end at 2021-01-01 *
     * build 5 : deploy to prod, FAILED, end at 2021-01-02 *
     * build 6 : deploy to prod, SUCCESS, end at 2021-01-03 *
     * build 7 : deploy to prod, SUCCESS, end at 2021-01-05
     */
    @Test
    fun `should return MTTR given all builds`() {
        val allExecutions: List<Execution> = objectMapper.readValue(
            this.javaClass.getResource("/calculator/builds-for-MTTR-case-1.json").readText()
        )

        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to uat"))

        val mttr = meanTimeToRestoreCalculator.calculateValue(allExecutions, startTimestamp, endTimestamp, targetStage)

        assertThat(mttr.toDouble() * MILLISECOND_TO_HOURS).isEqualTo(144000005.00)
    }

    /**
     * test file: builds-for-MTTR-case-2.json
     * build 1, pipeline 1: deploy to prod, FAILED, end at 2020-12-29 *
     * build 2, pipeline 1: deploy to prod, FAILED, end at 2020-12-30 *
     * build 3, pipeline 1: deploy to prod, FAILED, end at 2020-12-31 *
     * build 4, pipeline 1: deploy to prod, SUCCESS, end at 2021-01-01 *
     * build 5, pipeline 2: deploy to prod, FAILED, end at 2021-01-02 *
     * build 6, pipeline 2: deploy to prod, SUCCESS, end at 2021-01-03 *
     */
    @Test
    fun `should contain first build given builds all failed before begin time`() {
        val allExecutions: List<Execution> = objectMapper.readValue(
            this.javaClass.getResource("/calculator/builds-for-MTTR-case-2.json").readText()
        )

        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to prod"))

        val averageMTTR =
            meanTimeToRestoreCalculator.calculateValue(allExecutions, startTimestamp, endTimestamp, targetStage)

        assertThat(averageMTTR.toDouble() * MILLISECOND_TO_HOURS).isEqualTo(187201525.00)
    }

    /**
     * test file: builds-for-MTTR-case-3.json
     * build 1, pipeline 1 : deploy to prod, SUCCESS, end at 2020-12-29
     * build 2, pipeline 1 : deploy to prod, SUCCESS, end at 2020-12-30
     * build 3, pipeline 1 : deploy to prod, SUCCESS, end at 2020-12-31
     * build 4, pipeline 1 : deploy to prod, SUCCESS, end at 2021-01-01
     * build 5, pipeline 2 : deploy to prod, SUCCESS, end at 2021-01-02
     * build 6, pipeline 2 : deploy to prod, SUCCESS, end at 2021-01-03
     * build 7, pipeline 2 : deploy to prod, SUCCESS, end at 2021-01-05
     */
    @Test
    fun `should return NaN while all builds are success`() {
        val allExecutions: List<Execution> = objectMapper.readValue(
            this.javaClass.getResource("/calculator/builds-for-MTTR-case-3.json").readText()
        )

        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to prod"))

        val averageMTTR =
            meanTimeToRestoreCalculator.calculateValue(allExecutions, startTimestamp, endTimestamp, targetStage)

        assertEquals(averageMTTR, Double.NaN)
    }

    /**
     * test file: builds-for-MTTR-case-raw-build-summary-4.json
     * build 1, pipeline 1 : deploy to prod, FAILED, end at 2020-12-29
     * build 2, pipeline 1 : deploy to prod, FAILED, end at 2020-12-30
     * build 3, pipeline 1 : deploy to prod, FAILED, end at 2020-12-31
     * build 4, pipeline 1 : deploy to prod, FAILED, end at 2021-01-01
     * build 5, pipeline 2 : deploy to prod, FAILED, end at 2021-01-02
     * build 6, pipeline 2 : deploy to prod, FAILED, end at 2021-01-03
     * build 7, pipeline 2 : deploy to prod, FAILED, end at 2021-01-05
     */
    @Test
    fun `should return NaN while all builds are failed`() {
        val allExecutions: List<Execution> = objectMapper.readValue(
            this.javaClass.getResource("/calculator/builds-for-MTTR-case-4.json").readText()
        )

        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to prod"))

        val averageMTTR =
            meanTimeToRestoreCalculator.calculateValue(allExecutions, startTimestamp, endTimestamp, targetStage)

        assertEquals(averageMTTR, Double.NaN)
    }

    /**
     * test file: builds-for-MTTR-case-5.json
     * build 5, pipeline 1 : deploy to prod, SUCCESS, end at 2021-01-02 *
     * build 6, pipeline 1 : deploy to prod, FAILED, end at 2021-01-03 *
     * build 5, pipeline 2 : deploy to prod, SUCCESS, end at 2021-01-02 *
     * build 6, pipeline 2 : deploy to prod, FAILED, end at 2021-01-03 *
     */
    @Test
    fun `should return correct MTTR given last stage status is failed `() {
        val allExecutions: List<Execution> = objectMapper.readValue(
            this.javaClass.getResource("/calculator/builds-for-MTTR-case-5.json").readText()
        )

        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to prod"))

        val averageMTTR =
            meanTimeToRestoreCalculator.calculateValue(allExecutions, startTimestamp, endTimestamp, targetStage)

        assertThat(averageMTTR.toDouble()).isNaN
    }

    /**
     * test file: builds-for-MTTR-case-6.json
     * build 1, pipeline 1 : deploy to prod, SUCCESS, end at 2020-12-29
     * build 2, pipeline 1 : deploy to prod, FAILED, end at 2020-12-30 *
     * build 3, pipeline 1 : deploy to prod, OTHERS, end at 2020-12-31 *
     * build 4, pipeline 1 : deploy to prod, SUCCESS, end at 2021-01-01 *
     * build 5, pipeline 2 : deploy to prod, FAILED, end at 2021-01-02 *
     * build 6, pipeline 2 : deploy to prod, OTHERS, end at 2021-01-02 *
     * build 7, pipeline 2 : deploy to prod, SUCCESS, end at 2021-01-03 *
     */
    @Test
    fun `should return MTTR given all builds with one Aborted`() {
        val allExecutions: List<Execution> = objectMapper.readValue(
            this.javaClass.getResource("/calculator/builds-for-MTTR-case-6.json").readText()
        )

        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to prod"))

        val mttr = meanTimeToRestoreCalculator.calculateValue(allExecutions, startTimestamp, endTimestamp, targetStage)

        assertThat(mttr.toDouble() * MILLISECOND_TO_HOURS).isEqualTo(144000005.00)
    }

    /**
     * test file: builds-for-MTTR-case-7.json
     * build 1, pipeline 1 : deploy to prod, SUCCESS, end at 2020-12-29
     * build 2, pipeline 1 : deploy to prod, SUCCESS, end at 2020-12-30 *
     * build 3, pipeline 1 : deploy to prod, OTHERS, end at 2020-12-31 *
     * build 4, pipeline 1 : deploy to prod, SUCCESS, end at 2021-01-01 *
     * build 5, pipeline 2 : deploy to prod, OTHERS, end at 2021-01-02 *
     * build 6, pipeline 2 : deploy to prod, SUCCESS, end at 2021-01-03 *
     * build 7, pipeline 2 : deploy to prod, SUCCESS, end at 2021-01-05
     */
    @Test
    fun `should return MTTR given all builds with some Aborted and others success`() {
        val allExecutions: List<Execution> = objectMapper.readValue(
            this.javaClass.getResource("/calculator/builds-for-MTTR-case-7.json").readText()
        )

        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to prod"))

        val mttr = meanTimeToRestoreCalculator.calculateValue(allExecutions, startTimestamp, endTimestamp, targetStage)

        assertEquals(mttr, Double.NaN)
    }

    @Test
    fun `should return NO_VALUE given no builds`() {
        val startTimestamp = 1609459200000L // 2021.01.01
        val endTimestamp = 1609689600000L // 2021.01.04
        val targetStage = mapOf(Pair("1", "deploy to prod"), Pair("2", "deploy to prod"))

        val mttr = meanTimeToRestoreCalculator.calculateValue(emptyList(), startTimestamp, endTimestamp, targetStage)

        assertEquals(mttr, Double.NaN)
    }

    @Test
    fun `should return elite while MTTR less than 1 hour`() {
        val calculateLevel = meanTimeToRestoreCalculator.calculateLevel(0.5)

        assertThat(calculateLevel).isEqualTo(LEVEL.ELITE)
    }

    @Test
    fun `should return high while MTTR less than 24 hour and more than or equals 1 hour`() {
        val calculateLevel = meanTimeToRestoreCalculator.calculateLevel(0.999)

        assertThat(calculateLevel).isEqualTo(LEVEL.HIGH)
    }

    @Test
    fun `should return medium while MTTR less than 168 hour and more than or equals 24 hour`() {
        val calculateLevel = meanTimeToRestoreCalculator.calculateLevel(156.0)

        assertThat(calculateLevel).isEqualTo(LEVEL.MEDIUM)
    }

    @Test
    fun `should return low while MTTR more than or equals 168 hour`() {
        val calculateLevel = meanTimeToRestoreCalculator.calculateLevel(233.0)

        assertThat(calculateLevel).isEqualTo(LEVEL.LOW)
    }

    @Test
    fun `should return null while MTTR NaN`() {
        val calculateLevel = meanTimeToRestoreCalculator.calculateLevel(Double.NaN)

        assertThat(calculateLevel).isEqualTo(LEVEL.INVALID)
    }

    companion object {
        private const val MILLISECOND_TO_HOURS: Double = 3600000.0
    }
}

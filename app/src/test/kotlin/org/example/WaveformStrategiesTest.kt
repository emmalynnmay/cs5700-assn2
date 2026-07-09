package org.example

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class WaveformStrategyTest {

    private val tolerance = 1e-9

    // ================= SineStrategy =================

    @Test
    fun `sine at phase zero is zero`() {
        assertEquals(0.0, SineStrategy().doStrategy(0.0), tolerance)
    }

    @Test
    fun `sine at PI over two is one`() {
        assertEquals(1.0, SineStrategy().doStrategy(PI / 2), tolerance)
    }

    @Test
    fun `sine at PI is zero`() {
        assertEquals(0.0, SineStrategy().doStrategy(PI), tolerance)
    }

    @Test
    fun `sine at three PI over two is negative one`() {
        assertEquals(-1.0, SineStrategy().doStrategy(3 * PI / 2), tolerance)
    }

    @Test
    fun `sine at two PI wraps back to zero`() {
        assertEquals(0.0, SineStrategy().doStrategy(2 * PI), tolerance)
    }

    @Test
    fun `sine handles negative phase symmetrically`() {
        assertEquals(-1.0, SineStrategy().doStrategy(-PI / 2), tolerance)
    }

    @Test
    fun `sine handles phase far outside zero to two PI range`() {
        // sin is periodic, should behave the same as phase mod 2PI
        val farPhase = 100 * PI + PI / 2
        assertEquals(sin(farPhase), SineStrategy().doStrategy(farPhase), tolerance)
    }

    @Test
    fun `sine of NaN phase is NaN`() {
        assertTrue(SineStrategy().doStrategy(Double.NaN).isNaN())
    }

    @Test
    fun `sine of infinite phase is NaN`() {
        assertTrue(SineStrategy().doStrategy(Double.POSITIVE_INFINITY).isNaN())
        assertTrue(SineStrategy().doStrategy(Double.NEGATIVE_INFINITY).isNaN())
    }

    @Test
    fun `sine output is always within valid amplitude range`() {
        val phases = listOf(0.0, 0.1, PI, 2 * PI, -5.0, 1000.0)
        for (p in phases) {
            val result = SineStrategy().doStrategy(p)
            assertTrue(result in -1.0..1.0, "sine($p) = $result out of range")
        }
    }

    // ================= SquareStrategy =================

    @Test
    fun `square below PI is high`() {
        assertEquals(1.0, SquareStrategy().doStrategy(0.0))
        assertEquals(1.0, SquareStrategy().doStrategy(PI - 0.0001))
    }

    @Test
    fun `square at exactly PI is low due to strict less-than comparison`() {
        // phase < PI is false at exactly PI, so it falls into the else branch.
        assertEquals(-1.0, SquareStrategy().doStrategy(PI))
    }

    @Test
    fun `square above PI is low`() {
        assertEquals(-1.0, SquareStrategy().doStrategy(PI + 0.0001))
        assertEquals(-1.0, SquareStrategy().doStrategy(2 * PI))
    }

    @Test
    fun `square with negative phase is treated as less than PI and is high`() {
        // No wrapping/modulo logic exists, so any negative phase is unconditionally < PI.
        assertEquals(1.0, SquareStrategy().doStrategy(-1.0))
        assertEquals(1.0, SquareStrategy().doStrategy(-1000.0))
    }

    @Test
    fun `square with very large phase beyond one period is still governed by raw comparison to PI`() {
        // No periodicity: a large phase like 10*PI is NOT < PI, so it's low,
        // even though musically it should have wrapped back to a "high" region.
        assertEquals(-1.0, SquareStrategy().doStrategy(10 * PI))
    }

    @Test
    fun `square of NaN phase is low because NaN comparisons are false`() {
        // NaN < PI evaluates to false in Kotlin, so this falls into the else branch.
        assertEquals(-1.0, SquareStrategy().doStrategy(Double.NaN))
    }

    @Test
    fun `square of positive infinity is low`() {
        assertEquals(-1.0, SquareStrategy().doStrategy(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `square of negative infinity is high`() {
        assertEquals(1.0, SquareStrategy().doStrategy(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `square output is always exactly one or negative one`() {
        val phases = listOf(0.0, PI, 2 * PI, -5.0, 1000.0, Double.NaN)
        for (p in phases) {
            val result = SquareStrategy().doStrategy(p)
            assertTrue(result == 1.0 || result == -1.0, "square($p) = $result is not ±1")
        }
    }

    // ================= SawtoothStrategy =================

    @Test
    fun `sawtooth at phase zero is negative one`() {
        assertEquals(-1.0, SawtoothStrategy().doStrategy(0.0), tolerance)
    }

    @Test
    fun `sawtooth at PI is zero`() {
        assertEquals(0.0, SawtoothStrategy().doStrategy(PI), tolerance)
    }

    @Test
    fun `sawtooth at two PI is one`() {
        assertEquals(1.0, SawtoothStrategy().doStrategy(2 * PI), tolerance)
    }

    @Test
    fun `sawtooth with negative phase produces value below negative one`() {
        // No wrapping: phase = -PI -> -PI/PI - 1 = -2.0, outside the nominal -1..1 range.
        assertEquals(-2.0, SawtoothStrategy().doStrategy(-PI), tolerance)
    }

    @Test
    fun `sawtooth with phase beyond two PI produces value above one`() {
        // No wrapping: phase = 4*PI -> 4*PI/PI - 1 = 3.0, outside the nominal -1..1 range.
        assertEquals(3.0, SawtoothStrategy().doStrategy(4 * PI), tolerance)
    }

    @Test
    fun `sawtooth of NaN phase is NaN`() {
        assertTrue(SawtoothStrategy().doStrategy(Double.NaN).isNaN())
    }

    @Test
    fun `sawtooth of positive infinity is positive infinity`() {
        assertEquals(Double.POSITIVE_INFINITY, SawtoothStrategy().doStrategy(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `sawtooth of negative infinity is negative infinity`() {
        assertEquals(Double.NEGATIVE_INFINITY, SawtoothStrategy().doStrategy(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `sawtooth is linear and monotonically increasing within one period`() {
        val strategy = SawtoothStrategy()
        val a = strategy.doStrategy(PI / 4)
        val b = strategy.doStrategy(PI / 2)
        val c = strategy.doStrategy(3 * PI / 4)
        assertTrue(a < b && b < c, "expected strictly increasing sequence, got $a, $b, $c")
    }

    // ================= WhiteNoiseStrategy =================

    @Test
    fun `white noise output is always within negative one to one range`() {
        val strategy = WhiteNoiseStrategy()
        repeat(1000) {
            val result = strategy.doStrategy(0.0)
            assertTrue(result in -1.0..1.0, "white noise produced out-of-range value $result")
        }
    }

    @Test
    fun `white noise ignores the phase argument entirely`() {
        // Can't assert exact equality since it's random, but can assert the same call
        // repeated with wildly different phases doesn't error or bias toward a single value class.
        val strategy = WhiteNoiseStrategy()
        val resultsWithZeroPhase = List(500) { strategy.doStrategy(0.0) }
        val resultsWithHugePhase = List(500) { strategy.doStrategy(1_000_000.0) }
        val meanZero = resultsWithZeroPhase.average()
        val meanHuge = resultsWithHugePhase.average()
        // Both should hover near 0 (mean of uniform[-1,1]) regardless of phase input.
        assertTrue(meanZero in -0.2..0.2, "unexpected bias with phase=0.0: mean=$meanZero")
        assertTrue(meanHuge in -0.2..0.2, "unexpected bias with phase=1e6: mean=$meanHuge")
    }

    @Test
    fun `white noise handles NaN and infinite phase without throwing`() {
        val strategy = WhiteNoiseStrategy()
        // Since phase is ignored, these should simply not throw.
        strategy.doStrategy(Double.NaN)
        strategy.doStrategy(Double.POSITIVE_INFINITY)
        strategy.doStrategy(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun `white noise produces varying values across repeated calls`() {
        val strategy = WhiteNoiseStrategy()
        val results = List(50) { strategy.doStrategy(0.0) }
        assertFalse(results.all { it == results[0] }, "expected white noise to vary, got constant output")
    }
}
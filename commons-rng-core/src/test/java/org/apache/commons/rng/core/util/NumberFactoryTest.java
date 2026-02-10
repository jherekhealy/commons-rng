/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.rng.core.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link NumberFactory}.
 */
class NumberFactoryTest {
    /**
     * sizeof(int) in bytes.
     */
    private static final int INT_SIZE = Integer.BYTES;
    /**
     * sizeof(long) in bytes.
     */
    private static final int LONG_SIZE = Long.BYTES;

    /**
     * Test values.
     */
    private static final long[] LONG_TEST_VALUES = new long[]{0L, 1L, -1L, 19337L, 1234567891011213L,
        -11109876543211L, Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE,
        Long.MIN_VALUE, 0x9e3779b97f4a7c13L};
    /**
     * Test values.
     */
    private static final int[] INT_TEST_VALUES = new int[]{0, 1, -1, 19337, 1234567891, -1110987656,
        Integer.MAX_VALUE, Integer.MIN_VALUE, 0x9e3779b9};

    /**
     * Provide a stream of the test values for long conversion.
     *
     * @return the stream
     */
    static LongStream longTestValues() {
        return Arrays.stream(LONG_TEST_VALUES);
    }

    /**
     * Provide a stream of the test values for long conversion.
     *
     * @return the stream
     */
    static IntStream intTestValues() {
        return Arrays.stream(INT_TEST_VALUES);
    }

    /**
     * Assert that the value is close to but <strong>not above</strong> 1. This is used to test
     * the output from methods that produce a {@code float} value that must be in the range
     * between 0 and 1.
     *
     * @param value   the value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point values between x and y.
     * @see Precision#equals(float, float, int)
     */
    private static void assertCloseToNotAbove1(float value, int maxUlps) {
        Assertions.assertTrue(value <= 1.0f, "Not <= 1.0f");
        Assertions.assertTrue(Precision.equals(1.0f, value, maxUlps),
            () -> "Not equal to 1.0f within units of least precision: " + maxUlps);
    }

    /**
     * Assert that the value is close to but <strong>not above</strong> 1. This is used to test
     * the output from methods that produce a {@code double} value that must be in the range
     * between 0 and 1.
     *
     * @param value   the value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point values between x and y.
     * @see Precision#equals(double, double, int)
     */
    private static void assertCloseToNotAbove1(double value, int maxUlps) {
        Assertions.assertTrue(value <= 1.0, "Not <= 1.0");
        Assertions.assertTrue(Precision.equals(1.0, value, maxUlps),
            () -> "Not equal to 1.0 within units of least precision: " + maxUlps);
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testMakeBooleanFromInt(int v) {
        // Test if the bit is set differently then the booleans are opposite
        final boolean b1 = NumberFactory.makeBoolean(v);
        final boolean b2 = NumberFactory.makeBoolean(~v);
        Assertions.assertNotEquals(b1, b2);
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testMakeBooleanFromLong(long v) {
        // Test if the bit is set differently then the booleans are opposite
        final boolean b1 = NumberFactory.makeBoolean(v);
        final boolean b2 = NumberFactory.makeBoolean(~v);
        Assertions.assertNotEquals(b1, b2);
    }

    @Test
    void testMakeIntFromLong() {
        // Test the high order bits and low order bits are xor'd together
        Assertions.assertEquals(0xffffffff, NumberFactory.makeInt(0xffffffff00000000L));
        Assertions.assertEquals(0x00000000, NumberFactory.makeInt(0xffffffffffffffffL));
        Assertions.assertEquals(0xffffffff, NumberFactory.makeInt(0x00000000ffffffffL));
        Assertions.assertEquals(0x00000000, NumberFactory.makeInt(0x0000000000000000L));
        Assertions.assertEquals(0x0f0f0f0f, NumberFactory.makeInt(0x0f0f0f0f00000000L));
        Assertions.assertEquals(0xf0f0f0f0, NumberFactory.makeInt(0x00000000f0f0f0f0L));
        Assertions.assertEquals(0x00000000, NumberFactory.makeInt(0x0f0f0f0f0f0f0f0fL));
        Assertions.assertEquals(0xffffffff, NumberFactory.makeInt(0x0f0f0f0ff0f0f0f0L));
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testExtractLoExtractHi(long v) {
        final int vL = NumberFactory.extractLo(v);
        final int vH = NumberFactory.extractHi(v);

        final long actual = (((long) vH) << 32) | (vL & 0xffffffffL);
        Assertions.assertEquals(v, actual);
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLong2Long(long v) {
        final int vL = NumberFactory.extractLo(v);
        final int vH = NumberFactory.extractHi(v);

        Assertions.assertEquals(v, NumberFactory.makeLong(vH, vL));
    }

    @Test
    void testLongToByteArraySignificanceOrder() {
        // Start at the least significant bit
        long value = 1;
        for (int i = 0; i < LONG_SIZE; i++) {
            final byte[] b = NumberFactory.makeByteArray(value);
            for (int j = 0; j < LONG_SIZE; j++) {
                // Only one byte should be non zero
                Assertions.assertEquals(b[j] != 0, j == i);
            }
            // Shift to the next byte
            value <<= 8;
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLongToBytesIsLittleEndian(long v) {
        final ByteBuffer bb = ByteBuffer.allocate(LONG_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(v);
        Assertions.assertArrayEquals(bb.array(), NumberFactory.makeByteArray(v));
    }

    @RepeatedTest(value = 5)
    void testByteArrayToLongArrayIsLittleEndian() {
        final int n = 5;
        byte[] bytes = new byte[n * LONG_SIZE];
        ThreadLocalRandom.current().nextBytes(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final long[] data = NumberFactory.makeLongArray(bytes);
        for (int i = 0; i < n; i++) {
            Assertions.assertEquals(bb.getLong(), data[i]);
        }
        Assertions.assertArrayEquals(bytes, NumberFactory.makeByteArray(data));
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLongFromByteArray2Long(long expected) {
        final byte[] b = NumberFactory.makeByteArray(expected);
        Assertions.assertEquals(expected, NumberFactory.makeLong(b));
    }

    @Test
    void testLongArrayFromByteArray2LongArray() {
        final byte[] b = NumberFactory.makeByteArray(LONG_TEST_VALUES);
        Assertions.assertArrayEquals(LONG_TEST_VALUES, NumberFactory.makeLongArray(b));
    }

    @ParameterizedTest
    @MethodSource(value = {"longTestValues"})
    void testLongArrayToByteArrayMatchesLongToByteArray(long v) {
        // Test individually the bytes are the same as the array conversion
        final byte[] b1 = NumberFactory.makeByteArray(v);
        final byte[] b2 = NumberFactory.makeByteArray(new long[]{v});
        Assertions.assertArrayEquals(b1, b2);
    }

    @Test
    void testIntToByteArraySignificanceOrder() {
        // Start at the least significant bit
        int value = 1;
        for (int i = 0; i < INT_SIZE; i++) {
            final byte[] b = NumberFactory.makeByteArray(value);
            for (int j = 0; j < INT_SIZE; j++) {
                // Only one byte should be non zero
                Assertions.assertEquals(b[j] != 0, j == i);
            }
            // Shift to the next byte
            value <<= 8;
        }
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testIntToBytesIsLittleEndian(int v) {
        final ByteBuffer bb = ByteBuffer.allocate(INT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(v);
        Assertions.assertArrayEquals(bb.array(), NumberFactory.makeByteArray(v));
    }

    @RepeatedTest(value = 5)
    void testByteArrayToIntArrayIsLittleEndian() {
        final int n = 5;
        byte[] bytes = new byte[n * INT_SIZE];
        ThreadLocalRandom.current().nextBytes(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final int[] data = NumberFactory.makeIntArray(bytes);
        for (int i = 0; i < n; i++) {
            Assertions.assertEquals(bb.getInt(), data[i]);
        }
        Assertions.assertArrayEquals(bytes, NumberFactory.makeByteArray(data));
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testIntFromByteArray2Int(int expected) {
        final byte[] b = NumberFactory.makeByteArray(expected);
        Assertions.assertEquals(expected, NumberFactory.makeInt(b));
    }

    @Test
    void testIntArrayFromByteArray2IntArray() {
        final byte[] b = NumberFactory.makeByteArray(INT_TEST_VALUES);
        Assertions.assertArrayEquals(INT_TEST_VALUES, NumberFactory.makeIntArray(b));
    }

    @ParameterizedTest
    @MethodSource(value = {"intTestValues"})
    void testIntArrayToByteArrayMatchesIntToByteArray(int v) {
        // Test individually the bytes are the same as the array conversion
        final byte[] b1 = NumberFactory.makeByteArray(v);
        final byte[] b2 = NumberFactory.makeByteArray(new int[]{v});
        Assertions.assertArrayEquals(b1, b2);
    }

    @Test
    void testMakeIntPrecondition1() {
        for (int i = 0; i <= 10; i++) {
            final byte[] bytes = new byte[i];
            if (i != INT_SIZE) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeInt(bytes));
            } else {
                Assertions.assertEquals(0, NumberFactory.makeInt(bytes));
            }
        }
    }

    @Test
    void testMakeIntArrayPrecondition1() {
        for (int i = 0; i <= 20; i++) {
            final byte[] bytes = new byte[i];
            if (i != 0 && i % INT_SIZE != 0) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeIntArray(bytes));
            } else {
                Assertions.assertArrayEquals(new int[i / INT_SIZE], NumberFactory.makeIntArray(bytes));
            }
        }
    }

    @Test
    void testMakeLongPrecondition1() {
        for (int i = 0; i <= 10; i++) {
            final byte[] bytes = new byte[i];
            if (i != LONG_SIZE) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeLong(bytes));
            } else {
                Assertions.assertEquals(0L, NumberFactory.makeLong(bytes));
            }
        }
    }

    @Test
    void testMakeLongArrayPrecondition1() {
        for (int i = 0; i <= 20; i++) {
            final byte[] bytes = new byte[i];
            if (i != 0 && i % LONG_SIZE != 0) {
                Assertions.assertThrows(IllegalArgumentException.class,
                    () -> NumberFactory.makeLongArray(bytes));
            } else {
                Assertions.assertArrayEquals(new long[i / LONG_SIZE], NumberFactory.makeLongArray(bytes));
            }
        }
    }

    /**
     * Test different methods for generation of a {@code float} from a {@code int}. The output
     * value should be in the range between 0 and 1.
     */
    @Test
    void testFloatGenerationMethods() {
        final int allBits = 0xffffffff;

        // Not capable of generating 1. Set the delta with 1 or 2 ULP of 1.
        assertCloseToNotAbove1((allBits >>> 9) * 0x1.0p-23f, 2);
        assertCloseToNotAbove1((allBits >>> 8) * 0x1.0p-24f, 1);
        assertCloseToNotAbove1(Float.intBitsToFloat(0x7f << 23 | allBits >>> 9) - 1.0f, 2);

        final int noBits = 0;
        Assertions.assertEquals(0.0f, (noBits >>> 9) * 0x1.0p-23f);
        Assertions.assertEquals(0.0f, (noBits >>> 8) * 0x1.0p-24f);
        Assertions.assertEquals(0.0f, Float.intBitsToFloat(0x7f << 23 | noBits >>> 9) - 1.0f);
    }

    /**
     * Test different methods for generation of a {@code double} from a {@code long}. The output
     * value should be in the range between 0 and 1.
     */
    @Test
    void testDoubleGenerationMethods() {
        final long allBits = 0xffffffffffffffffL;

        // Not capable of generating 1. Set the delta with 1 or 2 ULP of 1.
        assertCloseToNotAbove1((allBits >>> 12) * 0x1.0p-52d, 2);
        assertCloseToNotAbove1((allBits >>> 11) * 0x1.0p-53d, 1);
        assertCloseToNotAbove1(Double.longBitsToDouble(0x3ffL << 52 | allBits >>> 12) - 1.0, 2);

        final long noBits = 0;
        Assertions.assertEquals(0.0, (noBits >>> 12) * 0x1.0p-52d);
        Assertions.assertEquals(0.0, (noBits >>> 11) * 0x1.0p-53d);
        Assertions.assertEquals(0.0, Double.longBitsToDouble(0x3ffL << 52 | noBits >>> 12) - 1.0);
    }

    @Test
    void testMakeDoubleFromLong() {
        final long allBits = 0xffffffffffffffffL;
        final long noBits = 0;
        // Within 1 ULP of 1.0
        assertCloseToNotAbove1(NumberFactory.makeDouble(allBits), 1);
        Assertions.assertEquals(0.0, NumberFactory.makeDouble(noBits));
    }

    @Test
    void testMakeDoubleFromIntInt() {
        final int allBits = 0xffffffff;
        final int noBits = 0;
        // Within 1 ULP of 1.0
        assertCloseToNotAbove1(NumberFactory.makeDouble(allBits, allBits), 1);
        Assertions.assertEquals(0.0, NumberFactory.makeDouble(noBits, noBits));
    }

    @Test
    void testMakeFloatFromInt() {
        final int allBits = 0xffffffff;
        final int noBits = 0;
        // Within 1 ULP of 1.0f
        assertCloseToNotAbove1(NumberFactory.makeFloat(allBits), 1);
        Assertions.assertEquals(0.0f, NumberFactory.makeFloat(noBits));
    }

    @Test
    void testDoubleOpenFromLong() {
        long[] theLongs = new long[]{0xffffffffffffffffL, 0x0L, 0x1234567890abcdefL};
        double[] theValues = new double[theLongs.length];
        for (int i = 0; i < theLongs.length; i++) {
            theValues[i] = NumberFactory.makeDoubleOpen(theLongs[i]);
        }
        for (double theValue : theValues) {
            Assertions.assertNotEquals(1.0, theValue);
            Assertions.assertNotEquals(0.0, theValue);
            Assertions.assertTrue(theValue > 0.0);
            Assertions.assertTrue(theValue < 1.0);
        }
        assertCloseToNotAbove1(theValues[0], 1);
        Assertions.assertEquals(0x1.0p-53d, theValues[1]);
    }

    @Test
    void testDoubleOpenFromLongAgainstMT19937x64() {
        final long[] theLongsRandom = new long[]{0xA3F1C9D27B4E8F10L, 0x6D2A7C5E91B3F4C8L, 0xF0E1D2C3B4A59687L,
            0x1B9E3A7C5D2F8E40L, 0x9C4D1A2B7E8F3C65L, 0x07F3B8D4C1A29E6BL, 0xD8A1F0C37B5E2D94L, 0x2E7C9A4D1F3B6C80L,
            0xB5C2E8F1A3479D60L, 0x4A9D6C3B2F1E8A75L, 0xC1F07A9E3D2B4C68L, 0x8E3B5D1A7C9F2460L, 0x13579BDF2468ACE0L,
            0xFEDCBA9876543210L, 0x0F1E2D3C4B5A6978L, 0xABCDEF0123456789L, 0x7645B3D29A1F0C8EL, 0xE2C4A6F8D1B3907CL,
            0x39A7C5E1F2D4B680L, 0x5F8E1C3A7D2B4960L, 0x91A2B3C4D5E6F708L, 0xC7D8E9FA0B1C2D3EL, 0x2B4D6F8091A3C5E7L,
            0x8C0D1E2F3A4B5C6DL, 0xF1A3B5C7D9E0F246L, 0x6A8C0E1F2B3D4F50L, 0x3D5F7A9C1E2B4D68L, 0xB0C2D4E6F8091A3CL,
            0x4F6D8B0A1C3E5F72L, 0xDA7C5E3B1F092846L, 0x17C9E3A5D7F1B260L, 0xE8F6D4C2B0A98765L, 0x9A0C1E2D3F4B5A68L,
            0x5C7E9A1B3D4F6082L, 0xC3E5F7091A2B4D6FL, 0x0A2C4E6F8B1D3F59L, 0x7B9D1F3A5C6E8092L, 0xE4F2D0C1B3A59687L,
            0x21C3E5A7D9F0B468L, 0x8F1D2B3C4A596E70L, 0xD2F4A6C8E0B19375L, 0x6E0F1A2C3D4B5F98L, 0xB9D7F5E3C1A28460L,
            0x4C6A8E0D1F2B3957L, 0xF8D6B4A2901C3E75L, 0x13A5C7E9F0B2D468L, 0xA7C9E1F3B5D70826L, 0x5E7A9C0D2F4B6D81L,
            0xC0FFEE1234AB5678L, 0xDEADBEEFCAFEBABEL, 0x1029384756ABCDEFL, 0x89ABCDEF01234567L, 0x55AA55AA33CC33CCL,
            0xAA55AA55CC33CC33L, 0x0C0D0E0F1A1B1C1DL, 0xFEE1DEADBEAD1234L, 0x3141592653589793L, 0x2718281828459045L,
            0x9E3779B97F4A7C15L, 0xC6A4A7935BD1E995L, 0xD1B54A32D192ED03L, 0x94D049BB133111EBL, 0x8538ECB5BD456EA3L,
            0xDA942042E4DD58B5L};

        final long[] theLongsSpecial = new long[]{0x0000000000000000L, 0x0000000000000001L, 0x0000000000000002L,
            0x0000000000000007L, 0x0000000000000008L, 0x000000000000000FL, 0x0000000000000010L, 0x00000000000007FFL,
            0x0000000000000800L, 0x0000000000000801L, 0x0000000000000FFFL, 0x0000000000001000L, 0x0000000000001001L,
            0x0000000000007FFFL, 0x0000000000008000L, 0x0000000000008001L, 0x000000000007FFFFL, 0x0000000000080000L,
            0x0000000000080001L, 0x00000000000FFFFFL, 0x0000000000100000L, 0x0000000000100001L, 0x0000000007FFFFFFL,
            0x0000000008000000L, 0x0000000008000001L, 0x000000000FFFFFFFL, 0x0000000010000000L, 0x0000000010000001L,
            0x000FFFFFFFFFFFFFL, 0x0010000000000000L, 0x0010000000000001L, 0x001FFFFFFFFFFFFFL, 0x0020000000000000L,
            0x0020000000000001L, 0x003FFFFFFFFFFFFFL, 0x0040000000000000L, 0x0040000000000001L, 0x007FFFFFFFFFFFFFL,
            0x0080000000000000L, 0x0080000000000001L, 0x00FFFFFFFFFFFFFFL, 0x0100000000000000L, 0x0100000000000001L,
            0x3FFFFFFFFFFFFFFFL, 0x4000000000000000L, 0x4000000000000001L, 0x7FFFFFFFFFFFFFFEL, 0x7FFFFFFFFFFFFFFFL,
            0x8000000000000000L, 0x8000000000000001L, 0x8000000000000800L, 0xFFFFFFFFFFFFFFFEL, 0xFFFFFFFFFFFFFFFFL,
            0xAAAAAAAAAAAAAAAAL, 0x5555555555555555L, 0xAAAAAAAAAAAAAAABL, 0x5555555555555554L, 0x000FFFFFFFFFF800L,
            0x000FFFFFFFFFF801L, 0x00100000000007FFL, 0x0010000000000800L};
        final long[][] allLongs = new long[][]{theLongsRandom, theLongsSpecial};
        for (long[] theLongs : allLongs) {
            for (long theLong : theLongs) {
                double theValue = NumberFactory.makeDoubleOpen(theLong);
                double refValue = genrand64_real3(theLong); //mt19937-64.c implementation
                Assertions.assertEquals(refValue, theValue, 0);
            }
        }
        Assertions.assertNotEquals( NumberFactory.makeDoubleOpen(allLongs[0][0]), genrand64_fast_res52_open(allLongs[0][0]));
    }

    private static double genrand64_real3(long random64) {
        //as in Takuji Nishimura and Makoto Matsumoto mt19937-64.c
       return ((random64 >>> 12) + 0.5) * (1.0 / 4503599627370496.0);
    }

    private static double genrand64_fast_res52_open(long random64) {
        //as in Shin Harase and Takamitsu Kimoto melg607-64.c
        long bits = (random64 >>> 12) | 0x3FF0000000000001L;
        return Double.longBitsToDouble(bits) - 1.0;
    }

    @Test
    void testDoubleOpenFromIntegers() {
        long[] theLongs = new long[]{0xffffffffffffffffL, 0x0L, 0x1234567890abcdefL};
        double[] theValues = new double[theLongs.length];
        for (int i = 0; i < theLongs.length; i++) {
            theValues[i] = NumberFactory.makeDoubleOpen((int) (theLongs[i] >>> 32), (int) theLongs[i]);
        }
        for (double theValue : theValues) {
            Assertions.assertNotEquals(1.0, theValue);
            Assertions.assertNotEquals(0.0, theValue);
            Assertions.assertTrue(theValue > 0.0);
            Assertions.assertTrue(theValue < 1.0);
        }
        Assertions.assertEquals(1.0, theValues[0], Math.pow(2, -21)); // the xor will distort value after 20 bits.
        Assertions.assertEquals(0x1.0p-53d, theValues[1]);
    }
}

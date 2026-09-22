package com.securevault.core.crypto

import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class SecretGeneratorTest {
    @Test fun passphraseRequestsExactly32BytesFromSecureSource() {
        val source = RecordingSecureRandom()
        val result = SecretGenerator(source).generatePassphrase()

        assertEquals(32, result.size)
        assertEquals(listOf(32), source.requestedSizes)
        assertArrayEquals(ByteArray(32) { 0x5a.toByte() }, result)
    }

    @Test fun eachRequestFillsANewIndependentBuffer() {
        val source = RecordingSecureRandom()
        val generator = SecretGenerator(source)
        val first = generator.generatePassphrase()
        val second = generator.generatePassphrase()

        assertEquals(listOf(32, 32), source.requestedSizes)
        assertNotSame(first, second)
        // Identical fake output is intentional: we test allocation, not probabilistic uniqueness.
        assertArrayEquals(first, second)
        first.fill(0)
        assertArrayEquals(ByteArray(32) { 0x5a.toByte() }, second)
    }

    @Test fun byteRequestsUseTheRequestedLength() {
        val source = RecordingSecureRandom()
        assertEquals(16, SecretGenerator(source).generateBytes(16).size)
        assertEquals(listOf(16), source.requestedSizes)
    }

    @Test fun invalidLengthsFailBeforeConsultingTheSource() {
        val source = RecordingSecureRandom()
        val generator = SecretGenerator(source)
        for (length in listOf(0, -1, Int.MIN_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { generator.generateBytes(length) }
        }
        assertTrue(source.requestedSizes.isEmpty())
    }

    @Test fun sourceFailurePropagatesWithoutFallback() {
        val failure = IllegalStateException("Random source unavailable")
        val source = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) { throw failure }
        }
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            SecretGenerator(source).generatePassphrase()
        })
    }
}

/** Deterministic source confined to tests; it does not measure randomness quality. */
internal class RecordingSecureRandom(private val fill: Byte = 0x5a.toByte()) : SecureRandom() {
    val requestedSizes = mutableListOf<Int>()
    override fun nextBytes(bytes: ByteArray) {
        requestedSizes.add(bytes.size)
        bytes.fill(fill)
    }
}

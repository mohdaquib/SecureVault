package com.securevault.core.crypto

sealed class CryptoException(
    message: String,
) : Exception(message)

class KeyGenerationException(
    cause: Throwable,
) : CryptoException("Failed to generate encryption key")

class KeyRetrievalException(
    cause: Throwable,
) : CryptoException("Failed to retrieve encryption key")

class KeyInvalidatedException : CryptoException("Encryption key invalidated (device security changed)")

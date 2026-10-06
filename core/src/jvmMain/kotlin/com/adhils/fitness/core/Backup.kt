package com.adhils.fitness.core

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object Backup {
    private val magic = "ADHFIT01".toByteArray()
    private const val ITERATIONS = 210000
    private const val MAX = 16 * 1024 * 1024
    fun encrypt(state: ProfileStore, password: CharArray): ByteArray {
        require(password.size >= 10) { "Use at least 10 characters for the backup password." }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        // Device-local content URIs are not portable.
        val payload = AppJson.encodeToString(state.validated().copy(profiles = state.profiles.map { it.copy(state = it.state.copy(videos = emptyMap())) })).toByteArray()
        require(payload.size < MAX)
        return magic + salt + nonce + cipher.doFinal(payload)
    }
    fun decrypt(bytes: ByteArray, password: CharArray): ProfileStore {
        require(bytes.size in 52..MAX && bytes.take(8).toByteArray().contentEquals(magic)) { "Not a supported ADhils backup." }
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(8)
        val salt = ByteArray(16).also(buffer::get)
        val nonce = ByteArray(12).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        return AppJson.decodeFromString<ProfileStore>(cipher.doFinal(encrypted).toString(Charsets.UTF_8)).validated()
    }
    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES") }
        finally { spec.clearPassword() }
    }
}

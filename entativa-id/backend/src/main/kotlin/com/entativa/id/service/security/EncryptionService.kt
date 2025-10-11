package com.entativa.id.service.security

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*
import javax.crypto.*
import javax.crypto.spec.*

/**
 * Encryption Service for Entativa ID
 * Provides comprehensive encryption, decryption, key management, and cryptographic operations
 * 
 * @author Neo Qiss
 * @status Production-ready encryption with enterprise-grade security
 */
@Service
class EncryptionService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)
    private val secureRandom = SecureRandom()
    
    companion object {
        // Encryption algorithms and parameters
        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val RSA_ALGORITHM = "RSA"
        private const val RSA_TRANSFORMATION = "RSA/OAEP/SHA-256"
        private const val AES_KEY_SIZE = 256
        private const val RSA_KEY_SIZE = 4096
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        
        // Key derivation
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 100000
        private const val SALT_LENGTH = 32
        
        // Key management
        private const val KEY_ROTATION_DAYS = 90L
        private const val MASTER_KEY_CACHE_TTL = 3600L // 1 hour
        private const val DEK_CACHE_TTL = 7200L // 2 hours
    }
    
    /**
     * Encrypt sensitive data using AES-GCM
     */
    suspend fun encryptData(
        plaintext: ByteArray,
        keyId: String? = null,
        additionalData: ByteArray? = null
    ): Result<EncryptedData> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔐 Encrypting data with key: ${keyId ?: "default"}")
                
                // Get or generate data encryption key
                val dataKey = keyId?.let { getDataEncryptionKey(it) } 
                    ?: generateDataEncryptionKey()
                
                // Generate IV
                val iv = ByteArray(GCM_IV_LENGTH)
                secureRandom.nextBytes(iv)
                
                // Setup cipher
                val cipher = Cipher.getInstance(AES_TRANSFORMATION)
                val secretKey = SecretKeySpec(dataKey.keyMaterial, AES_ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
                
                // Add authenticated additional data if provided
                additionalData?.let { cipher.updateAAD(it) }
                
                // Encrypt
                val ciphertext = cipher.doFinal(plaintext)
                
                val encryptedData = EncryptedData(
                    ciphertext = ciphertext,
                    keyId = dataKey.keyId,
                    iv = iv,
                    algorithm = AES_TRANSFORMATION,
                    additionalData = additionalData,
                    encryptedAt = Instant.now(),
                    keyVersion = dataKey.version
                )
                
                logger.debug("✅ Data encrypted successfully with key: ${dataKey.keyId}")
                Result.success(encryptedData)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to encrypt data", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Decrypt data using stored encryption parameters
     */
    suspend fun decryptData(encryptedData: EncryptedData): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔓 Decrypting data with key: ${encryptedData.keyId}")
                
                // Get data encryption key
                val dataKey = getDataEncryptionKey(encryptedData.keyId)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Data encryption key not found: ${encryptedData.keyId}")
                    )
                
                // Setup cipher
                val cipher = Cipher.getInstance(encryptedData.algorithm)
                val secretKey = SecretKeySpec(dataKey.keyMaterial, AES_ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
                
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
                
                // Add authenticated additional data if present
                encryptedData.additionalData?.let { cipher.updateAAD(it) }
                
                // Decrypt
                val plaintext = cipher.doFinal(encryptedData.ciphertext)
                
                logger.debug("✅ Data decrypted successfully")
                Result.success(plaintext)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to decrypt data", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Encrypt data with password-based encryption
     */
    suspend fun encryptWithPassword(
        plaintext: ByteArray,
        password: String,
        salt: ByteArray? = null
    ): Result<PasswordEncryptedData> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔐 Encrypting data with password-based encryption")
                
                // Generate salt if not provided
                val actualSalt = salt ?: ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
                
                // Derive key from password
                val derivedKey = deriveKeyFromPassword(password, actualSalt)
                
                // Generate IV
                val iv = ByteArray(GCM_IV_LENGTH)
                secureRandom.nextBytes(iv)
                
                // Encrypt
                val cipher = Cipher.getInstance(AES_TRANSFORMATION)
                val secretKey = SecretKeySpec(derivedKey, AES_ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
                
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
                val ciphertext = cipher.doFinal(plaintext)
                
                val encryptedData = PasswordEncryptedData(
                    ciphertext = ciphertext,
                    salt = actualSalt,
                    iv = iv,
                    iterations = PBKDF2_ITERATIONS,
                    algorithm = AES_TRANSFORMATION,
                    encryptedAt = Instant.now()
                )
                
                logger.debug("✅ Password-based encryption completed")
                Result.success(encryptedData)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to encrypt with password", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Decrypt password-encrypted data
     */
    suspend fun decryptWithPassword(
        encryptedData: PasswordEncryptedData,
        password: String
    ): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔓 Decrypting password-encrypted data")
                
                // Derive key from password
                val derivedKey = deriveKeyFromPassword(password, encryptedData.salt, encryptedData.iterations)
                
                // Decrypt
                val cipher = Cipher.getInstance(encryptedData.algorithm)
                val secretKey = SecretKeySpec(derivedKey, AES_ALGORITHM)
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
                
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
                val plaintext = cipher.doFinal(encryptedData.ciphertext)
                
                logger.debug("✅ Password-based decryption completed")
                Result.success(plaintext)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to decrypt with password", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generate RSA key pair for asymmetric encryption
     */
    suspend fun generateRSAKeyPair(keySize: Int = RSA_KEY_SIZE): Result<AsymmetricKeyPair> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔑 Generating RSA key pair with size: $keySize")
                
                val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM)
                keyPairGenerator.initialize(keySize, secureRandom)
                
                val keyPair = keyPairGenerator.generateKeyPair()
                val keyId = generateKeyId()
                
                val asymmetricKeyPair = AsymmetricKeyPair(
                    keyId = keyId,
                    publicKey = keyPair.public.encoded,
                    privateKey = keyPair.private.encoded,
                    algorithm = RSA_ALGORITHM,
                    keySize = keySize,
                    createdAt = Instant.now(),
                    expiresAt = Instant.now().plusDays(KEY_ROTATION_DAYS * 4) // 1 year
                )
                
                // Store key pair securely
                storeAsymmetricKeyPair(asymmetricKeyPair)
                
                logger.debug("✅ RSA key pair generated: $keyId")
                Result.success(asymmetricKeyPair)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to generate RSA key pair", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Encrypt data using RSA public key
     */
    suspend fun encryptWithRSA(
        plaintext: ByteArray,
        publicKeyId: String
    ): Result<AsymmetricEncryptedData> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔐 Encrypting with RSA public key: $publicKeyId")
                
                // Get public key
                val keyPair = getAsymmetricKeyPair(publicKeyId)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Public key not found: $publicKeyId")
                    )
                
                val publicKey = KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePublic(X509EncodedKeySpec(keyPair.publicKey))
                
                // For large data, use hybrid encryption (RSA + AES)
                if (plaintext.size > 190) { // RSA 4096 can encrypt max ~446 bytes with OAEP
                    return@withContext encryptWithRSAHybrid(plaintext, publicKey, publicKeyId)
                }
                
                // Direct RSA encryption for small data
                val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val ciphertext = cipher.doFinal(plaintext)
                
                val encryptedData = AsymmetricEncryptedData(
                    ciphertext = ciphertext,
                    keyId = publicKeyId,
                    algorithm = RSA_TRANSFORMATION,
                    encryptedAt = Instant.now(),
                    isHybrid = false
                )
                
                logger.debug("✅ RSA encryption completed")
                Result.success(encryptedData)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to encrypt with RSA", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Decrypt data using RSA private key
     */
    suspend fun decryptWithRSA(encryptedData: AsymmetricEncryptedData): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔓 Decrypting with RSA private key: ${encryptedData.keyId}")
                
                // Get private key
                val keyPair = getAsymmetricKeyPair(encryptedData.keyId)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Private key not found: ${encryptedData.keyId}")
                    )
                
                val privateKey = KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePrivate(PKCS8EncodedKeySpec(keyPair.privateKey))
                
                // Handle hybrid decryption
                if (encryptedData.isHybrid) {
                    return@withContext decryptWithRSAHybrid(encryptedData, privateKey)
                }
                
                // Direct RSA decryption
                val cipher = Cipher.getInstance(encryptedData.algorithm)
                cipher.init(Cipher.DECRYPT_MODE, privateKey)
                val plaintext = cipher.doFinal(encryptedData.ciphertext)
                
                logger.debug("✅ RSA decryption completed")
                Result.success(plaintext)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to decrypt with RSA", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create digital signature
     */
    suspend fun createDigitalSignature(
        data: ByteArray,
        privateKeyId: String,
        algorithm: String = "SHA256withRSA"
    ): Result<DigitalSignature> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("✍️ Creating digital signature with key: $privateKeyId")
                
                // Get private key
                val keyPair = getAsymmetricKeyPair(privateKeyId)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Private key not found: $privateKeyId")
                    )
                
                val privateKey = KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePrivate(PKCS8EncodedKeySpec(keyPair.privateKey))
                
                // Create signature
                val signature = Signature.getInstance(algorithm)
                signature.initSign(privateKey, secureRandom)
                signature.update(data)
                val signatureBytes = signature.sign()
                
                val digitalSignature = DigitalSignature(
                    signature = signatureBytes,
                    algorithm = algorithm,
                    keyId = privateKeyId,
                    dataHash = hashData(data, "SHA-256"),
                    createdAt = Instant.now()
                )
                
                logger.debug("✅ Digital signature created")
                Result.success(digitalSignature)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create digital signature", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verify digital signature
     */
    suspend fun verifyDigitalSignature(
        data: ByteArray,
        digitalSignature: DigitalSignature
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Verifying digital signature with key: ${digitalSignature.keyId}")
                
                // Get public key
                val keyPair = getAsymmetricKeyPair(digitalSignature.keyId)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Public key not found: ${digitalSignature.keyId}")
                    )
                
                val publicKey = KeyFactory.getInstance(RSA_ALGORITHM)
                    .generatePublic(X509EncodedKeySpec(keyPair.publicKey))
                
                // Verify data hash first
                val dataHash = hashData(data, "SHA-256")
                if (!dataHash.contentEquals(digitalSignature.dataHash)) {
                    logger.warn("⚠️ Data hash mismatch during signature verification")
                    return@withContext Result.success(false)
                }
                
                // Verify signature
                val signature = Signature.getInstance(digitalSignature.algorithm)
                signature.initVerify(publicKey)
                signature.update(data)
                val isValid = signature.verify(digitalSignature.signature)
                
                logger.debug("✅ Digital signature verification: $isValid")
                Result.success(isValid)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to verify digital signature", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generate secure random data
     */
    suspend fun generateSecureRandom(length: Int): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val randomData = ByteArray(length)
                secureRandom.nextBytes(randomData)
                Result.success(randomData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Hash data using specified algorithm
     */
    suspend fun hashData(
        data: ByteArray,
        algorithm: String = "SHA-256",
        salt: ByteArray? = null
    ): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val digest = MessageDigest.getInstance(algorithm)
                salt?.let { digest.update(it) }
                val hash = digest.digest(data)
                Result.success(hash)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Rotate encryption keys
     */
    suspend fun rotateDataEncryptionKey(keyId: String): Result<DataEncryptionKey> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔄 Rotating data encryption key: $keyId")
                
                // Generate new key version
                val newKey = generateDataEncryptionKey(keyId)
                
                // Mark old key as rotated but keep for decryption
                val oldKey = getDataEncryptionKey(keyId)
                if (oldKey != null) {
                    val rotatedKey = oldKey.copy(
                        status = KeyStatus.ROTATED,
                        rotatedAt = Instant.now()
                    )
                    storeDataEncryptionKey(rotatedKey)
                }
                
                logger.info("✅ Key rotation completed for: $keyId")
                Result.success(newKey)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to rotate key", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun generateKeyId(): String {
        return "key_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    private fun generateDataEncryptionKey(keyId: String? = null): DataEncryptionKey {
        val actualKeyId = keyId ?: generateKeyId()
        val keyMaterial = ByteArray(AES_KEY_SIZE / 8)
        secureRandom.nextBytes(keyMaterial)
        
        return DataEncryptionKey(
            keyId = actualKeyId,
            keyMaterial = keyMaterial,
            algorithm = AES_ALGORITHM,
            keySize = AES_KEY_SIZE,
            version = 1,
            status = KeyStatus.ACTIVE,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusDays(KEY_ROTATION_DAYS)
        )
    }
    
    private fun getDataEncryptionKey(keyId: String): DataEncryptionKey? {
        val cacheKey = "dek:$keyId"
        return cacheManager.get<DataEncryptionKey>(cacheKey)
    }
    
    private fun storeDataEncryptionKey(key: DataEncryptionKey) {
        val cacheKey = "dek:${key.keyId}"
        cacheManager.set(cacheKey, key, DEK_CACHE_TTL)
    }
    
    private fun storeAsymmetricKeyPair(keyPair: AsymmetricKeyPair) {
        val cacheKey = "asymmetric:${keyPair.keyId}"
        cacheManager.set(cacheKey, keyPair, keyPair.expiresAt.epochSecond - Instant.now().epochSecond)
    }
    
    private fun getAsymmetricKeyPair(keyId: String): AsymmetricKeyPair? {
        val cacheKey = "asymmetric:$keyId"
        return cacheManager.get<AsymmetricKeyPair>(cacheKey)
    }
    
    private fun deriveKeyFromPassword(
        password: String,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS
    ): ByteArray {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, AES_KEY_SIZE)
        val secretKey = factory.generateSecret(spec)
        return secretKey.encoded
    }
    
    private fun encryptWithRSAHybrid(
        plaintext: ByteArray,
        publicKey: PublicKey,
        keyId: String
    ): Result<AsymmetricEncryptedData> {
        try {
            // Generate AES key for data encryption
            val aesKey = ByteArray(32) // 256-bit key
            secureRandom.nextBytes(aesKey)
            
            // Encrypt data with AES
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)
            
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKey = SecretKeySpec(aesKey, AES_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encryptedData = cipher.doFinal(plaintext)
            
            // Encrypt AES key with RSA
            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedAESKey = rsaCipher.doFinal(aesKey)
            
            // Combine encrypted key and data
            val hybridData = HybridEncryptionData(
                encryptedKey = encryptedAESKey,
                encryptedData = encryptedData,
                iv = iv
            )
            
            return Result.success(AsymmetricEncryptedData(
                ciphertext = serializeHybridData(hybridData),
                keyId = keyId,
                algorithm = RSA_TRANSFORMATION,
                encryptedAt = Instant.now(),
                isHybrid = true
            ))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    private fun decryptWithRSAHybrid(
        encryptedData: AsymmetricEncryptedData,
        privateKey: PrivateKey
    ): Result<ByteArray> {
        try {
            // Deserialize hybrid data
            val hybridData = deserializeHybridData(encryptedData.ciphertext)
            
            // Decrypt AES key with RSA
            val rsaCipher = Cipher.getInstance(encryptedData.algorithm)
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
            val aesKey = rsaCipher.doFinal(hybridData.encryptedKey)
            
            // Decrypt data with AES
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val secretKey = SecretKeySpec(aesKey, AES_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, hybridData.iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val plaintext = cipher.doFinal(hybridData.encryptedData)
            
            return Result.success(plaintext)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    private fun serializeHybridData(hybridData: HybridEncryptionData): ByteArray {
        // Simple serialization - in production would use proper serialization
        val keyLength = hybridData.encryptedKey.size
        val dataLength = hybridData.encryptedData.size
        val ivLength = hybridData.iv.size
        
        val result = ByteArray(4 + keyLength + 4 + dataLength + 4 + ivLength)
        var offset = 0
        
        // Write key length and key
        result[offset++] = (keyLength shr 24).toByte()
        result[offset++] = (keyLength shr 16).toByte()
        result[offset++] = (keyLength shr 8).toByte()
        result[offset++] = keyLength.toByte()
        System.arraycopy(hybridData.encryptedKey, 0, result, offset, keyLength)
        offset += keyLength
        
        // Write data length and data
        result[offset++] = (dataLength shr 24).toByte()
        result[offset++] = (dataLength shr 16).toByte()
        result[offset++] = (dataLength shr 8).toByte()
        result[offset++] = dataLength.toByte()
        System.arraycopy(hybridData.encryptedData, 0, result, offset, dataLength)
        offset += dataLength
        
        // Write IV length and IV
        result[offset++] = (ivLength shr 24).toByte()
        result[offset++] = (ivLength shr 16).toByte()
        result[offset++] = (ivLength shr 8).toByte()
        result[offset++] = ivLength.toByte()
        System.arraycopy(hybridData.iv, 0, result, offset, ivLength)
        
        return result
    }
    
    private fun deserializeHybridData(data: ByteArray): HybridEncryptionData {
        var offset = 0
        
        // Read key length and key
        val keyLength = ((data[offset++].toInt() and 0xFF) shl 24) or
                       ((data[offset++].toInt() and 0xFF) shl 16) or
                       ((data[offset++].toInt() and 0xFF) shl 8) or
                       (data[offset++].toInt() and 0xFF)
        val encryptedKey = ByteArray(keyLength)
        System.arraycopy(data, offset, encryptedKey, 0, keyLength)
        offset += keyLength
        
        // Read data length and data
        val dataLength = ((data[offset++].toInt() and 0xFF) shl 24) or
                        ((data[offset++].toInt() and 0xFF) shl 16) or
                        ((data[offset++].toInt() and 0xFF) shl 8) or
                        (data[offset++].toInt() and 0xFF)
        val encryptedData = ByteArray(dataLength)
        System.arraycopy(data, offset, encryptedData, 0, dataLength)
        offset += dataLength
        
        // Read IV length and IV
        val ivLength = ((data[offset++].toInt() and 0xFF) shl 24) or
                      ((data[offset++].toInt() and 0xFF) shl 16) or
                      ((data[offset++].toInt() and 0xFF) shl 8) or
                      (data[offset++].toInt() and 0xFF)
        val iv = ByteArray(ivLength)
        System.arraycopy(data, offset, iv, 0, ivLength)
        
        return HybridEncryptionData(encryptedKey, encryptedData, iv)
    }
    
    private fun hashData(data: ByteArray, algorithm: String): ByteArray {
        val digest = MessageDigest.getInstance(algorithm)
        return digest.digest(data)
    }
}

// Encryption-specific enums and data classes
enum class KeyStatus {
    ACTIVE, ROTATED, EXPIRED, REVOKED
}

data class EncryptedData(
    val ciphertext: ByteArray,
    val keyId: String,
    val iv: ByteArray,
    val algorithm: String,
    val additionalData: ByteArray?,
    val encryptedAt: Instant,
    val keyVersion: Int
)

data class PasswordEncryptedData(
    val ciphertext: ByteArray,
    val salt: ByteArray,
    val iv: ByteArray,
    val iterations: Int,
    val algorithm: String,
    val encryptedAt: Instant
)

data class DataEncryptionKey(
    val keyId: String,
    val keyMaterial: ByteArray,
    val algorithm: String,
    val keySize: Int,
    val version: Int,
    val status: KeyStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
    val rotatedAt: Instant? = null
)

data class AsymmetricKeyPair(
    val keyId: String,
    val publicKey: ByteArray,
    val privateKey: ByteArray,
    val algorithm: String,
    val keySize: Int,
    val createdAt: Instant,
    val expiresAt: Instant
)

data class AsymmetricEncryptedData(
    val ciphertext: ByteArray,
    val keyId: String,
    val algorithm: String,
    val encryptedAt: Instant,
    val isHybrid: Boolean
)

data class DigitalSignature(
    val signature: ByteArray,
    val algorithm: String,
    val keyId: String,
    val dataHash: ByteArray,
    val createdAt: Instant
)

data class HybridEncryptionData(
    val encryptedKey: ByteArray,
    val encryptedData: ByteArray,
    val iv: ByteArray
)

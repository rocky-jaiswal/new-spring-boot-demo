package dev.rockyj.example.demo.services

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import dev.rockyj.example.demo.configuration.Secrets
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo
import org.springframework.stereotype.Service
import java.io.InputStream
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * JWT Service for signing and verifying tokens using RSA keys with passphrase protection
 * Keys are static and loaded from files
 */
@Service
class JWTService(val secrets: Secrets) {
    companion object {
        init {
            // Add BouncyCastle provider for PEM parsing
            Security.addProvider(BouncyCastleProvider())
        }
    }

    val privateKeyResource =
        Thread.currentThread().contextClassLoader.getResourceAsStream(secrets.privateKeyPath)
            ?: throw IllegalArgumentException("File not found!")

    val pubKeyResource =
        Thread.currentThread().contextClassLoader.getResourceAsStream(secrets.publicKeyPath)
            ?: throw IllegalArgumentException("File not found!")

    // Load keys from files
    val privateKey = loadPrivateKey(privateKeyResource, secrets.privateKeyPassword)
    val publicKey = loadPublicKey(pubKeyResource)

    // Initialize signer and verifier
    private val signer: RSASSASigner = RSASSASigner(privateKey)
    private val verifier: RSASSAVerifier = RSASSAVerifier(publicKey)

    // @Throws(Exception::class)
    fun signJWT(
        subject: String?,
        audience: String?,
        expirationMinutes: Long,
        customClaims: MutableMap<String?, Any?>?,
    ): String {
        val now = Instant.now()
        val expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES)

        // Create JWT claims
        val claimsBuilder =
            JWTClaimsSet
                .Builder()
                .subject(subject)
                .issuer("dev.rockyj")
                .audience(audience)
                .expirationTime(Date.from(expiration))
                .notBeforeTime(Date.from(now))
                .issueTime(Date.from(now))
                .jwtID(UUID.randomUUID().toString())

        // Add custom claims
        customClaims?.forEach(claimsBuilder::claim)
        val claimsSet: JWTClaimsSet? = claimsBuilder.build()

        // Create JWT header
        val header = JWSHeader(JWSAlgorithm.RS512)

        // Create and sign JWT
        val signedJWT = SignedJWT(header, claimsSet)
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }

    // @Throws(Exception::class)
    fun verifyJWT(token: String?): JWTClaimsSet {
        val signedJWT: SignedJWT = SignedJWT.parse(token)

        // Verify signature
        if (!signedJWT.verify(verifier)) {
            throw SecurityException("Invalid JWT signature")
        }

        val claimsSet: JWTClaimsSet = signedJWT.jwtClaimsSet

        // Verify expiration
        val expirationTime: Date? = claimsSet.expirationTime
        if (expirationTime != null && expirationTime.before(Date())) {
            throw SecurityException("JWT has expired")
        }

        // Verify not before
        val notBefore: Date? = claimsSet.notBeforeTime
        if (notBefore != null && notBefore.after(Date())) {
            throw SecurityException("JWT not yet valid")
        }

        return claimsSet
    }

    // @Throws(Exception::class)
    private fun loadPrivateKey(
        fileStream: InputStream,
        passphrase: String,
    ): RSAPrivateKey {
        fileStream.reader().use { fileReader ->
            PEMParser(fileReader).use { pemParser ->
                val pemObject = pemParser.readObject() as PKCS8EncryptedPrivateKeyInfo

                val decryptorProvider =
                    JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .setProvider("BC")
                        .build(passphrase.toCharArray())

                val privateKeyInfo = pemObject.decryptPrivateKeyInfo(decryptorProvider)
                val converter = JcaPEMKeyConverter().setProvider("BC")
                val privateKey = converter.getPrivateKey(privateKeyInfo)

                if (privateKey !is RSAPrivateKey) {
                    throw IllegalArgumentException("Key is not an RSA private key")
                }

                return privateKey
            }
        }
    }

    // @Throws(Exception::class)
    private fun loadPublicKey(fileStream: InputStream): RSAPublicKey {
        fileStream.reader().use { fileReader ->
            PEMParser(fileReader).use { pemParser ->
                val pemObject: Any? = pemParser.readObject()
                val converter: JcaPEMKeyConverter = JcaPEMKeyConverter().setProvider("BC")

                if (pemObject is SubjectPublicKeyInfo) {
                    return converter.getPublicKey(pemObject) as RSAPublicKey
                } else {
                    throw IllegalArgumentException("Unsupported PEM object type for public key")
                }
            }
        }
    }
}

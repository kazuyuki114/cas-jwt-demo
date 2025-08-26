package org.example.casjwtdemo.service;

import org.apache.commons.codec.binary.Base64;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.AesKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.signing.key}")
    private String signingKey;

    @Value("${jwt.encryption.key}")
    private String encryptionKey;

    public Map<String, Object> decodeJwt(String secureJwt) {
        try {
            // Step 1: Verify JWS signature using the signing key
            var key = new AesKey(signingKey.getBytes(StandardCharsets.UTF_8));

            var jws = new JsonWebSignature();
            jws.setCompactSerialization(secureJwt);
            jws.setKey(key);

            if (!jws.verifySignature()) {
                throw new Exception("JWT verification failed");
            }

            // Step 2: Decode the JWS payload (which contains the JWE)
            var decodedBytes = Base64.decodeBase64(jws.getEncodedPayload().getBytes(StandardCharsets.UTF_8));
            var decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            // Step 3: Decrypt the JWE using the encryption key
            var jwe = new JsonWebEncryption();
            var jsonWebKey = JsonWebKey.Factory
                .newJwk("{\n" +
                       "\"kty\":\"oct\",\n" +
                       "\"k\":\"" + encryptionKey + "\"\n" +
                       "}");

            jwe.setCompactSerialization(decodedPayload);
            jwe.setKey(new AesKey(jsonWebKey.getKey().getEncoded()));

            String plaintextPayload = jwe.getPlaintextString();

            // Step 4: Parse and return the decrypted payload information
            Map<String, Object> result = new HashMap<>();
            result.put("header", extractHeader(secureJwt));
            result.put("payload", plaintextPayload);
            result.put("rawToken", secureJwt);
            result.put("decryptedPayload", plaintextPayload);

            return result;

        } catch (Exception e) {
            // Fallback: try basic Base64 decoding if JWE decryption fails
            return fallbackDecode(secureJwt, e);
        }
    }

    private String extractHeader(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length > 0) {
                byte[] headerBytes = Base64.decodeBase64(parts[0]);
                return new String(headerBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // Ignore and return raw header
        }
        return "Unable to decode header";
    }

    private Map<String, Object> fallbackDecode(String token, Exception originalException) {
        try {
            // Basic JWT structure parsing as fallback
            String[] chunks = token.split("\\.");

            if (chunks.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }

            // Decode the payload (second part) using Base64
            java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);

            Map<String, Object> result = new HashMap<>();
            result.put("header", new String(decoder.decode(chunks[0]), StandardCharsets.UTF_8));
            result.put("payload", payload);
            result.put("rawToken", token);
            result.put("error", "JWE decryption failed, showing basic decode: " + originalException.getMessage());

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT token with both JWE and fallback methods", e);
        }
    }
}

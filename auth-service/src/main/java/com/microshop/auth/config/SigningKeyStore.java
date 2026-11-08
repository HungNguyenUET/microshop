package com.microshop.auth.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Lay khoa RSA tu bang signing_key; lan dau chay thi sinh khoa moi va luu lai.
 * Neu sinh khoa ngau nhien moi lan khoi dong (nhu nhieu vi du tren mang) thi restart auth-service
 * se lam MOI token dang luu hanh bi tu choi o tat ca service.
 */
@Component
public class SigningKeyStore {

    private static final Logger log = LoggerFactory.getLogger(SigningKeyStore.class);

    private final JdbcTemplate jdbc;

    public SigningKeyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RSAKey loadOrCreate() {
        return jdbc.query("SELECT kid, public_key, private_key FROM signing_key ORDER BY created_at DESC LIMIT 1",
                        (rs, i) -> toRsaKey(rs.getString(1), rs.getString(2), rs.getString(3)))
                .stream().findFirst()
                .orElseGet(this::createAndStore);
    }

    private RSAKey createAndStore() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            String kid = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO signing_key (kid, public_key, private_key, created_at) VALUES (?, ?, ?, ?)",
                    kid,
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                    Timestamp.from(Instant.now()));
            log.info("Generated new RSA signing key kid={}", kid);
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey(pair.getPrivate()).keyID(kid).build();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate RSA key", e);
        }
    }

    private static RSAKey toRsaKey(String kid, String publicKey, String privateKey) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            var pub = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            var priv = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
            return new RSAKey.Builder(pub).privateKey(priv).keyID(kid).build();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load RSA key " + kid, e);
        }
    }
}

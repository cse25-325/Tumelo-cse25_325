package bank.util;

import bank.model.BankException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted PBKDF2 password hashing (data-protection requirement). */
public final class PasswordHasher {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_BITS = 256;

    private PasswordHasher() {
    }

    public static String newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String saltBase64) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(saltBase64),
                    ITERATIONS, KEY_BITS);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new BankException("Could not secure the password.", e);
        }
    }

    public static boolean verify(String password, String saltBase64, String expectedHash) {
        byte[] a = hash(password, saltBase64).getBytes();
        byte[] b = expectedHash.getBytes();
        return MessageDigest.isEqual(a, b); // constant-time compare
    }
}

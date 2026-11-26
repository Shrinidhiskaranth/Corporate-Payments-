package com.nmamit.payments.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time passwords for confirming a payment.
 * - 6 random digits, valid for 5 minutes, at most 3 wrong attempts.
 * - Only a SHA-256 hash of the code is kept, in memory, keyed by request id.
 * - A code works only for the user it was issued to.
 */
@Service
public class OtpService {

    static final Duration VALIDITY = Duration.ofMinutes(5);
    static final int MAX_ATTEMPTS = 3;

    private final SecureRandom random = new SecureRandom();
    private final Map<Long, OtpEntry> entries = new ConcurrentHashMap<>();

    /** Creates a new code for this request and user, replacing any earlier one. Returns the plain code. */
    public String generate(Long requestId, Long userId) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        entries.put(requestId, new OtpEntry(hash(code), userId, Instant.now().plus(VALIDITY)));
        return code;
    }

    public boolean hasActiveCode(Long requestId, Long userId) {
        OtpEntry e = entries.get(requestId);
        return e != null && e.userId.equals(userId) && Instant.now().isBefore(e.expiresAt);
    }

    /** Checks the code; throws WorkflowException with a clear reason if it is not accepted. */
    public void verify(Long requestId, Long userId, String code) {
        OtpEntry e = entries.get(requestId);
        if (e == null || !e.userId.equals(userId)) {
            throw new WorkflowException("No OTP has been sent for this payment. Click 'Send OTP' first.");
        }
        if (Instant.now().isAfter(e.expiresAt)) {
            entries.remove(requestId);
            throw new WorkflowException("The OTP has expired. Please request a new one.");
        }
        if (code == null || !code.trim().matches("\\d{6}")) {
            throw new WorkflowException("Enter the 6-digit OTP");
        }
        if (!MessageDigest.isEqual(e.hash.getBytes(StandardCharsets.UTF_8),
                                   hash(code.trim()).getBytes(StandardCharsets.UTF_8))) {
            e.attempts++;
            if (e.attempts >= MAX_ATTEMPTS) {
                entries.remove(requestId);
                throw new WorkflowException("Wrong OTP entered " + MAX_ATTEMPTS
                        + " times. Please request a new one.");
            }
            throw new WorkflowException("Wrong OTP. " + (MAX_ATTEMPTS - e.attempts) + " attempt(s) left.");
        }
        entries.remove(requestId); // a code can be used only once
    }

    /** Shows only the last 4 digits, e.g. ******0002. */
    public static String mask(String mobile) {
        if (mobile == null || mobile.length() <= 4) {
            return "****";
        }
        return "*".repeat(mobile.length() - 4) + mobile.substring(mobile.length() - 4);
    }

    private static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class OtpEntry {
        final String hash;
        final Long userId;
        final Instant expiresAt;
        int attempts;

        OtpEntry(String hash, Long userId, Instant expiresAt) {
            this.hash = hash;
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
}

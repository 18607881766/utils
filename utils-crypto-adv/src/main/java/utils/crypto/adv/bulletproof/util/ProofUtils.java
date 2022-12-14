package utils.crypto.adv.bulletproof.util;/*
 * Decompiled with CFR 0_110.
 */

import org.bouncycastle.jcajce.provider.digest.Keccak;
import utils.crypto.adv.bulletproof.algebra.GroupElement;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

public class ProofUtils {
    private static final ThreadLocal<Keccak.Digest256> KECCACK;
    private static Random RNG;


    private static BigInteger toInt(byte[] hash, BigInteger q) {
        BigInteger bigIntHash = new BigInteger(hash);
        if (bigIntHash.signum() < 0) {
            return bigIntHash.add(BigInteger.ONE.shiftLeft(256)).mod(q);
        }
        return bigIntHash.mod(q);
    }

    public static <T extends GroupElement<T>> BigInteger computeChallenge(BigInteger q, T... points) {

        MessageDigest sha = KECCACK.get();
        for (T point : points) {
            sha.update(point.canonicalRepresentation());
        }
        byte[] hash = sha.digest();
        return toInt(hash, q);
    }

    public static <T extends GroupElement<T>> BigInteger computeChallenge(BigInteger q, BigInteger[] points) {

        MessageDigest sha = KECCACK.get();
        for (BigInteger point : points) {
            hashUInt(sha, point);
        }
        byte[] hash = sha.digest();
        return toInt(hash, q);
    }

    public static <T extends GroupElement<T>> BigInteger computeChallenge(BigInteger q, Iterable<T> points) {
        MessageDigest sha = KECCACK.get();
        for (T point : points) {
            sha.update(point.canonicalRepresentation());
        }
        byte[] hash = sha.digest();
        return toInt(hash, q);
    }


    public static <T extends GroupElement<T>> BigInteger computeChallenge(BigInteger q, BigInteger salt, Iterable<T> points) {
        MessageDigest sha = KECCACK.get();
        hashUInt(sha, salt);
        for (T point : points) {
            sha.update(point.canonicalRepresentation());
        }
        byte[] hash = sha.digest();
        return toInt(hash, q);
    }

    public static <T extends GroupElement<T>> BigInteger computeChallenge(BigInteger q, BigInteger[] ints, T... points) {
        MessageDigest sha = KECCACK.get();
        for (BigInteger integer : ints) {
            hashUInt(sha, integer);

        }
        for (T point : points) {
            sha.update(point.canonicalRepresentation());
        }
        byte[] hash = sha.digest();
        return toInt(hash, q);

    }

    public static <T extends GroupElement<T>> BigInteger computeChallenge(BigInteger q, BigInteger salt, T... points) {
        MessageDigest sha = KECCACK.get();
        hashUInt(sha, salt);

        for (T point : points) {
            sha.update(point.canonicalRepresentation());
        }
        byte[] hash = sha.digest();
        return toInt(hash, q);
    }

    public static BigInteger challengeFromints(BigInteger q, BigInteger... ints) {
        MessageDigest sha = KECCACK.get();
        for (BigInteger integer : ints) {
            hashUInt(sha, integer);
        }
        return toInt(sha.digest(), q);
    }


    public static BigInteger hash(String string) {
        KECCACK.get().update(string.getBytes());
        return new BigInteger(KECCACK.get().digest());
    }

    public static BigInteger paddedHash(String part1, int i) {
        KECCACK.get().update(part1.getBytes());
        KECCACK.get().update(ByteBuffer.allocate(32).putInt(28, i));
        BigInteger hashResult = new BigInteger(KECCACK.get().digest());
        if (hashResult.signum() < 0) {
            return BigInteger.valueOf(1).shiftLeft(256).add(hashResult);
        } else {
            return hashResult;
        }
    }


    public static BigInteger hash(String id, BigInteger salt) {
        KECCACK.get().update(id.getBytes());
        KECCACK.get().update(salt.toByteArray());
        return new BigInteger(KECCACK.get().digest());
    }

    public static BigInteger randomNumber(int bits) {
        return new BigInteger(bits, RNG);
    }

    public static BigInteger randomNumber() {
        return ProofUtils.randomNumber(256);
    }

    /**
     * This methods allows to set a RNG. The RNG
     *
     * @param rng
     */
    public static void setRNG(Random rng) {
        if (!SecureRandom.class.isAssignableFrom(rng.getClass())) {
        }
        RNG = rng;

    }

    private static void hashUInt(MessageDigest messageDigest, BigInteger integer) {
        byte[] intArr = integer.toByteArray();
        if (intArr.length >= 32) {
            messageDigest.update(intArr, intArr.length - 32, 32);
        } else {
            byte[] shaArr = new byte[32];
            System.arraycopy(intArr, 0, shaArr, 32 - intArr.length, intArr.length);
            messageDigest.update(shaArr);

        }

    }

    static {
        RNG = new SecureRandom(new byte[]{1, 2});
        KECCACK = ThreadLocal.withInitial(Keccak.Digest256::new);
    }
}


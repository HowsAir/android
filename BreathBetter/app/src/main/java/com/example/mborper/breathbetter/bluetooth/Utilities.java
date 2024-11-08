package com.example.mborper.breathbetter.bluetooth;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * The Utilities class provides a set of utility functions to handle string and byte conversions,
 * including operations on UUIDs, byte arrays, and hexadecimal representations.
 *
 * @author Jordi Bataller i Mascarell
 */
public class Utilities {

    /**
     * Converts a byte array to a string.
     *
     *      [bytes] ---> bytesToString() ---> Texto
     *
     * @param bytes The byte array to convert.
     * @return A string representation of the byte array.
     */
    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * Converts two long values into a byte array.
     *
     *      Natural:masSignificativos   ---> dosLongToBytes() ---> [byte]
     *      Natural:menosSignificativos
     *
     * @param masSignificativos The most significant bits.
     * @param menosSignificativos The least significant bits.
     * @return A byte array representing the two long values.
     */
    public static byte[] dosLongToBytes(long masSignificativos, long menosSignificativos) {
        ByteBuffer buffer = ByteBuffer.allocate(2 * Long.BYTES);
        buffer.putLong(masSignificativos);
        buffer.putLong(menosSignificativos);
        return buffer.array();
    }

    /**
     * Converts a byte array to an integer.
     *
     *      [bytes] ---> bytesToInt() ---> Natural
     *
     * @param bytes The byte array to convert.
     * @return The integer representation of the byte array.
     */
    public static int bytesToInt(byte[] bytes) {
        return new BigInteger(bytes).intValue();
    }

    /**
     * Converts a byte array to a long value.
     *
     *      [bytes] ---> bytesToLong() ---> Natural
     *
     * @param bytes The byte array to convert.
     * @return The long representation of the byte array.
     */
    public static long bytesToLong(byte[] bytes) {
        return new BigInteger(bytes).longValue();
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     *      [bytes] ---> bytesToHexString() ---> Texto hexadecimal
     *
     * @param bytes The byte array to convert.
     * @return The hexadecimal string representation of the byte array.
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
            sb.append(':');
        }
        return sb.toString();
    }

    /**
     * Converts a byte array to an integer using safe checks.
     *
     *      [bytes] ---> bytesToIntOK() ---> Natural
     *
     * @param bytes The byte array to convert.
     * @return The integer representation of the byte array.
     * @throws Error if the byte array is longer than 4 bytes.
     */
    public static int bytesToIntOK(byte[] bytes) {
        if (bytes == null) {
            return 0;
        }

        if (bytes.length > 4) {
            throw new Error("Too many bytes to convert to int");
        }

        int res = 0;
        for (byte b : bytes) {
            res = (res << 8) + (b & 0xFF); // Accumulates bytes
        }

        if ((bytes[0] & 0x8) != 0) {
            res = -(~(byte) res) - 1; // Two's complement for negative numbers
        }

        return res;
    }

    /**
     * Converts a string to a byte array.
     *
     *      Texto ---> stringToBytes() ---> [byte]
     *
     * @param texto The string to convert.
     * @return A byte array representing the input string.
     */
    public static byte[] stringToBytes(String texto) {
        return texto.getBytes();
    }

    /**
     * Converts a 16-character string into a UUID.
     *
     *      Texto:uuid ---> stringToUUID ---> [Caracteres]:UUID
     *
     * @param uuid The string to convert to a UUID.
     * @return A UUID generated from the input string.
     * @throws Error if the string is not 16 characters long.
     */
    public static UUID stringToUUID(String uuid) {
        if (uuid.length() != 16) {
            throw new Error("stringToUUID: string must have 16 characters");
        }

        byte[] comoBytes = uuid.getBytes();
        String masSignificativo = uuid.substring(0, 8);
        String menosSignificativo = uuid.substring(8, 16);

        return new UUID(bytesToLong(masSignificativo.getBytes()), bytesToLong(menosSignificativo.getBytes()));
    }

    /**
     * Converts a UUID to a string.
     *
     *      [Caracteres]:UUID ---> uuidToString() ---> Texto
     *
     * @param uuid The UUID to convert.
     * @return A string representation of the UUID.
     */
    public static String uuidToString(UUID uuid) {
        return bytesToString(dosLongToBytes(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
    }

    /**
     * Converts a UUID to a hexadecimal string.
     *
     *      [Caracteres]:UUID ---> uuidToHexString() ---> Texto hexadecimal
     *
     * @param uuid The UUID to convert.
     * @return A hexadecimal string representation of the UUID.
     */
    public static String uuidToHexString(UUID uuid) {
        return bytesToHexString(dosLongToBytes(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
    }
}
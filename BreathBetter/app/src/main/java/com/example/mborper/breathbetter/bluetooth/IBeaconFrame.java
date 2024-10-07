package com.example.mborper.breathbetter.bluetooth;

import java.util.Arrays;

// -----------------------------------------------------------------------------------
// @author: Jordi Bataller i Mascarell
// -----------------------------------------------------------------------------------
public class IBeaconFrame {
    private byte[] prefix = null; // 9 bytes
    private byte[] uuid = null; // 16 bytes
    private byte[] major = null; // 2 bytes
    private byte[] minor = null; // 2 bytes
    private byte txPower = 0; // 1 byte

    private byte[] bytes;

    private byte[] advFlags = null; // 3 bytes
    private byte[] advHeader = null; // 2 bytes
    private byte[] companyID = new byte[2]; // 2 bytes
    private byte iBeaconType = 0; // 1 byte
    private byte iBeaconLength = 0; // 1 byte

    // -------------------------------------------------------------------------------
    /**
     * Returns the prefix of the iBeacon.
     *
     * @return The prefix as a byte array.
     */
    public byte[] getPrefix() {
        return prefix;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the UUID of the iBeacon.
     *
     * @return The UUID as a byte array.
     */
    public byte[] getUUID() {
        return uuid;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the major value of the iBeacon.
     *
     * @return The major as a byte array.
     */
    public byte[] getMajor() {
        return major;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the minor value of the iBeacon.
     *
     * @return The minor as a byte array.
     */
    public byte[] getMinor() {
        return minor;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the transmission power of the iBeacon.
     *
     * @return The transmission power as a byte.
     */
    public byte getTxPower() {
        return txPower;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the raw bytes of the iBeacon.
     *
     * @return The bytes as a byte array.
     */
    public byte[] getBytes() {
        return bytes;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the advertising flags of the iBeacon.
     *
     * @return The advertising flags as a byte array.
     */
    public byte[] getAdvFlags() {
        return advFlags;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the advertising header of the iBeacon.
     *
     * @return The advertising header as a byte array.
     */
    public byte[] getAdvHeader() {
        return advHeader;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the company ID of the iBeacon.
     *
     * @return The company ID as a byte array.
     */
    public byte[] getCompanyID() {
        return companyID;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the iBeacon type.
     *
     * @return The iBeacon type as a byte.
     */
    public byte getIBeaconType() {
        return iBeaconType;
    }

    // -------------------------------------------------------------------------------
    /**
     * Returns the length of the iBeacon.
     *
     * @return The length of the iBeacon as a byte.
     */
    public byte getIBeaconLength() {
        return iBeaconLength;
    }

    // -------------------------------------------------------------------------------
    /**
     * Constructs a new IBeaconFrame instance from the provided bytes.
     *
     * @param bytes The raw bytes containing the iBeacon information.
     */
    public IBeaconFrame(byte[] bytes) {
        this.bytes = bytes;

        prefix = Arrays.copyOfRange(bytes, 0, 9); // 9 bytes
        uuid = Arrays.copyOfRange(bytes, 9, 25); // 16 bytes
        major = Arrays.copyOfRange(bytes, 25, 27); // 2 bytes
        minor = Arrays.copyOfRange(bytes, 27, 29); // 2 bytes
        txPower = bytes[29]; // 1 byte

        advFlags = Arrays.copyOfRange(prefix, 0, 3); // 3 bytes
        advHeader = Arrays.copyOfRange(prefix, 3, 5); // 2 bytes
        companyID = Arrays.copyOfRange(prefix, 5, 7); // 2 bytes
        iBeaconType = prefix[7]; // 1 byte
        iBeaconLength = prefix[8]; // 1 byte
    } // ()
} // class

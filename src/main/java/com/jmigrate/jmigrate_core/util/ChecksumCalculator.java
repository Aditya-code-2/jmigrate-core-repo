package com.jmigrate.jmigrate_core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumCalculator {

    public static String calculateSHA256(File file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not supported on this JVM", e);
        }

        try (FileInputStream fis = new FileInputStream(file);
             DigestInputStream dis = new DigestInputStream(fis, digest)) {

            byte[] buffer = new byte[1024];
            while (dis.read(buffer) != -1) {
                
            }
        }

        StringBuilder hexString = new StringBuilder();
        for (byte b : digest.digest()) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}

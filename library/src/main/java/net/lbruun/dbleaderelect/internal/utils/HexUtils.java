/*
 * Copyright 2021 lbruun.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.dbleaderelect.internal.utils;

import java.util.Objects;

/**
 * Utilities for converting byte(s) into hexadecimal character/string
 * representation. Methods are optimized for speed.
 * 
 * <p>
 * Can be replaced by {@code HexFormat} introduced in JDK 17.
 */
public class HexUtils {

    private static final char[] HEX_CHARS_LOWER = 
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final char[] HEX_CHARS_UPPER = 
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    
    
    
    /**
     * Case used for hex string.
     * 
     * <p>
     * Example:
     * <ul>
     *   <li>Upper case: {@code 3E}</li>
     *   <li>Lower case: {@code 3e}</li>
     * </ul>
     */
    public enum Case {
        LOWER,
        UPPER
    }

    private HexUtils() {
    }
    
    /**
     * Converts a byte into hexadecimal representation and put the result into a
     * target character array at a specified location. Exactly two chars
     * will be written to the target array.
     *
     * @param b input to convert to hex
     * @param target the char array where the result will be put
     * @param targetStartPos where to put the hex chars in {@code target}
     *          in the target array.
     * @param caseType if the hexadecimal representation should be in upper or lower case
     * @return the next position in the target array (after writing to it)
     */
    private static int putHexIntoCharArray(byte b, char[] target, int targetStartPos, Case caseType) {
        int octet = b & 0xFF;
        char[] hexArray = (caseType == Case.UPPER) ? HEX_CHARS_UPPER : HEX_CHARS_LOWER;
        target[targetStartPos] = hexArray[octet >>> 4];
        target[targetStartPos + 1] = hexArray[octet & 0x0F];
        return targetStartPos + 2;
    }

    /**
     * Converts a single byte to its hexadecimal representation.
     * 
     * @param b input
     * @param caseType if the hexadecimal representation should be in upper or lower case
     * @return char array of length 2
     */
    public static char[] byteToHex(byte b, Case caseType) {
        char[] hexChars = new char[2];
        putHexIntoCharArray(b, hexChars, 0, caseType);
        return hexChars;
    }

    /**
     * Converts a byte array to its hexadecimal representation.
     * 
     * @param bytes input
     * @param caseType if the hexadecimal representation should be in upper or lower case
     * @param delim optional delimiter between hex values, may be null
     * @return hexadecimal representation
     */
    public static char[] bytesToHex(byte[] bytes, Case caseType, String delim) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length == 0) {
            return new char[]{};
        }
        Objects.requireNonNull(caseType, "caseType cannot be null");
        
        int delimLength = (delim != null) ? delim.length() : 0;
        int noOfDelims = bytes.length - 1;
        char[] hexChars = new char[(bytes.length * 2) + (noOfDelims * delimLength)];

        int arrPos = 0;
        for (int i = 0; i < bytes.length; i++) {
            arrPos = putHexIntoCharArray(bytes[i], hexChars, arrPos, caseType);
            if (i < (bytes.length - 1)) { // as long as not at the end
                if (delim != null && (!delim.isEmpty())) {
                    for (int j = 0; j < delim.length(); j++) {
                        hexChars[arrPos + j] = delim.charAt(j);
                    }
                    arrPos = arrPos + delim.length();
                }
            }
        }
        return hexChars;
    }
    
    /**
     * Converts a byte array to its hexadecimal representation.
     * 
     * @param bytes input
     * @param caseType if the hexadecimal representation should be in upper or lower case
     * @param delim optional delimiter between hex values, may be null
     * @return hexadecimal representation
     */
    public static String bytesToHexStr(byte[] bytes, Case caseType, String delim) {
        char[] chars = bytesToHex(bytes, caseType, delim);
        if (chars == null) {
            return null;
        }
        return new String(chars);
    }
}

/*
 * Copyright 2021 lbruun.
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
package net.lbruun.dbleaderelect.utils;

import net.lbruun.dbleaderelect.internal.utils.HexUtils;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HexUtilsTest {
    
    @Test
    public void testByteToHex() {
        char[] expResult = new char[]{'F', 'F'};
        char[] result = HexUtils.byteToHex((byte) 0xff, HexUtils.Case.UPPER);
        assertArrayEquals(expResult, result);

        expResult = new char[]{'0', '0'};
        result = HexUtils.byteToHex((byte) 0x00, HexUtils.Case.UPPER);
        assertArrayEquals(expResult, result);

        expResult = new char[]{'4', 'A'};
        result = HexUtils.byteToHex((byte) 0x4a, HexUtils.Case.UPPER);
        assertArrayEquals(expResult, result);
        
        expResult = new char[]{'4', 'a'};
        result = HexUtils.byteToHex((byte) 0x4a, HexUtils.Case.LOWER);
        assertArrayEquals(expResult, result);
    }

    @Test
    public void testBytesToHex() {
        System.out.println("bytesToHex");

        byte[] bytes = "Lorum Ipsum".getBytes(StandardCharsets.US_ASCII);
        char[] expectedResult = "4c6f72756d20497073756d".toCharArray();
        char[] result = HexUtils.bytesToHex(bytes, HexUtils.Case.LOWER, null);
        assertArrayEquals(expectedResult, result);
        
        expectedResult = "4C6F72756D20497073756D".toCharArray();
        result = HexUtils.bytesToHex(bytes, HexUtils.Case.UPPER, null);
        assertArrayEquals(expectedResult, result);

        expectedResult = "4C:6F:72:75:6D".toCharArray();
        result = HexUtils.bytesToHex("Lorum".getBytes(StandardCharsets.US_ASCII), HexUtils.Case.UPPER, ":");
        assertArrayEquals(expectedResult, result);

        expectedResult = "4C---6F---72---75---6D".toCharArray();
        result = HexUtils.bytesToHex("Lorum".getBytes(StandardCharsets.US_ASCII), HexUtils.Case.UPPER, "---");
        assertArrayEquals(expectedResult, result);

        expectedResult = "4C6F72756D".toCharArray();
        result = HexUtils.bytesToHex("Lorum".getBytes(StandardCharsets.US_ASCII), HexUtils.Case.UPPER, "");
        assertArrayEquals(expectedResult, result);
        
        expectedResult = "4C6F72756D".toCharArray();
        result = HexUtils.bytesToHex("Lorum".getBytes(StandardCharsets.US_ASCII), HexUtils.Case.UPPER, null);
        assertArrayEquals(expectedResult, result);
        
        expectedResult = "00".toCharArray();
        result = HexUtils.bytesToHex(new byte[]{0}, HexUtils.Case.UPPER, "---");
        assertArrayEquals(expectedResult, result);

        expectedResult = "0A:23:25:7E".toCharArray();
        result = HexUtils.bytesToHex("\n#%~".getBytes(StandardCharsets.US_ASCII), HexUtils.Case.UPPER, ":");
        assertArrayEquals(expectedResult, result);
    }

    @Test
    public void testBytesToHexStr() {
        String expectedResult = "4368696361676f";
        String hexStr = HexUtils.bytesToHexStr("Chicago".getBytes(StandardCharsets.US_ASCII), HexUtils.Case.LOWER, "");
        assertEquals(expectedResult, hexStr);

        // Input is empty --> result is empty
        expectedResult = "";
        hexStr = HexUtils.bytesToHexStr(new byte[]{}, HexUtils.Case.LOWER, ":");
        assertEquals(expectedResult, hexStr);

        // Input is null --> result is null
        hexStr = HexUtils.bytesToHexStr(null, HexUtils.Case.LOWER, ":");
        assertNull(hexStr);
    }
    
}

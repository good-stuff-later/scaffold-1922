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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class NodeIdUtilsTest {

    @Test
    public void testGetPidAndHostname() {
        String processIdAndHostname = NodeIdUtils.getPidAndHostname();
        assertTrue(processIdAndHostname.contains("@"));
    }

    @Test
    public void testGetComputerName() {
        String computerName = NodeIdUtils.getComputerName();
        assertNotNull(computerName);
    }
    
    @Test
    public void testGetPid() {
        long pid = NodeIdUtils.getPid();
        assertTrue(pid >= 0);  // All know OS'es use positive integers for process id
    }


    @Test
    public void testGetPidAndComputerName() {
        String pidAndComputerName = NodeIdUtils.getPidAndComputerName();
        assertTrue(pidAndComputerName.contains("@"));
    }
    
    @Test
    public void testGetMACAddress() {
        String macAddress = NodeIdUtils.getMACAddress(null);
        
        // MAC address is always 6 bytes which means the resulting
        // hex string including delimiters will be 6*2+5 characters long.
        assertTrue(macAddress.length() == (6 * 2) + 5);
    }
}

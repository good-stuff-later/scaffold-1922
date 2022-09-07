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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.lbruun.dbleaderelect.internal.utils.HexUtils;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Methods for building a node id.
 * 
 * <p>
 * A node id can be used in leader election as a unique identification of
 * of a leader candidate. 
 */
public class NodeIdUtils {

    private static volatile String PID_AND_HOSTNAME_CACHED = null;
    private NodeIdUtils() {
    }
    
    
    
    /**
     * Gets the name of the current host.
     * 
     * <p>
     * On Windows hosts it is the value of the {@code %COMPUTERNAME%}
     * OS environment variable. On Unix-like hosts it is the value of the
     * {@code $HOSTNAME} OS environment variable.
     * 
     * <p>
     * Unlike {@link #getNetworkHostname()} this does not require a (potential)
     * call to DNS and the method will generally always give a result even if
     * the host is not networked. And it will never block on a network operation
     * as it performs none.
     *
     * 
     * @return computer name or {@code null}
     */
    public static String getComputerName() {
        String nodename;

        if (System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows")) {
            nodename = System.getenv("COMPUTERNAME");
            if (nodename != null) {
                return nodename;
            }
        } else {
            // If it is not Windows then it is most likely a Unix-like operating
            // system (e.g. Linux or Mac OS)

            // Many shells such as Bash or derivatives sets the 
            // HOSTNAME variable. Kubernetes also sets it (to the name of the
            // pod, not the name of the node)
            nodename = System.getenv("HOSTNAME");
            if (nodename != null) {
                return nodename;
            } else {
                // Get it by capturing output of 'hostname' command
                nodename = execOsCommand("hostname");
                if (nodename != null) {
                    return nodename;
                }
            }
        }
        return null;
    }

    
    /**
     * Gets the network hostname. This is the hostname as named in the 
     * network.
     * 
     * <p>
     * The method returns the value of {@link java.net.InetAddress#getLocalHost() 
     * InetAddress.getLocalHost()}.{@link java.net.InetAddress#getHostName() getHostName()}.
     * There are a number of caveats with this approach:
     * <ul>
     *   <li>If the host is not on a network the host will have no network name.</li>
     *   <li>If the host has multiple network interfaces the result of this 
     *       method is indeterminate.</li>
     *   <li>In some circumstances the method will require a call to DNS. 
     *       Such call may block for a period of time.</li>
     * </ul>
     * 
     * @see #getComputerName() 
     * @return host or {@code null} if the network hostname cannot be resolved.
     */
    public static String getNetworkHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return null;
        }
    }
    
    /**
     * Gets the MAC address corresponding to a particular local IP address. A
     * media access control address (MAC address) is a unique identifier
     * assigned to a network interface controller (NIC) for use as a network
     * address in communications within a network segment. 
     *
     * <p>
     * If {@code null} is supplied then the MAC address of the NIC which
     * has been assigned the IP equal to {@link InetAddress#getLocalHost()}
     * is returned.
     * 
     * <p>
     * {@code null} is returned if the MAC address cannot be determined.
     * 
     * <p>
     * The hex string returned in upper-case and hex values are separated
     * by the ':' character, for example ''.
     * 
     * @throws IllegalArgumentException if a non-null argument is supplied
     *         but the argument is not a local IP address.
     * @param ip local IP or null
     * @return MAC address in hex notation or {@code null} if the MAC address 
     *         cannot be determined.
     */
    public static String getMACAddress(InetAddress ip)  {
        
        // First attempt: Find the network interface corresponding
        // to the IP supplied or if not supplied: ourselves
        NetworkInterface foundNi = null;
        try {
            if (ip != null) {
                foundNi = NetworkInterface.getByInetAddress(ip);
                if (foundNi == null) {
                    throw new IllegalArgumentException(ip + " is not a local IP address");
                }
            } else {
                InetAddress localHost = InetAddress.getLocalHost();
                foundNi = NetworkInterface.getByInetAddress(localHost);
            }
        } catch (UnknownHostException | SocketException ex) {
        }
        
        // Second attempt: loop through all network interfaces on the host
        // and pick one of them. Note that enumerating network interfaces
        // can be a bit lenghty operation on some hosts, up to 1000-1500 ms
        // worst case.
        if (foundNi == null) {
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface ni = networkInterfaces.nextElement();
                    if (ni.isUp() && (!ni.isLoopback())) {
                        if (!ni.isVirtual()) {  // preference for non-virtual NIC
                            foundNi = ni;
                            break;
                        } else {
                            if (foundNi == null) { 
                                foundNi = ni;
                            }
                        }
                    }
                }
            } catch (SocketException ex) {
            }
        }

        // If we have located the network interface by now we can extract
        // its MAC Address.
        if (foundNi != null) {
            try {
                byte[] hardwareAddress = foundNi.getHardwareAddress();
                if (hardwareAddress != null) {
                    return HexUtils.bytesToHexStr(hardwareAddress, HexUtils.Case.UPPER, ":");
                }
            } catch (SocketException ex) {
            }
        }
        return null;
    }
    
    /**
     * Gets the MAC address of the NIC which has been assigned the IP equal to
     * {@link InetAddress#getLocalHost()}. This is equivalent to calling
     * {@link #getMACAddress(java.net.InetAddress)} with a {@code null} argument.
     * 
     * <p>
     * {@code null} is returned if the MAC address cannot be determined.
     * 
     * @see #getMACAddress(java.net.InetAddress) 
     * @return MAC address in hex notation or {@code null} if the MAC address 
     *         cannot be determined.
     */
    public static String getMACAddress()  {
        return getMACAddress(null);
    }
    
    /**
     * Gets a string on the form {@code pid@hostname} where {@code pid} is
     * the process id of the current process and {@code hostname} is the same
     * value as returned by {@link #getNetworkHostname()}.
     *
     * @return pid and hostname
     */
    public static String getPidAndHostname() {
        if (PID_AND_HOSTNAME_CACHED == null) {
            PID_AND_HOSTNAME_CACHED = ManagementFactory.getRuntimeMXBean().getName();
        }
        return PID_AND_HOSTNAME_CACHED;
    }
    
    /**
     * Gets a string on the form {@code pid@computername} where {@code pid} is
     * the process id of the current process and {@code hostname} is the same
     * value as returned by {@link #getComputerName()}.
     *
     * @return pid and computername
     */
    public static String getPidAndComputerName() {
        long pid = getPid();
        String computerName = getComputerName();
        if (computerName != null) {
            return Long.toString(pid) + "@" + computerName;
        }
        return getPidAndHostname();
    }
    
    /**
     * Gets a random UUID.
     * 
     * @see UUID#randomUUID() 
     * @return 
     */
    public String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    
    /**
     * Gets the native process ID of the current OS process. The native process
     * ID is an identification number that the operating system assigns to the
     * process.
     *
     * @return process id
     * @throws UnsupportedOperationException if the process id cannot be determined
     */
    public static long getPid() {
        // Need to support Java 8 so cannot use ProcessHandle API introduced 
        // in Java 9.
        String processIdAndHostname = getPidAndHostname();
        String[] split = processIdAndHostname.split("\\@");
        if (split.length > 1) {
            String pidStr = split[0];
            if (!pidStr.isEmpty()) {
                try {
                    return Integer.parseInt(pidStr);
                } catch (NumberFormatException ex) {
                }
            }
        }
        throw new UnsupportedOperationException("Cannot derive pid from " + processIdAndHostname);
    }
    
    
    private static String execOsCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[512];
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            process.waitFor(5, TimeUnit.SECONDS);
            int exitValue = process.exitValue();
            if (exitValue == 0) {
                return result.toString(StandardCharsets.UTF_8);
            } else {
                return null;
            }
        } catch (IOException | InterruptedException ex) {
            return null;
        }
    }
}

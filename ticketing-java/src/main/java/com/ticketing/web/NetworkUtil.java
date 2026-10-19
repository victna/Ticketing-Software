package com.ticketing.web;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finds this computer's addresses on the local network (like 192.168.1.25). */
public final class NetworkUtil {

    private NetworkUtil() {
    }

    public static List<String> lanAddresses() {
        List<String> found = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        found.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // No network information available: return what we have
        }
        return found;
    }
}


package com.FreshE;

import org.shortpasta.icmp2.IcmpPingRequest;
import org.shortpasta.icmp2.IcmpPingResponse;
import org.shortpasta.icmp2.IcmpPingUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Simple traceroute tool. Scribbled this together just to see how it would work after reading about ICMP in my data
 * communication coursebook. Uses ICMP echo requests with an incrementing TTL value in order to trace the route to a
 * chosen destination.
 *
 * Uses the icmp4j library (http://www.icmp4j.org) for ping functionality.
 *
 * Example usage: java SimpleTraceroute www.gp.se
 *
 * Tracing route to 104.16.104.16 (www.gp.se) with a maximum of 30 hops
 *
 * [#hops]  RTT   [IP]
 * ------------------------------------------
 * [1]      2 ms  192.168.0.1
 * [2]      19 ms  213.200.143.10
 * [3]      18 ms  213.200.168.7
 * [4]      25 ms  213.200.162.23
 * [5]      27 ms  213.200.162.44
 * [6]      27 ms  212.237.192.246
 * [7]      28 ms  104.16.104.16
 *
 * Trace complete.
 *
 * @author Eric Andersson
 * @version 2018-04-03
 */
public class SimpleTraceroute {

    private InetAddress destAddress;
    private int ttl = 1;
    private int fails = 0;
    private boolean destReached = false;

    private final int MAX_TIMEOUTS = 5;
    private final int MAX_HOPS = 30;

    public static void main(String[] args) {
        SimpleTraceroute tr = new SimpleTraceroute();

        try {
            tr.parseArg(args);
        } catch(UnknownHostException e) {
            System.out.println("Unknown host error\nUsage: java SimpleTraceroute <destination>\n<destination> must be a valid IP address or domain name");
            System.exit(1);
        }

        tr.run();
    }

    private void run() {
        System.out.println("Tracing route to " + destAddress.getHostAddress() + " (" + destAddress.getHostName()
                            + ") with a maximum of " + MAX_HOPS + " hops\n");
        System.out.println("[#hops]  RTT   [IP]");
        System.out.println("------------------------------------------");

        while(!destReached && ttl < MAX_HOPS) {
            ping();
            ttl++;
        }

        System.out.println("\nTrace complete.");
        System.exit(0);
    }

    private void ping() {
        IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
        request.setHost(destAddress.getHostAddress());
        request.setTtl(ttl);

        IcmpPingResponse response = IcmpPingUtil.executePingRequest(request);
        if(response.getTimeoutFlag()) {
            fails++;
            System.out.println("[" + ttl + "]      *     Request timed out.");
            if(fails == MAX_TIMEOUTS) {
                System.out.println("Five timeouts in a row, assuming we have reached a blocking firewall.\n\nTrace aborted.");
                System.exit(1);
            }
        } else {
            fails = 0;
            System.out.println("[" + ttl + "]" + "      " + response.getRtt() + " ms  " + response.getHost());
        }

        if(response.getSuccessFlag())
            destReached = true;
    }

    private void parseArg(String[] args) throws UnknownHostException {
        if(args.length != 1 || args[0].equals("?")) {
            System.out.println("Usage: java SimpleTraceroute <destination>");
            System.exit(1);
        }
        destAddress = InetAddress.getByName(args[0]);
    }
}
package io.reactivej.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * @author heartup@gmail.com
 */
public class HostUtil {
    private static Logger log = LoggerFactory.getLogger(HostUtil.class);

    public HostUtil() {
    }

    public static List<String> getLocalIPList() {
        ArrayList ipList = new ArrayList();

        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();

            while(e.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface)e.nextElement();
                Enumeration inetAddresses = networkInterface.getInetAddresses();

                while(inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = (InetAddress)inetAddresses.nextElement();
                    if(inetAddress != null && inetAddress instanceof Inet4Address) {
                        String ip = inetAddress.getHostAddress();
                        ipList.add(ip);
                    }
                }
            }
        } catch (SocketException var6) {
            log.warn("retrieve localhost ip failed", var6);
        }

        return ipList;
    }

    public static String getSuitLocalAddress() {
        List addressList = getLocalIPList();
        String suitAddress = null;
        Iterator i$ = addressList.iterator();

        String tmpAdd;
        while (i$.hasNext()) {
            tmpAdd = (String) i$.next();
            if (!tmpAdd.equalsIgnoreCase("127.0.0.1")) {
                suitAddress = tmpAdd;
                break;
            }
        }

        return suitAddress;
    }
}

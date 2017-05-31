package com.yd.container;

import com.yd.device.DeviceManager;
import com.yd.monkey.MKConfig;
import com.yd.zkReg.ZKCenter;
import org.apache.curator.test.KillSession;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by liuhailin on 2017/4/17.
 */
public class Application {

    public final static String ROOT = "/" + getIpAddress();
    public final static String APK = "/" + getIpAddress() + "/app/apk";
    public final static String MONKEYCMD = "/" + getIpAddress() + "/app/monkeycmd";
    public final static String PACKAGE = "/" + getIpAddress() + "/app/package";
    public final static String RUNTIMES = "/" + getIpAddress() + "/task/runtimes";
    public final static String DEVICES = "/" + getIpAddress() + "/devices";
    public final static String RET = "/" + getIpAddress() + "/reset";

    private DeviceManager manager;

    private ZKCenter center;

    public Application(DeviceManager manager, ZKCenter center) {
        this.manager = manager;
        this.center = center;
    }

    public void init(MKConfig config) {
        initPath(config);
    }

    public void destroy() {
        closeROOT();
    }

    private void closeROOT() {
        center.deleteNode(ROOT);
    }

    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = null;
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresss = ni.getInetAddresses();
                while (addresss.hasMoreElements()) {
                    InetAddress nextElement = addresss.nextElement();
                    if (nextElement != null && nextElement instanceof Inet4Address) {
                        if (!nextElement.getHostAddress().startsWith("127.0")) {
                            return nextElement.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initPath() {
        center.createNodeIfNotExist(APK, CreateMode.PERSISTENT);
        center.createNodeIfNotExist(MONKEYCMD, CreateMode.PERSISTENT);
        center.createNodeIfNotExist(PACKAGE, CreateMode.PERSISTENT);
        center.createNodeIfNotExist(RUNTIMES, CreateMode.PERSISTENT);
        center.createNodeIfNotExist(DEVICES, CreateMode.PERSISTENT);
        center.createNode(RET, "0");
    }

    private void initPath(MKConfig config) {

        if (config == null) {
            initPath();
        } else {
            center.createNode(APK, config.getApkPath(), CreateMode.PERSISTENT);
            center.createNode(MONKEYCMD, config.getMonkeyCMD(), CreateMode.PERSISTENT);
            center.createNode(PACKAGE, config.getPackageName(), CreateMode.PERSISTENT);
            center.createNode(RUNTIMES, String.valueOf(config.getRunTimes()), CreateMode.PERSISTENT);
            center.createNodeIfNotExist(DEVICES, CreateMode.PERSISTENT);
            center.createNode(RET, "0");
        }

    }

    public void resetAllDevices() {
        manager.getOnLineDevice().clear();
    }

    public void resetDevice(String deviceId) {
        manager.getOnLineDevice().remove(deviceId);
    }

    public static void main(String[] args) {
        getIpAddress();
    }
}

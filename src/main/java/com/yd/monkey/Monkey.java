package com.yd.monkey;

import com.yd.container.Application;
import com.yd.device.DeviceEntry;
import com.yd.listener.IListener;

import com.yd.zkReg.ZKCenter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by liuhailin on 2017/4/10.
 */
@Slf4j
public abstract class Monkey implements Runnable {

    private DeviceEntry deviceEntry;
    private MKConfig config;
    private IListener listener;
    private ZKCenter center;

    public Monkey(DeviceEntry deviceEntry, MKConfig config, IListener listener, ZKCenter center) {
        this.deviceEntry = deviceEntry;
        this.config = config;
        this.listener = listener;
        this.center = center;
    }

    public abstract void runMK(DeviceEntry deviceEntry, MKConfig config);

    @Override
    public void run() {

        listener.addListener(deviceEntry, config.getPackageName());
        String status = Application.DEVICES + "/" + deviceEntry.getDeviceId() + "/" + "status";
        center.updateNode(status, "running");

        runMK(deviceEntry, config);

        synchronized (deviceEntry) {
            try {
                deviceEntry.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        listener.removeListenr(deviceEntry);
        center.updateNode(status, "stop");
    }

}

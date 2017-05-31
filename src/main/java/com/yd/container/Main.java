package com.yd.container;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.yd.commend.CommendManager;
import com.yd.device.DeviceManager;
import com.yd.download.DownloadFileFromFile;
import com.yd.monkey.MKConfig;
import com.yd.zkReg.ZKCenter;
import com.yd.zkReg.ZKConifg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by liuhailin on 2017/4/7.
 */
@Slf4j
public class Main {
    private static volatile boolean running = true;

    /**
     * 配置zk中心，注册config配置，通过web改配置后，重启DeviceManager
     *
     * @param args
     */
    public static void main(String[] args) {

        final Config config = ConfigFactory.load();
        int argLen = args.length;
        ZKCenter zkTemp = null;
        boolean enableRemoteZK = true;
        if (argLen > 0 && args[0].equals("-c")) {
            enableRemoteZK = false;
            zkTemp = new ZKCenter(null, true);
            zkTemp.init();
        } else {
            final ZKConifg zk = ZKConifg.builder().servers(config.getStringList("zk.servers"))
                    .nameSpace(config.getString("zk.namespace")).connectionTimeOut(10000).sessionTimeOut(5000).build();

            zkTemp = new ZKCenter(zk, false);
            zkTemp.init();
        }
        if (zkTemp == null) {
            log.error("ZKCenter init has Error");
            return;
        }
        final ZKCenter zkCenter = zkTemp;

        final MKConfig mk = MKConfig.builder().apkPath(config.getString("app.apk")).packageName(config.getString("app.package"))
                .monkeyCMD(config.getString("app.monkey-cmd")).catLogCMD(config.getString("app.catlog-cmd"))
                .runTimes(config.getInt("app.monkey-run-times")).build();


        final CommendManager commendManager = new CommendManager();
        final DeviceManager deviceManager = new DeviceManager(commendManager, zkCenter);
        final Application application = new Application(deviceManager, zkCenter);

        if (enableRemoteZK) {
            application.init(null);
        } else {
            application.init(mk);
        }


        deviceManager.init(mk);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Ready to close..");
                commendManager.close();
                deviceManager.destroy();
                application.destroy();
                zkCenter.close();
                synchronized (Main.class) {
                    running = false;
                    Main.class.notify();
                }

            }
        });

        synchronized (Main.class) {
            while (running) {
                try {
                    Main.class.wait();
                    log.info("close...........done!!");
                } catch (Throwable e) {
                }
            }
        }

    }

}

package com.yd.monkey;

import com.yd.device.DeviceEntry;
import com.yd.device.IDevice;
import com.yd.download.DownloadFileFromFile;
import com.yd.listener.IListener;
import com.yd.zkReg.ZKCenter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Created by liuhailin on 2017/4/10.
 */
@Slf4j
public class MonkeyRunner extends Monkey {

    private IDevice device;

    public MonkeyRunner(IDevice device, DeviceEntry deviceEntry, MKConfig config, IListener listener, ZKCenter center) {
        super(deviceEntry, config, listener, center);
        this.device = device;
    }

    @Override
    public void runMK(DeviceEntry deviceEntry, MKConfig config) {

        boolean isInstall = device.checkIsInstall(deviceEntry, config.getPackageName());

        if (!isInstall) {
            device.installApp(deviceEntry, config.getApkPath());
        }
        String localPath = MonkeyRunner.class.getClassLoader().getResource("").getPath();

        String mkLog = localPath + "logs/monkey/" + deviceEntry.getDeviceId() + ".log";


        File mkLogFile = new File(mkLog);
        if (mkLogFile.exists()) {
            mkLogFile.delete();
        }

        if (!mkLogFile.getParentFile().exists()) {
            mkLogFile.getParentFile().mkdirs();
        }

        String catLog = localPath + "logs/catlog/" + deviceEntry.getDeviceId() + ".log";

        File catLogDir = new File(catLog);
        if (catLogDir.exists()) {
            mkLogFile.delete();
        }
        if (!catLogDir.getParentFile().exists()) {
            catLogDir.getParentFile().mkdirs();
        }

        // 跑monkey
        String mk_cmd = "adb -s " + deviceEntry.getDeviceId() + " " + config.getMonkeyCMD();
        String logcat_cmd = "adb -s " + deviceEntry.getDeviceId() + " " + config.getCatLogCMD();

        device.runCmd(logcat_cmd, catLogDir);

        device.runCmd(deviceEntry, mk_cmd, mkLogFile);

    }
}

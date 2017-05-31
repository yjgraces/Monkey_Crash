package com.yd.device;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.yd.container.Application;
import com.yd.download.DownloadFileFromFile;
import com.yd.listener.CrashLog;
import com.yd.listener.IListener;
import com.yd.listener.Performance;
import com.yd.monkey.MKConfig;
import com.yd.monkey.MonkeyRunner;
import com.yd.zkReg.ZKCenter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.yd.commend.CommendManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;

/**
 * Created by liuhailin on 2017/4/7.
 */
@Slf4j
public class DeviceManager {

    private final Map<String, DeviceEntry> onLine = Maps.newConcurrentMap();

    private final Map<String, DeviceEntry> readyToRun = Maps.newConcurrentMap();

    private final Map<String, Integer> runInfo = Maps.newConcurrentMap();

    private final Map<String, String> apkPathMap = Maps.newConcurrentMap();

    public Map<String, DeviceEntry> getOnLineDevice() {
        return onLine;
    }

    private IDevice deviceOp;

    private Performance performance;

    private CrashLog crashLog;

    private MKConfig config;

    private ZKCenter zkCenter;

    private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    private static ExecutorService MonkeyService = Executors.newFixedThreadPool(20);

    public DeviceManager(CommendManager commendManager, ZKCenter zk) {
        deviceOp = new DeviceService(commendManager);
        performance = new Performance(commendManager);
        crashLog = new CrashLog(commendManager);
        zkCenter = zk;
    }

    private boolean initPrecoditionData() {
        String apkPath = zkCenter.getNodeData(Application.APK);
        String packageName = zkCenter.getNodeData(Application.PACKAGE);
        String rt = zkCenter.getNodeData(Application.RUNTIMES);
        String monkey_cmd = zkCenter.getNodeData(Application.MONKEYCMD);
        log.info("[Find APK URL:]:{}", apkPath);
        try {
            if (StringUtils.isNotEmpty(apkPath) && StringUtils.isNotEmpty(packageName)
                    && StringUtils.isNotEmpty(monkey_cmd)) {
                if (!apkPathMap.containsKey(packageName)) {
                    log.info("downloading file:{}", apkPath);
                    String path = DownloadFileFromFile.download(apkPath, "app/");
                    config.setApkPath(path);
                    apkPathMap.put(packageName, path);
                    config.setRunTimes(Integer.parseInt(rt));
                    config.setPackageName(packageName);
                    config.setMonkeyCMD(monkey_cmd);
                }


                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void init(MKConfig config) {
        this.config = config;
        service.scheduleAtFixedRate(new ScanDevice(), 0, 30, TimeUnit.SECONDS);
    }

    private void checkAPK(MKConfig config) {
        Preconditions.checkNotNull(config.getApkPath());
    }

    public void destroy() {
        service.shutdown();
        performance.destroy();
        crashLog.destroy();
    }

    public void onLineDevice(DeviceEntry entry, String appName) {
        performance.addListener(entry, appName);
        crashLog.addListener(entry, appName);
        onLine.put(entry.getDeviceId(), entry);
    }

    public void offLineDevice(DeviceEntry entry) {

        String id = entry.getDeviceId();
        Integer t = runInfo.get(id);
        int it = t == null ? 1 : t.intValue();
        int times = config.getRunTimes();

        log.info("TaskNum:{}**************Stop**Monkey Task Device:{}", it, JSON.toJSONString(entry));
        // 注册zk
        // 关闭监听
        performance.removeListenr(entry);

        crashLog.removeListenr(entry);

        if (times > 0 && times > it) {
            startMonkeyOnDevice(entry, it + 1);
            runInfo.put(id, it + 1);
            return;
        }
    }

    class ScanDevice implements Runnable {

        @Override
        public void run() {
            log.info("Start Scaning Device...");
            boolean conditionIsReady = initPrecoditionData();
            Map<String, DeviceEntry> newLine = Maps.newHashMap();
            List<DeviceEntry> deviceEntries = deviceOp.getAllDevice();
            for (DeviceEntry d : deviceEntries) {
                newLine.put(d.getDeviceId(), d);
                if (!onLine.containsKey(d.getDeviceId())) {
                    log.info("[*] New Device onLine:{}", JSON.toJSONString(d));
                    onLine.put(d.getDeviceId(), d);
                    // 注册ZK
                    // 执行流程
                    // 添加监听
                    String id = Application.DEVICES + "/" + d.getDeviceId() + "/" + "id";
                    String info = Application.DEVICES + "/" + d.getDeviceId() + "/" + "info";
                    String online = Application.DEVICES + "/" + d.getDeviceId() + "/" + "onLine";
                    String status = Application.DEVICES + "/" + d.getDeviceId() + "/" + "status";
                    zkCenter.createNode(status, "new", CreateMode.PERSISTENT);
                    zkCenter.createNode(id, d.getDeviceId(), CreateMode.PERSISTENT);
                    zkCenter.createNode(info, JSON.toJSONString(d), CreateMode.PERSISTENT);
                    zkCenter.createNode(online, "true", CreateMode.PERSISTENT);

                    if (conditionIsReady) {

                        startMonkeyOnDevice(d, 1);
                    } else {
                        readyToRun.put(d.getDeviceId(), d);
                        return;
                    }
                }
            }
            if (conditionIsReady) {
                for (Map.Entry<String, DeviceEntry> entry : readyToRun.entrySet()) {
                    startMonkeyOnDevice(entry.getValue(), 1);
                    readyToRun.remove(entry.getKey());
                }
            }
            MapDifference diff = Maps.difference(newLine, onLine);

            Map<String, DeviceEntry> offLine = diff.entriesOnlyOnRight();

            log.info("********debug offLine:{}", JSON.toJSONString(offLine));

            if (!offLine.isEmpty()) {
                for (Map.Entry<String, DeviceEntry> entry : offLine.entrySet()) {
                    onLine.remove(entry.getKey());
                    zkCenter.deleteNode(Application.DEVICES + "/" + entry.getKey());
                    log.info("[*] OffLine Device:{}", JSON.toJSONString(entry));
                }
            }

        }
    }

    private void startMonkeyOnDevice(DeviceEntry d, int circle) {
        log.info("TaskNum:{}**************Start**Monkey Task Device:{}", circle, JSON.toJSONString(d));
        MonkeyService.execute(new MonkeyRunner(deviceOp, d, config, new IListener() {
            @Override
            public boolean addListener(DeviceEntry device, String appName) {
                onLineDevice(device, appName);
                return true;
            }

            @Override
            public boolean removeListenr(DeviceEntry device) {
                offLineDevice(device);
                return true;
            }
        }, zkCenter));
    }
}

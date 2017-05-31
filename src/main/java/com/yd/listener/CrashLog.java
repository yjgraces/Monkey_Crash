package com.yd.listener;

import com.google.common.collect.Maps;
import com.yd.commend.CommendManager;
import com.yd.device.DeviceEntry;
import com.yd.monkey.MKConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by liuhailin on 2017/4/11.
 */
@Slf4j
public class CrashLog extends UploadListener {

    private String NullPointer = "NullPointerException";
    private String IllegalState = "IllegalStateException";
    private String IllegalArgument = "IllegalArgumentException";
    private String ArrayIndexOutOfBounds = "ArrayIndexOutOfBoundsException";
    private String RuntimeException = "RuntimeException";
    private String SecurityException = "SecurityException";
    private String ArithmeticException = "ArithmeticException";

    private Map<String, Future> runingJob = Maps.newConcurrentMap();

    private static ExecutorService service = Executors.newFixedThreadPool(10);

    public CrashLog(CommendManager commendManager) {
        super(commendManager);
    }

    @Override
    public boolean addListener(DeviceEntry device, String appName) {
        String localPath = CrashLog.class.getClassLoader().getResource("").getPath();
        String catLog = localPath + "logs/catlog/" + device.getDeviceId() + ".log";
        File catLogFile = new File(catLog);

        Future future = service.submit(new AnaylyzeLog(catLogFile, device));
        runingJob.put(device.getDeviceId(), future);
        return true;
    }

    public void destroy() {
        service.shutdown();
    }

    @Override
    public boolean removeListenr(DeviceEntry device) {
        String deviceId = device.getDeviceId();

        Future task = runingJob.get(deviceId);

        if (task != null) {
            boolean r = task.cancel(true);
            if (r) {
                log.info("[remove crash log listener ]:device-id:{}", deviceId);
                runingJob.remove(deviceId);
            }
            return r;
        }

        return false;
    }

    class AnaylyzeLog implements Runnable {

        private File catlog;

        private DeviceEntry deviceEntry;

        public AnaylyzeLog(File catlog, DeviceEntry deviceEntry) {
            this.catlog = catlog;
            this.deviceEntry = deviceEntry;
        }

        @Override
        public void run() {
            while (!catlog.exists()) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            log.info("*************************crash log anaylze*************************");
            BufferedInputStream fis = null;
            BufferedReader reader = null;
            StringBuilder sb = new StringBuilder();
            try {
                fis = new BufferedInputStream(new FileInputStream(catlog));
                reader = new BufferedReader(new InputStreamReader(fis, "utf-8"), 1 * 1024 * 1024);
                String line = "";
                while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {

                    if (line.contains(NullPointer) || line.contains(IllegalState) || line.contains(IllegalArgument)
                            || line.contains(ArrayIndexOutOfBounds) || line.contains(RuntimeException)
                            || line.contains(SecurityException) || line.contains(ArithmeticException)) {

                        sb.append(line).append("\n");
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fis.close();
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            log.info("Error" + sb.toString());
            // boolean isS = uploadInfo(params);

        }
    }
}

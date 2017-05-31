package com.yd.download;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by liuhailin on 2017/4/14.
 */
public class DownloadFileFromFile {

    public static String download(String url, String localPath) throws IOException, URISyntaxException {

        String fileName = getFileNameFromUrl(url);
        URL local_url = DownloadFileFromFile.class.getClassLoader().getResource("");
        File saveFile = new File(local_url.getPath() + localPath + fileName);
        if (url.startsWith("http:")) {
            URL fileUrl = new URL(url);

            // // 文件保存位置
            if (saveFile.exists()) {
                saveFile.delete();
            }
            FileUtils.copyURLToFile(fileUrl, saveFile);
        }

        return saveFile.getAbsolutePath();
    }

    private static String getFileNameFromUrl(String url) {
        String name = System.currentTimeMillis() + ".apk";
        int index = url.lastIndexOf("/");
        if (index > 0) {
            name = url.substring(index + 1);
            if (name.trim().length() > 0) {
                return name;
            }
        }
        return name;
    }
}

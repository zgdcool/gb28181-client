package com.wydpp.gb28181.commander;

import com.wydpp.config.SystemConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class FfmpegCommander implements IFfmpegCommander {

    private final Logger logger = LoggerFactory.getLogger(FfmpegCommander.class);

    @Autowired
    private SystemConfig systemConfig;

    private static final Map<String, Process> processMap = new ConcurrentHashMap<>();

    public String pushStream(String callId, String filePath, String ip, int port, String payloadType, String ssrc) {
        String command = systemConfig.getFfmpegPath() + " " +
                systemConfig.getFfmpegPushStreamCmd()
                        .replace("{filePath}", filePath)
                        .replace("{ip}", ip)
                        .replace("{port}", String.valueOf(port))
                        .replace("{payloadType}", payloadType)
                        .replace("{ssrc}", ssrc);
        logger.info("callId={},\r\n推流命令={}", callId, command);
        try {
            new Thread(() -> {
                int code = 0;
                try {
                    Process process = new ProcessBuilder("/bin/sh", "-lc", command)
                            .command("/bin/sh", "-lc", "exec " + command)
                            .redirectErrorStream(true)
                            .start();
                    processMap.put(callId, process);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    String str;
                    while ((str = reader.readLine()) != null) {
                        logger.info("[ffmpeg] {}", str);
                    }
                    code = process.waitFor();
                    processMap.remove(callId);
                    logger.info("推流已结束, callId={}, exitCode={}", callId, code);
                } catch (Exception e) {
                    logger.error("ffmpeg推流异常!", e);
                }
            }).start();
            return command;
        } catch (Exception e) {
            logger.error("创建ffmpeg推流进程失败!", e);
        }
        return "";
    }

    public void closeStream(String callId) {
        logger.info("关闭推流:{}", callId);
        if (StringUtils.isEmpty(callId)) {
            closeAllStream();
        } else if (processMap.containsKey(callId)) {
            stopProcess(callId, processMap.remove(callId));
        } else {
            logger.info("没有推流要关闭!");
        }
    }

    public void closeAllStream() {
        logger.info("关闭所有推流");
        processMap.forEach(this::stopProcess);
        processMap.clear();
    }

    private void stopProcess(String callId, Process process) {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS) && process.isAlive()) {
                logger.warn("推流进程未正常退出，强制关闭, callId={}", callId);
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("等待推流进程退出被中断, callId={}", callId, e);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}

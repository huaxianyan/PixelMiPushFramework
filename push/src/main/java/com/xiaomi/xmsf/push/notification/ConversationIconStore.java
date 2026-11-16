package com.xiaomi.xmsf.push.notification;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 保存每个头像地址最近一次成功下载的图片。
 *
 * <p>通知头像来自网络，抓取失败时框架原本只能放弃。这里按地址哈希落盘，失败时取回上一次
 * 成功的结果，避免同一会话因为一次网络问题长期没有头像。文件只在成功下载后写入，
 * 超过 {@link #MAX_AGE_MS} 未更新的条目在下次写入时清理。
 */
final class ConversationIconStore {

    private static final long MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final File directory;

    ConversationIconStore(File directory) {
        this.directory = directory;
    }

    byte[] read(String key) {
        File file = fileFor(key);
        if (file == null || !file.isFile()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream((int) file.length())) {
            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    void write(String key, byte[] image) {
        File file = fileFor(key);
        if (file == null || image == null || image.length == 0) {
            return;
        }
        if (!directory.isDirectory() && !directory.mkdirs()) {
            return;
        }
        File temporary = new File(directory, file.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(image);
        } catch (IOException e) {
            temporary.delete();
            return;
        }
        // Rename does not replace an existing file on every platform, so drop the old copy first.
        file.delete();
        if (!temporary.renameTo(file)) {
            temporary.delete();
            return;
        }
        deleteExpired();
    }

    private void deleteExpired() {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
        for (File file : files) {
            if (file.lastModified() < cutoff) {
                file.delete();
            }
        }
    }

    private File fileFor(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            char[] name = new char[digest.length * 2 + 4];
            for (int i = 0; i < digest.length; i++) {
                int value = digest[i] & 0xFF;
                name[i * 2] = HEX[value >>> 4];
                name[i * 2 + 1] = HEX[value & 0x0F];
            }
            name[name.length - 4] = '.';
            name[name.length - 3] = 'p';
            name[name.length - 2] = 'n';
            name[name.length - 1] = 'g';
            return new File(directory, new String(name));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}

package com.xiaomi.push.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.xiaomi.channel.commonutils.file.IOUtils;
import com.xiaomi.channel.commonutils.logger.MyLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/* loaded from: classes.dex */
public class MyNotificationIconHelper {
    public static final int KiB = 1024;
    public static final int MiB = 1024 * KiB;

    private static final int CONNECT_TIMEOUT = 8000;
    private static final int READ_TIMEOUT = 20000;
    private static final int READ_UNIT = 1024;
    private static final int STANDARD_DENSITY = 160;
    private static final int STANDARD_ICON_SIZE = 48;
    private static final int SERVER_ERROR_STATUS = 500;
    private static final int SERVER_ERROR_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 400;

    /* loaded from: classes.dex */
    public static class GetIconResult {
        public Bitmap bitmap;
        public long downloadSize;

        public GetIconResult(Bitmap bitmap, long downloadSize) {
            this.bitmap = bitmap;
            this.downloadSize = downloadSize;
        }
    }

    public static GetIconResult getIconFromUrl(Context context, String urlStr, int maxDownloadBytes) {
        InputStream isForBitmapSize = null;
        GetIconResult result = new GetIconResult(null, 0L);
        try {
            GetDataResult getDataResult = getDataFromUrl(urlStr, maxDownloadBytes);
            if (getDataResult != null) {
                result.downloadSize = getDataResult.downloadSize;
                byte[] data = getDataResult.data;
                if (data != null) {
                    InputStream isForBitmapSize2 = new ByteArrayInputStream(data);
                    try {
                        int sampleSize = getSampleSize(context, isForBitmapSize2);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = sampleSize;
                        result.bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                        isForBitmapSize = isForBitmapSize2;
                    } catch (Exception e) {
                        isForBitmapSize = isForBitmapSize2;
                        MyLog.e(e);
                        IOUtils.closeQuietly(isForBitmapSize);
                        return result;
                    } catch (Throwable th) {
                        isForBitmapSize = isForBitmapSize2;
                        IOUtils.closeQuietly(isForBitmapSize);
                        throw th;
                    }
                }
            }
        } catch (Throwable throwable) {
            MyLog.w("Avatar download failed at " + hostOf(urlStr) + ": " + throwable);
        }
        IOUtils.closeQuietly(isForBitmapSize);
        return result;
    }

    private static String hostOf(String urlStr) {
        try {
            String host = Uri.parse(urlStr).getHost();
            return host != null ? host : "unknown host";
        } catch (Throwable throwable) {
            return "unknown host";
        }
    }

    /* loaded from: classes.dex */
    public static class GetDataResult {
        byte[] data;
        int downloadSize;

        public GetDataResult(byte[] data, int downloadSize) {
            this.data = data;
            this.downloadSize = downloadSize;
        }
    }

    /**
     * 头像 CDN 偶尔返回 502 或 503，稍后重试通常就能拿到图片。只对服务端错误重试，
     * 连接失败和 4xx 直接放弃，避免把通知构建拖长。
     */
    private static GetDataResult getDataFromUrl(String urlStr, int maxDownloadBytes) {
        for (int attempt = 1; ; attempt++) {
            Download download = downloadOnce(urlStr, maxDownloadBytes);
            if (download.result != null || download.statusCode < SERVER_ERROR_STATUS
                    || attempt >= SERVER_ERROR_ATTEMPTS) {
                return download.result;
            }
            MyLog.w("HTTP " + download.statusCode + " from " + hostOf(urlStr) + ", retrying");
            try {
                Thread.sleep(RETRY_DELAY_MS * attempt);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    private static Download downloadOnce(String urlStr, int maxDownloadBytes) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.connect();
            int contentLen = connection.getContentLength();
            if (contentLen > maxDownloadBytes) {
                MyLog.w("Bitmap size is too big, max size is " + maxDownloadBytes + "  contentLen size is " + contentLen + " from url " + urlStr);
                return new Download(null, 200);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                MyLog.w("Invalid Http Response Code " + responseCode + " received");
                return new Download(null, responseCode);
            }
            inputStream = connection.getInputStream();
            ByteArrayOutputStream tempOutStream = new ByteArrayOutputStream();
            int availableSpace = maxDownloadBytes;
            byte[] dataUnit = new byte[READ_UNIT];
            while (availableSpace > 0) {
                int readCount = inputStream.read(dataUnit, 0, READ_UNIT);
                if (readCount == -1) {
                    break;
                }
                availableSpace -= readCount;
                tempOutStream.write(dataUnit, 0, readCount);
            }
            if (availableSpace <= 0) {
                MyLog.w("length " + maxDownloadBytes + " exhausted.");
                return new Download(new GetDataResult(null, maxDownloadBytes), 200);
            }
            byte[] data = tempOutStream.toByteArray();
            return new Download(new GetDataResult(data, data.length), 200);
        } catch (IOException e) {
            MyLog.e(e);
            return new Download(null, 0);
        } finally {
            IOUtils.closeQuietly(inputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static final class Download {
        final GetDataResult result;
        final int statusCode;

        Download(GetDataResult result, int statusCode) {
            this.result = result;
            this.statusCode = statusCode;
        }
    }

    public static Bitmap getIconFromUri(Context context, String uriStr) {
        Bitmap bitmap = null;
        Uri uri = Uri.parse(uriStr);
        InputStream is = null;
        InputStream isForBitmapSize = null;
        try {
            try {
                isForBitmapSize = context.getContentResolver().openInputStream(uri);
                int sampleSize = getSampleSize(context, isForBitmapSize);
                is = context.getContentResolver().openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sampleSize;
                bitmap = BitmapFactory.decodeStream(is, null, options);
                IOUtils.closeQuietly(is);
            } catch (IOException e) {
                MyLog.e(e);
                IOUtils.closeQuietly(is);
            }
            IOUtils.closeQuietly(isForBitmapSize);
            return bitmap;
        } catch (Throwable th) {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(isForBitmapSize);
            throw th;
        }
    }

    private static int getSampleSize(Context context, InputStream inputStream) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, opt);
        if (opt.outWidth == -1 || opt.outHeight == -1) {
            MyLog.w("decode dimension failed for bitmap.");
            return 1;
        }
        int screenDensity = context.getResources().getDisplayMetrics().densityDpi;
        int targetWidth = Math.round((screenDensity / 160.0f) * 48.0f);
        if (opt.outWidth <= targetWidth || opt.outHeight <= targetWidth) {
            return 1;
        }
        return Math.min(opt.outWidth / targetWidth, opt.outHeight / targetWidth);
    }
}
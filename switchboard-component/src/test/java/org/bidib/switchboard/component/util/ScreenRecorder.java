package org.bidib.switchboard.component.util;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenRecorder implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenRecorder.class);

    private static boolean enabled = false;

    public static void setEnabled(boolean enabled) {
        ScreenRecorder.enabled = enabled;
    }

    private static final int FPS_10 = 10;

    // Frame rate presets
    public static final int FPS_24 = 24;

    public static final int FPS_30 = 30;

    public static final int FPS_60 = 60;

    private static final int BITRATE = 5000000;

    private final Robot robot;

    private final FFmpegFrameRecorder recorder;

    private final Rectangle area;

    private ScheduledExecutorService executor;

    private volatile boolean recording = false;

    // Buffers for color conversion
    private byte[] rgbBuffer;

    private ByteBuffer yuvBuffer;

    private int width;

    private int height;

    public ScreenRecorder(Rectangle area, Path output) throws Exception {

        this.width = area.width;
        this.height = area.height;

        // Ensure dimensions are even (required for YUV420P)
        if (width % 2 == 1) {
            width++;
        }
        if (height % 2 == 1) {
            height++;
        }

        // Pre-allocate buffers
        this.rgbBuffer = new byte[width * height * 3];
        this.yuvBuffer = ByteBuffer.allocateDirect(width * height * 3 / 2); // YUV420P size

        this.robot = new Robot();

        this.area = area;

        this.recorder = new FFmpegFrameRecorder(output.toFile(), area.width, area.height, 0);
        recorder.setFrameRate(FPS_24);
        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");

        // Use YUV420P (the only format supported by H.264 encoder)
        recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);

        recorder.setVideoBitrate(BITRATE); // e.g., 5000000 for 5 Mbps

        recorder.setVideoOption("preset", "medium");
        recorder.setVideoOption("crf", "18");

        // Add color space metadata for proper playback
        recorder.setVideoOption("colorspace", "bt709");
        recorder.setVideoOption("color_primaries", "bt709");
        recorder.setVideoOption("color_trc", "bt709");

        executor = Executors.newScheduledThreadPool(1);

        // recorder.start();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static ScreenRecorder startIfEnabled(Rectangle area, Path output) throws Exception {
        if (!enabled) {
            return null;
        }
        LOG.info("Recording started: {}", output);
        ScreenRecorder recorder = new ScreenRecorder(area, output);
        recorder.startRecording();
        return recorder;
    }

    public void startRecording() throws Exception {

        if (recording) {
            throw new IllegalStateException("Recording already started");
        }

        recorder.start();
        recording = true;

        // Capture frames at specified interval
        // For 30 FPS: capture every ~33ms
        long frameIntervalMs = 1000 / (long) recorder.getFrameRate();
        executor.scheduleAtFixedRate(this::captureFrame, 0, frameIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Capture a single frame and record it
     */
    private void captureFrame() {
        if (!recording) {
            return;
        }

        try {
            // Capture screen
            BufferedImage screenshot = robot.createScreenCapture(this.area);

            // KEY FIX: Convert BufferedImage to RGB24 byte buffer manually
            // This avoids color space issues with Java2DFrameConverter
            ByteBuffer rgbBuffer = convertBufferedImageToRGB24(screenshot);

            // Convert RGB24 → YUV420P with proper color space handling
            convertRGB24ToYUV420P();

            // Record with explicit RGB24 format
            recorder
                .recordImage(screenshot.getWidth(), screenshot.getHeight(), 8, // depth: 8 bits per channel
                    3, // channels: RGB = 3 channels
                    screenshot.getWidth() * 3, // stride
                    org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGB24, rgbBuffer);
        }
        catch (Exception e) {
            LOG.warn("Error capturing frame: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convert BufferedImage to RGB24 byte buffer (no color space conversion issues)
     */
    private ByteBuffer convertBufferedImageToRGB24(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] rgbData = new byte[width * height * 3];

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                // Extract RGB components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Store in RGB24 format (R, G, B)
                rgbData[index++] = (byte) r;
                rgbData[index++] = (byte) g;
                rgbData[index++] = (byte) b;
            }
        }

        return ByteBuffer.wrap(rgbData);
    }

    /**
     * Convert RGB24 to YUV420P with proper BT.709 color space This is the key to avoiding green/washed colors
     */
    private void convertRGB24ToYUV420P() {
        yuvBuffer.clear();

        int ySize = width * height;
        int uvSize = (width / 2) * (height / 2);

        // Create arrays for Y, U, V planes
        byte[] yPlane = new byte[ySize];
        byte[] uPlane = new byte[uvSize];
        byte[] vPlane = new byte[uvSize];

        // Convert each pixel with BT.709 color space
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbIndex = (y * width + x) * 3;
                int r = rgbBuffer[rgbIndex] & 0xFF;
                int g = rgbBuffer[rgbIndex + 1] & 0xFF;
                int b = rgbBuffer[rgbIndex + 2] & 0xFF;

                // BT.709 color space conversion (better for computer screens)
                // Y = 0.2126*R + 0.7152*G + 0.0722*B (luma)
                int yVal = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b + 16);
                yVal = Math.max(0, Math.min(255, yVal)); // Clamp to [16, 235]

                yPlane[y * width + x] = (byte) yVal;

                // Store U and V for 2x2 blocks (YUV420P is 2:1 subsampled)
                if ((x & 1) == 0 && (y & 1) == 0) {
                    int uvIndex = (y / 2) * (width / 2) + (x / 2);

                    // U = -0.09991*R - 0.33609*G + 0.436*B + 128 (chroma blue)
                    int uVal = (int) (-0.09991 * r - 0.33609 * g + 0.436 * b + 128);
                    uVal = Math.max(0, Math.min(255, uVal)); // Clamp to [16, 240]

                    // V = 0.615*R - 0.55861*G - 0.05639*B + 128 (chroma red)
                    int vVal = (int) (0.615 * r - 0.55861 * g - 0.05639 * b + 128);
                    vVal = Math.max(0, Math.min(255, vVal)); // Clamp to [16, 240]

                    uPlane[uvIndex] = (byte) uVal;
                    vPlane[uvIndex] = (byte) vVal;
                }
            }
        }

        // Copy Y, U, V planes to YUV buffer (YUV420P layout: Y plane, then U plane, then V plane)
        yuvBuffer.put(yPlane);
        yuvBuffer.put(uPlane);
        yuvBuffer.put(vPlane);
        yuvBuffer.flip();
    }

    /**
     * Stop recording and close resources
     */
    public void stopRecording() throws Exception {
        recording = false;

        // Stop executor
        executor.shutdown();
        boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        if (!terminated) {
            executor.shutdownNow();
        }

        // Flush and close recorder
        recorder.flush();
        recorder.stop();
        recorder.release();
    }

    @Override
    public void close() throws Exception {
        if (recording) {
            LOG.info("Recording saved");
            stopRecording();
        }
        else if (recorder != null) {
            recorder.release();
        }
    }
}

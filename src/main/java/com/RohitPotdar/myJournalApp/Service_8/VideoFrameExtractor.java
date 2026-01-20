package com.RohitPotdar.myJournalApp.Service_8;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
public class VideoFrameExtractor {

    public FrameResult extractFirstFrame(byte[] videoBytes) {
        List<String> warnings = new ArrayList<>();
        if (videoBytes == null || videoBytes.length == 0) {
            warnings.add("Video is empty; cannot extract frame.");
            return new FrameResult(null, warnings);
        }

        File tempFile = null;
        try {
            tempFile = Files.createTempFile("video-frame", ".mp4").toFile();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(videoBytes);
            }

            FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(tempFile));
            Picture picture = grab.getNativeFrame();
            if (picture == null) {
                warnings.add("Could not read a frame from the video.");
                return new FrameResult(null, warnings);
            }

            BufferedImage bufferedImage = convertToBufferedImage(picture, warnings);
            if (bufferedImage == null) {
                return new FrameResult(null, warnings);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", outputStream);
            return new FrameResult(outputStream.toByteArray(), warnings);
        } catch (IOException | JCodecException e) {
            warnings.add("Video frame extraction failed: " + e.getMessage());
            return new FrameResult(null, warnings);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                // Best-effort cleanup
                tempFile.delete();
            }
        }
    }

    public record FrameResult(byte[] jpegBytes, List<String> warnings) {}

    private BufferedImage convertToBufferedImage(Picture picture, List<String> warnings) {
        try {
            Class<?> awtUtil = Class.forName("org.jcodec.api.awt.AWTUtil");
            java.lang.reflect.Method method = awtUtil.getMethod("toBufferedImage", Picture.class);
            return (BufferedImage) method.invoke(null, picture);
        } catch (Exception e) {
            warnings.add("Frame conversion failed: " + e.getMessage());
            return null;
        }
    }
}

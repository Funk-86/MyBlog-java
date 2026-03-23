package org.example.myblog.serverl;

import org.example.myblog.mapper.PostMediaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService {

    @Value("${video.upload.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

    @Value("${video.upload.transcode.max-side:720}")
    private int videoTranscodeMaxSide;

    @Value("${video.upload.transcode.crf:28}")
    private int videoTranscodeCrf;

    @Value("${video.upload.transcode.audio-bitrate-k:96}")
    private int videoTranscodeAudioBitrateK;

    @Value("${video.upload.transcode.timeout-seconds:20}")
    private int videoTranscodeTimeoutSeconds;

    @Value("${upload.base-path:.}")
    private String uploadBasePath;

    @Autowired
    private PostMediaMapper postMediaMapper;

    private Path resolveUploadDir(String subdir) {
        Path base = Paths.get(uploadBasePath == null || uploadBasePath.isEmpty() ? "." : uploadBasePath).toAbsolutePath().normalize();
        return base.resolve(subdir);
    }

    private Path resolveVideoPath(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) return null;
        Path fileName = Paths.get(videoUrl).getFileName();
        if (fileName == null) return null;
        return resolveUploadDir("post_video").resolve(fileName.toString()).toAbsolutePath().normalize();
    }

    @Async("ffmpegExecutor")
    public void enqueueTranscode(String videoUrl) {
        Path inputPath = resolveVideoPath(videoUrl);
        if (inputPath == null) return;
        try {
            if (!Files.exists(inputPath) || Files.size(inputPath) <= 0) return;
            String fileName = inputPath.getFileName().toString();
            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : ".mp4";
            Path outputPath = Files.createTempFile("video-transcoded-", ext);
            Path ffmpegLog = Files.createTempFile("ffmpeg-transcode-", ".log");

            String vf = "scale='if(gt(iw,ih)," + videoTranscodeMaxSide + ",-2)':'if(gt(iw,ih),-2," + videoTranscodeMaxSide + ")':force_original_aspect_ratio=decrease";
            String crf = String.valueOf(videoTranscodeCrf);
            String ab = String.valueOf(videoTranscodeAudioBitrateK) + "k";

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegBin,
                    "-y",
                    "-i", inputPath.toString(),
                    "-vf", vf,
                    "-c:v", "libx264",
                    "-crf", crf,
                    "-preset", "veryfast",
                    "-c:a", "aac",
                    "-b:a", ab,
                    "-movflags", "+faststart",
                    outputPath.toString()
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ffmpegLog.toFile());

            Process p = pb.start();
            boolean finished = p.waitFor(Math.max(5, videoTranscodeTimeoutSeconds), TimeUnit.SECONDS);
            if (!finished) {
                try {
                    p.destroyForcibly();
                    p.waitFor(3, TimeUnit.SECONDS);
                } catch (Exception ignore) {}
                Files.deleteIfExists(outputPath);
                return;
            }
            if (p.exitValue() != 0 || !Files.exists(outputPath) || Files.size(outputPath) <= 0) {
                Files.deleteIfExists(outputPath);
                return;
            }
            Files.move(outputPath, inputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignore) {
        }
    }

    @Async("ffmpegExecutor")
    public void enqueueExtractFirstFrameCover(Long postId, String videoUrl) {
        if (postId == null) return;
        Path videoPath = resolveVideoPath(videoUrl);
        if (videoPath == null) return;
        try {
            if (!Files.exists(videoPath) || Files.size(videoPath) <= 0) return;
            Path imageDir = resolveUploadDir("post_img");
            Files.createDirectories(imageDir);
            String coverName = UUID.randomUUID() + ".jpg";
            Path coverPath = imageDir.resolve(coverName).toAbsolutePath().normalize();
            Path ffmpegLog = Files.createTempFile("ffmpeg-cover-", ".log");

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegBin,
                    "-y",
                    "-ss", "00:00:00",
                    "-i", videoPath.toString(),
                    "-frames:v", "1",
                    "-q:v", "3",
                    coverPath.toString()
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ffmpegLog.toFile());

            Process p = pb.start();
            boolean finished = p.waitFor(Math.max(5, videoTranscodeTimeoutSeconds), TimeUnit.SECONDS);
            if (!finished) {
                try {
                    p.destroyForcibly();
                    p.waitFor(3, TimeUnit.SECONDS);
                } catch (Exception ignore) {}
                return;
            }
            if (p.exitValue() != 0 || !Files.exists(coverPath) || Files.size(coverPath) <= 0) return;
            postMediaMapper.updateVideoCoverIfMissing(postId, "/post_img/" + coverName);
        } catch (Exception ignore) {
        }
    }
}

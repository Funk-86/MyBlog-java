package org.example.myblog.serverl;

import org.example.myblog.mapper.PostMediaMapper;
import org.example.myblog.storage.AliyunOssClientFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService {

    @Autowired(required = false)
    private AliyunOssClientFacade aliyunOssClientFacade;

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

    /**
     * 本地 post_video 路径，或 OSS 公开 URL 时下载到临时文件供 ffmpeg 使用。
     */
    private Path resolveVideoPath(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            return null;
        }
        String trimmed = videoUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                return AliyunOssClientFacade.downloadHttpToTempFile(trimmed);
            } catch (Exception e) {
                return null;
            }
        }
        Path fileName = Paths.get(trimmed).getFileName();
        if (fileName == null) {
            return null;
        }
        return resolveUploadDir("post_video").resolve(fileName.toString()).toAbsolutePath().normalize();
    }

    private boolean isHttpVideoUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            return false;
        }
        String t = videoUrl.trim();
        return t.startsWith("http://") || t.startsWith("https://");
    }

    @Async("ffmpegExecutor")
    public void enqueueTranscode(String videoUrl) {
        boolean remote = isHttpVideoUrl(videoUrl);
        String ossKey = remote && aliyunOssClientFacade != null
                ? aliyunOssClientFacade.keyFromPublicUrl(videoUrl.trim())
                : null;
        Path inputPath = resolveVideoPath(videoUrl);
        if (inputPath == null) {
            return;
        }
        try {
            if (!Files.exists(inputPath) || Files.size(inputPath) <= 0) {
                return;
            }
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
                } catch (Exception ignore) {
                }
                Files.deleteIfExists(outputPath);
                return;
            }
            if (p.exitValue() != 0 || !Files.exists(outputPath) || Files.size(outputPath) <= 0) {
                Files.deleteIfExists(outputPath);
                return;
            }
            if (remote && aliyunOssClientFacade != null && ossKey != null) {
                aliyunOssClientFacade.uploadLocalFile(outputPath, ossKey, "video/mp4");
                Files.deleteIfExists(outputPath);
            } else {
                Files.move(outputPath, inputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignore) {
        } finally {
            if (remote && inputPath != null) {
                try {
                    Files.deleteIfExists(inputPath);
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * 从已落盘视频文件截取一帧为 JPG 写入 post_img，返回相对路径如 /post_img/xxx.jpg；失败返回 null。
     * 用于上传视频后立即出封面（发帖前即可带回 coverUrl），与异步补封面共用逻辑。
     */
    public String extractCoverToPostImgDir(Path videoPath) {
        Path coverPath = extractCoverToTempFile(videoPath);
        if (coverPath == null) {
            return null;
        }
        try {
            Path imageDir = resolveUploadDir("post_img");
            Files.createDirectories(imageDir);
            String coverName = UUID.randomUUID() + ".jpg";
            Path dest = imageDir.resolve(coverName).toAbsolutePath().normalize();
            Files.move(coverPath, dest, StandardCopyOption.REPLACE_EXISTING);
            return "/post_img/" + coverName;
        } catch (Exception ignore) {
            try {
                Files.deleteIfExists(coverPath);
            } catch (IOException ignored) {
            }
            return null;
        }
    }

    /**
     * 截取封面到临时 JPG 文件；调用方负责删除。失败返回 null。
     */
    public Path extractCoverToTempFile(Path videoPath) {
        if (videoPath == null) {
            return null;
        }
        try {
            if (!Files.exists(videoPath) || Files.size(videoPath) <= 0) {
                return null;
            }
            Path coverPath = Files.createTempFile("post-cover-", ".jpg");
            Path ffmpegLog = Files.createTempFile("ffmpeg-cover-", ".log");

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegBin,
                    "-y",
                    "-ss", "0.1",
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
                } catch (Exception ignore) {
                }
                Files.deleteIfExists(coverPath);
                return null;
            }
            if (p.exitValue() != 0 || !Files.exists(coverPath) || Files.size(coverPath) <= 0) {
                Files.deleteIfExists(coverPath);
                return null;
            }
            return coverPath;
        } catch (Exception ignore) {
            return null;
        }
    }

    @Async("ffmpegExecutor")
    public void enqueueExtractFirstFrameCover(Long postId, String videoUrl) {
        if (postId == null) return;
        Path videoPath = resolveVideoPath(videoUrl);
        String coverRel = extractCoverToPostImgDir(videoPath);
        if (coverRel != null && !coverRel.isBlank()) {
            postMediaMapper.updateVideoCoverIfMissing(postId, coverRel);
        }
    }
}

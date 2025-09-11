package com.zhaoxinms.contract.tools.ocrcompare.compare;

import okhttp3.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.UUID;

public class GradioWorkflowClient {
    private final OkHttpClient http;
    private final String base;

    public GradioWorkflowClient(String baseUrl) {
        this.base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofMinutes(10))
                .writeTimeout(Duration.ofMinutes(10))
                .build();
    }

    public String uploadFile(Path file) throws IOException, InterruptedException {
        String uploadId = UUID.randomUUID().toString();
        // 优先使用 gradio 新版字段名 files（支持多文件）
        MultipartBody.Builder mbFiles = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("files", file.getFileName().toString(),
                        RequestBody.create(file.toFile(), MediaType.parse("application/pdf")));
        Request req = new Request.Builder().url(base + "/gradio_api/upload?upload_id=" + uploadId).post(mbFiles.build()).build();
        String uploadRespBody;
        try (Response resp = http.newCall(req).execute()) {
            uploadRespBody = resp.body()!=null?resp.body().string():"";
            if (!resp.isSuccessful()) throw new IOException("upload failed: " + resp.code() + ", body=" + uploadRespBody);
        }
        // poll progress
        for (int i=0;i<120;i++) { // up to ~60s
            Request p = new Request.Builder().url(base + "/gradio_api/upload_progress?upload_id=" + uploadId).get().build();
            try (Response r = http.newCall(p).execute()) {
                if (!r.isSuccessful()) throw new IOException("progress failed: " + r.code());
                String body = r.body()!=null?r.body().string():"";
                if (body.contains("\"success\":true") || body.contains("\"complete\":true")) break;
            }
            Thread.sleep(500);
        }
        // 从上传返回体中提取服务器保存路径：网站示例为 JSON 数组 ["/tmp/gradio/....pdf"]
        String serverPath = null;
        String s = uploadRespBody.trim();
        if (s.startsWith("[")) {
            int q1 = s.indexOf('"');
            int q2 = s.indexOf('"', q1+1);
            if (q1 >= 0 && q2 > q1) serverPath = s.substring(q1+1, q2);
        }
        if ((serverPath == null || serverPath.isEmpty())) {
            serverPath = extractServerPath(uploadRespBody);
        }
        if (serverPath == null || serverPath.isEmpty()) {
            // 再尝试用老字段名 file
            MultipartBody.Builder mbFile = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getFileName().toString(),
                            RequestBody.create(file.toFile(), MediaType.parse("application/pdf")));
            Request req2 = new Request.Builder().url(base + "/gradio_api/upload?upload_id=" + uploadId).post(mbFile.build()).build();
            try (Response resp2 = http.newCall(req2).execute()) {
                String body2 = resp2.body()!=null?resp2.body().string():"";
                if (resp2.isSuccessful()) {
                    String t = body2.trim();
                    if (t.startsWith("[")) {
                        int a = t.indexOf('"');
                        int b = t.indexOf('"', a+1);
                        if (a>=0 && b>a) serverPath = t.substring(a+1,b);
                    }
                    if (serverPath == null || serverPath.isEmpty()) serverPath = extractServerPath(body2);
                }
                if (serverPath == null || serverPath.isEmpty()) {
                    System.out.println("[Gradio] upload response (no path found): " + body2);
                    throw new IOException("upload succeed but no server path returned");
                }
            }
        }
        return serverPath;
    }

    public static class JoinInfo { public final String eventId; public final String sessionHash; public JoinInfo(String e,String s){eventId=e;sessionHash=s;} }

    public JoinInfo joinQueue(String serverPath, String promptMode, String vllmIp, int vllmPort, long minPixels, long maxPixels, boolean fitzPreprocess)
            throws IOException {
        // 与网站 payload 对齐
        String sessionHash = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String fileUrl = base + "/gradio_api/file=" + serverPath;
        String filePart = "{\"path\":\""+ escape(serverPath) +"\",\"url\":\""+ escape(fileUrl) +
                "\",\"orig_name\":\""+ escape(serverPath.substring(serverPath.lastIndexOf('/')+1)) +
                "\",\"size\":0,\"mime_type\":\"application/pdf\",\"meta\":{\"_type\":\"gradio.FileData\"}}";
        String dataArray = "[null,\"\"," + filePart + ",\""+ promptMode +"\",\""+ vllmIp +"\","+ vllmPort +","+ minPixels +","+ maxPixels +","+ (fitzPreprocess?"true":"false") +"]";
        String json = "{\"data\":" + dataArray + ",\"event_data\":null,\"fn_index\":5,\"trigger_id\":12,\"session_hash\":\""+ sessionHash +"\"}";
        Request req = new Request.Builder()
                .url(base + "/gradio_api/queue/join")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body()!=null?resp.body().string():"";
            if (!resp.isSuccessful()) throw new IOException("join failed: " + resp.code() + ", body=" + body);
            String eventId = extractTaskId(body);
            return new JoinInfo(eventId, sessionHash);
        }
    }

    public String pollResult(JoinInfo info) throws IOException, InterruptedException {
        for (int i=0;i<600;i++) { // up to ~5min
            Request req = new Request.Builder().url(base + "/gradio_api/queue/data?session_hash=" + info.sessionHash).get().build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) throw new IOException("queue data failed: " + resp.code());
                String body = resp.body()!=null?resp.body().string():"";
                if (body.contains("\"status\":\"COMPLETE\"") || body.contains("\"success\":true")) return body;
            }
            Thread.sleep(500);
        }
        throw new IOException("queue timeout");
    }

    private static String escape(String s){ return s.replace("\\","\\\\").replace("\"","\\\""); }

    private String extractTaskId(String body) {
        Pattern[] ps = new Pattern[] {
                Pattern.compile("\\\"event_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""),
                Pattern.compile("\\\"task_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""),
                Pattern.compile("\\\"hash\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        };
        for (Pattern p: ps) {
            Matcher m = p.matcher(body);
            if (m.find()) return m.group(1);
        }
        return body;
    }

    private String extractServerPath(String body) {
        // 常见格式：{"success":true,"files":[{"path":"/.../file.pdf"}]} 或 {"path":"/..."}
        Pattern[] ps = new Pattern[] {
                Pattern.compile("\\\"path\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""),
                Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        };
        for (Pattern p: ps) {
            Matcher m = p.matcher(body);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    public String findFirstZipUrl(String body) {
        Matcher m = Pattern.compile("(https?://[^\\\"\\s]+\\.zip)").matcher(body);
        return m.find()? m.group(1): null;
    }

    public List<String> findJsonUrls(String body) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("(https?://[^\\\"\\s]+\\.json)").matcher(body);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    public Path downloadTo(Path targetDir, String url) throws IOException {
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new IOException("download failed: " + resp.code());
            String fileName = url.substring(url.lastIndexOf('/')+1);
            Path out = targetDir.resolve(fileName);
            byte[] bytes = resp.body()!=null?resp.body().bytes(): new byte[0];
            Files.createDirectories(targetDir);
            Files.write(out, bytes);
            return out;
        }
    }

    public void extractZip(Path zipFile, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Files.createDirectories(outDir.resolve(entry.getName()));
                } else {
                    Path out = outDir.resolve(entry.getName());
                    Files.createDirectories(out.getParent());
                    try (java.io.OutputStream os = Files.newOutputStream(out)) {
                        int len;
                        while ((len = zis.read(buf)) > 0) os.write(buf, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }
}



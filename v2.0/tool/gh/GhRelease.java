import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.net.ssl.*;

/**
 * GitHub Release 小工具（用 JDK 自带 HTTPS，绕开本机 schannel 的 TLS 问题）
 *
 * 用法：
 *   java GhRelease.java check                                  # 免鉴权查最新 Release
 *   java GhRelease.java create <tag> <name> <bodyFile>          # 需要环境变量 GH_TOKEN
 *   java GhRelease.java upload <releaseId> <file>               # 需要 GH_TOKEN
 *
 * 环境变量（可选）：GH_PROXY=http://127.0.0.1:7890 ；GH_TOKEN=github_pat_xxx
 */
public class GhRelease {

    private static final String OWNER = "Noah-gif123";
    private static final String REPO = "academic_risk_warning";
    private static final String API = "https://api.github.com";

    public static void main(String[] args) throws Exception {
        String proxy = System.getenv("GH_PROXY");
        if (proxy != null && !proxy.isBlank()) {
            URI u = URI.create(proxy);
            System.setProperty("https.proxyHost", u.getHost());
            System.setProperty("https.proxyPort", String.valueOf(u.getPort() > 0 ? u.getPort() : 7890));
            System.out.println("[proxy] " + u.getHost() + ":" + u.getPort());
        }
        String cmd = args.length > 0 ? args[0] : "check";
        switch (cmd) {
            case "check" -> check();
            case "create" -> create(args[1], args[2], args[3]);
            case "upload" -> upload(args[1], args[2]);
            default -> System.out.println("unknown command: " + cmd);
        }
    }

    private static void check() throws Exception {
        String body = request("GET", API + "/repos/" + OWNER + "/" + REPO + "/releases", null, false);
        System.out.println("HTTP OK, body length=" + body.length());
        int i = body.indexOf("\"tag_name\":\"");
        if (i >= 0) {
            System.out.println("latest release tag = " + body.substring(i + 12, body.indexOf('"', i + 12)));
        }
        System.out.println(body.substring(0, Math.min(300, body.length())));
    }

    private static void create(String tag, String name, String bodyFile) throws Exception {
        String body = Files.readString(Paths.get(bodyFile), StandardCharsets.UTF_8);
        String json = "{\"tag_name\":\"" + esc(tag) + "\",\"name\":\"" + esc(name) + "\","
                + "\"body\":\"" + esc(body) + "\",\"draft\":false,\"prerelease\":false}";
        String resp = request("POST", API + "/repos/" + OWNER + "/" + REPO + "/releases", json, true);
        System.out.println(resp.substring(0, Math.min(600, resp.length())));
        int i = resp.indexOf("\"id\":");
        if (i >= 0) {
            int j = i + 5;
            int k = j;
            while (k < resp.length() && Character.isDigit(resp.charAt(k))) k++;
            System.out.println("RELEASE_ID=" + resp.substring(j, k));
        }
        int h = resp.indexOf("\"html_url\":\"");
        if (h >= 0) {
            System.out.println("HTML_URL=" + resp.substring(h + 12, resp.indexOf('"', h + 12)));
        }
    }

    private static void upload(String releaseId, String file) throws Exception {
        Path p = Paths.get(file);
        byte[] data = Files.readAllBytes(p);
        String name = p.getFileName().toString();
        String url = "https://uploads.github.com/repos/" + OWNER + "/" + REPO
                + "/releases/" + releaseId + "/assets?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        HttpURLConnection c = open(url, "POST", true);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(data.length);
        try (OutputStream os = c.getOutputStream()) { os.write(data); }
        String resp = read(c);
        System.out.println("upload HTTP " + c.getResponseCode() + " name=" + name + " bytes=" + data.length);
        System.out.println(resp.substring(0, Math.min(300, resp.length())));
    }

    // ---------------- 内部 ----------------
    private static String request(String method, String url, String body, boolean auth) throws Exception {
        HttpURLConnection c = open(url, method, auth);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream os = c.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        }
        return read(c);
    }

    private static HttpURLConnection open(String url, String method, boolean auth) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(45000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "academic-risk-warning-release-tool");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        if (auth) {
            String token = System.getenv("GH_TOKEN");
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("缺少环境变量 GH_TOKEN（需要 contents:write 权限）");
            }
            c.setRequestProperty("Authorization", "Bearer " + token.trim());
        }
        return c;
    }

    private static String read(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) {
            System.out.println("HTTP " + code + " -> " + text.substring(0, Math.min(500, text.length())));
            System.exit(2);
        }
        return text;
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
                }
            }
        }
        return sb.toString();
    }
}

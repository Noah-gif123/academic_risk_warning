import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** 用 Java 的 TLS 栈下载 jar（PowerShell 的 schannel 在这台机器上握手失败）。 */
public class FetchJUnit {

        private static final List<String> COORDS = List.of(
            "org/junit/jupiter/junit-jupiter-api/6.0.3/junit-jupiter-api-6.0.3.jar",
            "org/junit/jupiter/junit-jupiter-engine/6.0.3/junit-jupiter-engine-6.0.3.jar",
            "org/junit/platform/junit-platform-commons/6.0.3/junit-platform-commons-6.0.3.jar",
            "org/junit/platform/junit-platform-engine/6.0.3/junit-platform-engine-6.0.3.jar",
            "org/junit/platform/junit-platform-launcher/6.0.3/junit-platform-launcher-6.0.3.jar",
            "org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar",
            "org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar");

    private static final List<String> BASES = List.of(
            "https://maven.aliyun.com/repository/public/",
            "https://repo1.maven.org/maven2/");

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : ".");
        Files.createDirectories(out);
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        for (String coord : COORDS) {
            String file = coord.substring(coord.lastIndexOf('/') + 1);
            Path target = out.resolve(file);
            if (Files.exists(target) && Files.size(target) > 1024) {
                System.out.println("SKIP " + file + " (" + Files.size(target) + " bytes)");
                continue;
            }
            boolean ok = false;
            for (String base : BASES) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(base + coord))
                            .timeout(Duration.ofSeconds(60))
                            .GET().build();
                    HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (res.statusCode() == 200 && res.body().length > 1024) {
                        Files.write(target, res.body());
                        System.out.println("OK   " + file + " " + res.body().length + " bytes  <- " + base);
                        ok = true;
                        break;
                    }
                    System.out.println("HTTP " + res.statusCode() + " " + base + coord);
                } catch (Exception e) {
                    System.out.println("FAIL " + base + coord + " : " + e);
                }
            }
            if (!ok) {
                System.out.println("GIVEUP " + file);
            }
        }
    }
}

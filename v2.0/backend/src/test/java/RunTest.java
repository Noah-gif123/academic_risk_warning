import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** 不依赖 Gradle/Maven，直接用 JUnit Platform 跑单测（本机 gradlew 被沙箱挡住时的替代跑法）。 */
public class RunTest {

    public static void main(String[] args) throws Exception {
        String className = args.length > 0 ? args[0] : "com.example.academic_risk_warning.service.ai.TriggerGateTest";
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(Class.forName(className)))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);

        TestExecutionSummary summary = listener.getSummary();
        PrintWriter out = new PrintWriter(System.out, true);
        summary.printTo(out);
        if (summary.getTotalFailureCount() > 0) {
            summary.printFailuresTo(out);
            System.exit(1);
        }
        System.out.println("PASSED " + summary.getTestsSucceededCount() + " / " + summary.getTestsFoundCount());
    }
}

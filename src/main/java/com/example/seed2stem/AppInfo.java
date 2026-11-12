package com.example.seed2stem;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The app version from pom.xml, for the top of every screen (templates use {@code @appInfo.version}). */
@Component("appInfo")
public class AppInfo {

    private final String version;

    public AppInfo(@Value("${app.version:}") String version) {
        this.version = clean(version, AppInfo.class.getPackage().getImplementationVersion());
    }

    /**
     * The configured version, or the jar's version when the property wasn't filled in
     * (e.g. running straight from an IDE leaves the raw {@code @project.version@}).
     */
    static String clean(String configured, String fromJar) {
        if (configured != null && !configured.isBlank() && !configured.contains("@")) return configured.trim();
        if (fromJar != null && !fromJar.isBlank()) return fromJar.trim();
        return "dev";
    }

    /** "1.0.0" */
    public String getVersion() { return version; }

    /** "v1.0.0" */
    public String getLabel() { return "dev".equals(version) ? "dev" : "v" + version; }
}

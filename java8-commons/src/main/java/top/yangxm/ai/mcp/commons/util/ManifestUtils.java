package top.yangxm.ai.mcp.commons.util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class ManifestUtils {
    private ManifestUtils() {
    }

    public static Optional<String> getManifestField(Class<?> clazz, String fieldName) {
        try {
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return Optional.empty();
            }
            URL location = codeSource.getLocation();
            File file = new File(location.toURI());

            if (file.isFile() && file.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(file)) {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        Attributes attrs = manifest.getMainAttributes();
                        return Optional.ofNullable(attrs.getValue(fieldName));
                    }
                }
            } else {
                URL manifestUrl = new URL(location + "META-INF/MANIFEST.MF");
                try (InputStream in = manifestUrl.openStream()) {
                    Manifest manifest = new Manifest(in);
                    Attributes attrs = manifest.getMainAttributes();
                    return Optional.ofNullable(attrs.getValue(fieldName));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to read MANIFEST.MF for class " + clazz.getName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }
}

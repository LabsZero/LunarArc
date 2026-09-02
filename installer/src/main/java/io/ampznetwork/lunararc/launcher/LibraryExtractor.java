package io.ampznetwork.lunararc.launcher;

import java.io.File;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Unpacks the libraries embedded in the shipped jar under {@code META-INF/libraries/}.
 *
 * <p>Read through {@link JarFile} rather than a {@code ZipInputStream}. Both find the same entries,
 * but a stream has to walk the archive from the front to do it: on a jar carrying every runtime
 * dependency that means reading tens of megabytes on every single start, nearly all of it entries
 * this is not interested in. A zip's central directory lists what is inside and where, so opening
 * it as a JarFile turns that scan into a lookup, and only the library entries are read at all.</p>
 *
 * <p>The already-extracted check is also better for it. Entry sizes come from the central directory
 * and are always known, where a streamed entry can report -1 and force a copy that was not needed.
 * After the first start, when every library is present at the right size, this now reads no entry
 * data whatsoever.</p>
 */
public class LibraryExtractor {

    private static final String PREFIX = "META-INF/libraries/";

    public static void extractLibraries() {
        try {
            File selfJar = new File(LibraryExtractor.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            int extracted = 0;
            try (JarFile jar = new JarFile(selfJar)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(PREFIX)) continue;

                    Path dest = Paths.get(name.substring("META-INF/".length()));
                    if (entry.isDirectory()) {
                        Files.createDirectories(dest);
                        continue;
                    }

                    long expectedSize = entry.getSize();
                    if (Files.isRegularFile(dest) && expectedSize >= 0 && Files.size(dest) == expectedSize) {
                        continue;
                    }

                    Files.createDirectories(dest.getParent());
                    Path temp = dest.resolveSibling(dest.getFileName() + ".tmp");
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    try {
                        Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException ignored) {
                        Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    extracted++;
                }
            }

            // Only worth saying when something actually happened. On every start after the first
            // this does no work, and a step that reports itself for doing nothing is how a startup
            // log stops being read.
            if (extracted > 0) {
                ConsoleUI.printSuccess("Extracted " + extracted + " runtime libraries.");
            }
        } catch (Exception e) {
            ConsoleUI.printError("Failed to extract libraries: " + e.getMessage());
        }
    }
}

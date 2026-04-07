package forge.util;

import java.io.File;
import java.util.Arrays;

public final class DirectoryLocator {
    private DirectoryLocator() {
    }

    public static String locateDirectoryFrom(final String startingPath, final String directoryName,
                                             final int maximumLevelsUp, final String... requiredRelativePaths) {
        final File start = new File(startingPath).getAbsoluteFile();

        File current = start;
        for (int level = 0; level <= maximumLevelsUp && current != null; level++) {
            final File currentCandidate = current;
            logCandidate("current", currentCandidate, requiredRelativePaths);
            if (isValidCandidate(currentCandidate, requiredRelativePaths)) {
                return appendSeparator(currentCandidate.getPath());
            }

            final File namedCandidate = new File(current, directoryName).getAbsoluteFile();
            logCandidate(directoryName, namedCandidate, requiredRelativePaths);
            if (isValidCandidate(namedCandidate, requiredRelativePaths)) {
                return appendSeparator(namedCandidate.getPath());
            }

            current = current.getParentFile();
        }

        final String message = "Forge startup error: required assets were not found. startingPath=" + start.getPath()
                + ", directoryName=" + directoryName
                + ", requiredRelativePaths=" + Arrays.toString(requiredRelativePaths)
                + ", maximumLevelsUp=" + maximumLevelsUp
                + ". Try running Forge from the repo root, from the forge-gui folder, or from a child folder under the repo root.";
        System.err.println("DirectoryLocator: unable to locate required directory.");
        System.err.println(message);
        throw new RuntimeException(message);
    }

    public static String locateForgeAssetsDir() {
        return locateDirectoryFrom(
                ".",
                "forge-gui",
                2,
                "res" + File.separator + "languages" + File.separator + "en-US.properties",
                "res" + File.separator + "cardsfolder",
                "res" + File.separator + "editions"
        );
    }

    private static boolean isValidCandidate(final File candidate, final String... requiredRelativePaths) {
        for (final String requiredRelativePath : requiredRelativePaths) {
            final File requiredPath = new File(candidate, requiredRelativePath);
            if (!requiredPath.exists()) {
                return false;
            }
        }
        return true;
    }

    private static void logCandidate(final String label, final File candidate, final String... requiredRelativePaths) {
        System.err.println("DirectoryLocator: checking " + label + " candidate " + candidate.getPath()
                + " for " + Arrays.toString(requiredRelativePaths));
    }

    private static String appendSeparator(final String path) {
        if (path.endsWith(File.separator)) {
            return path;
        }
        return path + File.separator;
    }
}

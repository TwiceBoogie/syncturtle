package com.syncturtle.services.instance.version;

import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.syncturtle.services.instance.type.VersionStatus;

public class ReleaseVersion implements Comparable<ReleaseVersion> {

    private static final Pattern VERSION = Pattern.compile(
            "^[vV]?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");
    private static final Pattern NUMERIC = Pattern.compile("[0-9]+");

    private final BigInteger major;
    private final BigInteger minor;
    private final BigInteger patch;
    private final String prerelease;

    private ReleaseVersion(Matcher matcher) {
        major = new BigInteger(matcher.group(1));
        minor = new BigInteger(matcher.group(2));
        patch = new BigInteger(matcher.group(3));
        prerelease = matcher.group(4);
    }

    public static Optional<ReleaseVersion> parse(String value) {
        if (value == null || value.length() > 50) {
            return Optional.empty();
        }

        Matcher matcher = VERSION.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String prerelease = matcher.group(4);
        if (prerelease != null) {
            String[] identifiers = prerelease.split("\\.");
            for (String identifier : identifiers) {
                if (NUMERIC.matcher(identifier).matches() && identifier.length() > 1 && identifier.charAt(0) == '0') {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(new ReleaseVersion(matcher));
    }

    public static VersionStatus status(String current, String latest) {
        Optional<ReleaseVersion> currentVersion = parse(current);
        Optional<ReleaseVersion> latestVersion = parse(latest);

        if (currentVersion.isEmpty() || latestVersion.isEmpty()) {
            return VersionStatus.UNKNOWN;
        }

        int comparison = currentVersion.get().compareTo(latestVersion.get());
        if (comparison < 0) {
            return VersionStatus.UPDATE_AVAILABLE;
        }

        if (comparison > 0) {
            return VersionStatus.AHEAD_OF_RELEASE;
        }

        return VersionStatus.CURRENT;
    }

    @Override
    public int compareTo(ReleaseVersion other) {
        int result = major.compareTo(other.major);
        if (result != 0) {
            return result;
        }

        result = minor.compareTo(other.minor);
        if (result != 0) {
            return result;
        }

        result = patch.compareTo(other.patch);
        if (result != 0) {
            return result;
        }

        if (prerelease == null && other.prerelease == null) {
            return 0;
        }

        if (prerelease == null) {
            return 1;
        }

        if (other.prerelease == null) {
            return -1;
        }

        String[] left = prerelease.split("\\.");
        String[] right = other.prerelease.split("\\.");
        int shared = Math.min(left.length, right.length);

        for (int index = 0; index < shared; index++) {
            result = compareIdentifier(left[index], right[index]);
            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(left.length, right.length);
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = NUMERIC.matcher(left).matches();
        boolean rightNumeric = NUMERIC.matcher(right).matches();
        if (leftNumeric && rightNumeric) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumeric) {
            return -1;
        }
        if (rightNumeric) {
            return 1;
        }
        return left.compareTo(right);
    }

}

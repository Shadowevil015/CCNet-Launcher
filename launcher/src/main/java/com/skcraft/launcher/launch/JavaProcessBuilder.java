/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.launch;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tool to build the entire command line used to launch a Java process.
 * It combines flags, memory settings, arguments, the class path, and
 * the main class.
 */
@ToString
public class JavaProcessBuilder {

    private static final Pattern argsPattern = Pattern.compile("(?:([^\"]\\S*)|\"(.+?)\")\\s*");

    @Getter @Setter private JavaRuntime runtime;
    @Getter @Setter private int minMemory;
    @Getter @Setter private int maxMemory;
    @Getter @Setter private int permGen;

    @Getter private final List<File> classPath = new ArrayList<File>();
    @Getter private final List<String> flags = new ArrayList<String>();
    @Getter private final List<String> args = new ArrayList<String>();
    @Getter @Setter private String mainClass;

    private File getJavaBinPath() throws IOException {
        File path = runtime.getDir().getAbsoluteFile();

        // Try the parent directory
        if (!path.exists()) {
            throw new IOException(
                    "The configured Java runtime path '" + path + "' doesn't exist.");
        } else if (path.isFile()) {
            path = path.getParentFile();
        }

        File binDir = new File(path, "bin");
        if (binDir.isDirectory()) {
            path = binDir;
        }

        return path;
    }

    public JavaProcessBuilder classPath(File file) {
        getClassPath().add(file);
        return this;
    }

    public JavaProcessBuilder classPath(String path) {
        getClassPath().add(new File(path));
        return this;
    }

    public String buildClassPath() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (File file : classPath) {
            if (first) {
                first = false;
            } else {
                builder.append(File.pathSeparator);
            }

            builder.append(file.getAbsolutePath());
        }

        return builder.toString();
    }

    public List<String> buildCommand() throws IOException {
        List<String> command = new ArrayList<String>();

        if (getRuntime() != null) {
            File javaBinary = new File(getJavaBinPath(), "java");
            command.add(javaBinary.getAbsolutePath());
        } else {
            command.add("java");
        }

        command.addAll(flags);

        if (minMemory > 0) {
            command.add("-Xms" + minMemory + "M");
        }

        if (maxMemory > 0) {
            command.add("-Xmx" + maxMemory + "M");
        }

        if (permGen > 0) {
            // If we know the Java version, only add permsize for 7 or older
            if (getRuntime() == null || getRuntime().getMajorVersion() < 8) {
                command.add("-XX:MaxPermSize=" + permGen + "M");
            }
        }

        command.add(mainClass);
        command.addAll(args);

        return command;
    }

    /**
     * Split the given string as simple command line arguments.
     *
     * <p>This is not to be used for security purposes.</p>
     *
     * @param str the string
     * @return the split args
     */
    public static List<String> splitArgs(String str) {
        Matcher matcher = argsPattern.matcher(str);
        List<String> parts = new ArrayList<String>();
        while (matcher.find()) {
            parts.add(matcher.group(1));
        }
        return parts;
    }

}

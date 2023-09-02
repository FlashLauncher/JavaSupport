package Utils;

import UIL.base.IImage;

import java.io.*;
import java.util.Scanner;

public class Java {
    public final IImage icon;
    public final File file;
    public final String name, version;

    public Java(final IImage icon, final File file) throws IOException, InterruptedException {
        this.file = file;
        this.icon = icon;
        final Process p = Runtime.getRuntime().exec(new String[] { file.getAbsolutePath(), "-version" });
        p.getOutputStream().close();
        if (p.waitFor() != 0)
            throw new IOException("Exit code: " + p.exitValue());
        try (final Scanner r = new Scanner(p.getErrorStream(), "UTF-8")) {
            if (r.hasNextLine()) {
                String l = r.nextLine();
                int i = l.indexOf(" version \"");
                if (i != -1) {
                    name = l.substring(0, i);
                    l = l.substring(i + 10);
                    i = l.indexOf("\"");
                    if (i != -1) {
                        version = l.substring(0, i);
                        return;
                    }
                }
            }
        }
        throw new IOException("No information");
    }

    @Override public String toString() { return name + " " + version; }

    public Java(final JavaSupport javaSupport, final File file) throws IOException, InterruptedException {
        this.file = file;
        final Process p = Runtime.getRuntime().exec(new String[] { file.getAbsolutePath(), "-version" });
        p.getOutputStream().close();
        if (p.waitFor() != 0)
            throw new IOException("Exit code: " + p.exitValue());
        try (final Scanner r = new Scanner(p.getErrorStream(), "UTF-8")) {
            if (r.hasNextLine()) {
                String l = r.nextLine();
                int i = l.indexOf(" version \"");
                if (i != -1) {
                    final String n = l.substring(0, i);
                    switch (n) {
                        case "java":
                            name = "JRE";
                            break;
                        case "openjdk":
                            name = "OpenJDK";
                            break;
                        default:
                            name = n;
                            break;
                    }
                    l = l.substring(i + 10);
                    i = l.indexOf("\"");
                    if (i != -1) {
                        version = l.substring(0, i);
                        icon = javaSupport.javaIcon;
                        return;
                    }
                }
            }
        }
        throw new IOException("No information");
    }
}
package Utils;

import Launcher.*;
import UIL.*;
import UIL.base.*;
import Utils.json.Json;
import Utils.json.JsonElement;
import Utils.json.JsonList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JavaSupport extends Plugin {
    public static final String ID = "java-support", JAVA_RUN = Core.IS_WINDOWS ? "java.exe" : "java";
    public static final IImage JAVA_ICON;

    static {
        IImage ji = null;
        try {
            ji = UI.image(ID + "://images/java.png");
        } catch (final IOException|InterruptedException ex) {
            ex.printStackTrace();
        }
        JAVA_ICON = ji;
    }

    private final File config;

    public final ConcurrentLinkedQueue<String> hiddenJavaList = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Java> javaList = new ConcurrentLinkedQueue<>();

    public JavaSupport(final PluginContext context) throws IOException, InterruptedException {
        super(context);
        config = new File(context.getPluginData(), "config.json");
        try {
            if (config.exists() && config.isFile()) {
                final JsonList l;
                try (final FileInputStream is = new FileInputStream(config)) {
                    l = Json.parse(is, false, StandardCharsets.UTF_8).getAsList();
                }
                for (final JsonElement e : l) {
                    final File f = new File(e.getAsString());
                    if (f.exists())
                        javaList.add(new Java(f.getCanonicalFile()));
                    else
                        hiddenJavaList.add(e.getAsString());
                }
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        addMenuItem(new FLMenuItemListener("java", context.getIcon(), null) {
            @Override
            public void onOpen(final FLMenuItemEvent event) {
                new Object() {
                    private final IScrollPane sp = UI.scrollPane();
                    private final ContainerListBuilder clb = new ContainerListBuilder(sp.content(UI.panel().borderRadius(UI.ZERO)), 150, 8);
                    private Runnable u;

                    {
                        final IContainer tp = UI.panel();
                        final IImageView iv = UI.imageView(ImagePosMode.CENTER, ImageSizeMode.SCALE).image(event.icon).size(32, 32).pos(8, 8);
                        final IText t = UI.text("Javas").ha(HAlign.LEFT).size(event.width() - 56, 32).pos(48, 8);
                        event.add(tp.size(event.width(), 48).add(iv, t), clb.size(event.width(), event.height() - 56).pos(0, 56));
                        Core.onNotifyLoop(javaList, u = () -> {
                            clb.clear();
                            for (final Java j : javaList)
                                clb.add(UI.button(j, j.icon).imageAlign(ImgAlign.TOP).imageOffset(16).onAction((s, e) -> {
                                    Core.offNotifyLoop(u);
                                    final IButton del = UI.button(Lang.get("launcher.remove")).size(event.width() - 16, 32).pos(8, 48);
                                    final IContainer c = UI.panel().size(event.width(), 88).pos(0, 56)
                                            .add(UI.text(j.file).ha(HAlign.LEFT).size(del.width(), 32).pos(8, 8), del);
                                    final IButton back = UI.button("<").size(32, 32).pos(8, 8);

                                    t.text("Javas > " + j);
                                    tp.remove(iv).add(back);
                                    event.remove(clb).add(c).update();

                                    final Runnable ou = u, close = () -> {
                                        t.text("Javas");
                                        tp.remove(back).add(iv).update();
                                        event.remove(c).add(clb);
                                        Core.offNotifyLoop(u);
                                        Core.onNotifyLoop(javaList, u = ou);
                                    };
                                    Core.onNotifyLoop(javaList, u = () -> {
                                        if (!javaList.contains(j))
                                            close.run();
                                    });

                                    back.onAction((i1, i2) -> close.run());
                                    del.onAction((s1, e1) -> {
                                        javaList.remove(j);
                                        synchronized (javaList) {
                                            javaList.notifyAll();
                                        }
                                        synchronized (config) {
                                            if (config.exists())
                                                config.delete();
                                            else if (!context.getPluginData().exists())
                                                context.getPluginData().mkdirs();
                                            final JsonList l = new JsonList();
                                            for (final Java j1 : javaList)
                                                l.add(new JsonElement(FS.relative(Core.getPath(FLCore.class), j1.file)));
                                            for (final String j1 : hiddenJavaList)
                                                l.add(new JsonElement(j1));
                                            try (final FileOutputStream fos = new FileOutputStream(config)) {
                                                fos.write(l.toString().getBytes(StandardCharsets.UTF_8));
                                            } catch (final IOException ignored) {}
                                        }
                                    });
                                }));
                            clb.add(UI.button(Lang.get("launcher.add"), FLCore.ICON_ADD).imageAlign(ImgAlign.TOP).imageOffset(24).onAction((s, e) -> new FLFSChooser(event.launcher, "Java") {{
                                filters.add(f -> f.isDirectory() || f.getName().equals(JAVA_RUN));
                                allowMultipleSelection = false;
                                if (start()) {
                                    File f = null;
                                    for (final File ff : getSelected())
                                        if (ff.isFile()) {
                                            f = ff;
                                            break;
                                        }
                                    try {
                                        javaList.add(new Java(f));
                                        synchronized (javaList) {
                                            javaList.notifyAll();
                                        }
                                        synchronized (config) {
                                            if (config.exists())
                                                config.delete();
                                            else if (!context.getPluginData().exists())
                                                context.getPluginData().mkdirs();
                                            final JsonList l = new JsonList();
                                            for (final Java j : javaList)
                                                l.add(new JsonElement(FS.relative(Core.getPath(FLCore.class), j.file)));
                                            for (final String j : hiddenJavaList)
                                                l.add(new JsonElement(j));
                                            try (final FileOutputStream fos = new FileOutputStream(config)) {
                                                fos.write(l.toString().getBytes(StandardCharsets.UTF_8));
                                            } catch (final IOException ignored) {}
                                        }
                                    } catch (final IOException|InterruptedException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                dispose();
                            }})).update();
                        });
                        event.onClose(() -> Core.offNotifyLoop(u));
                    }
                };
            }
        });
        try {
            if (javaList.isEmpty()) {
                javaList.add(new Java(new File(System.getProperty("java.home") + (Core.IS_WINDOWS ? "/bin/java.exe" : "/bin/java"))));
                synchronized (config) {
                    if (config.exists())
                        config.delete();
                    else if (!context.getPluginData().exists())
                        context.getPluginData().mkdirs();
                    final JsonList l = new JsonList();
                    for (final Java j1 : javaList)
                        l.add(new JsonElement(FS.relative(Core.getPath(FLCore.class), j1.file)));
                    for (final String j1 : hiddenJavaList)
                        l.add(new JsonElement(j1));
                    try (final FileOutputStream fos = new FileOutputStream(config)) {
                        fos.write(l.toString().getBytes(StandardCharsets.UTF_8));
                    } catch (final IOException ignored) {}
                }
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            ex.fillInStackTrace();
            ex.printStackTrace();
        }
    }
}

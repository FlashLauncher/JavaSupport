package Utils;

import Launcher.*;
import UIL.*;
import UIL.base.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JavaSupport extends Plugin {
    public final String JAVA_RUN = Core.IS_WINDOWS ? "java.exe" : "java";

    public final IImage
            javaIcon
    ;

    public final ConcurrentLinkedQueue<Java> javaList = new ConcurrentLinkedQueue<>();

    public JavaSupport(final PluginContext context) throws IOException, InterruptedException {
        super(context);
        javaIcon = UI.image("java-support://images/java.png");
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
                                        javaList.add(new Java(JavaSupport.this, f));
                                        synchronized (javaList) {
                                            javaList.notifyAll();
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
            System.out.println(System.getProperty("java.home"));
            javaList.add(new Java(this, new File(System.getProperty("java.home") + (Core.IS_WINDOWS ? "/bin/java.exe" : "/bin/java"))) {{
                System.out.println(this);
            }});
        } catch (final Exception ex) {
            ex.printStackTrace();
            ex.fillInStackTrace();
            ex.printStackTrace();
        }
    }
}

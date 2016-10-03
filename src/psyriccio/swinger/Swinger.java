/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package psyriccio.swinger;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 *
 * @author psyriccio
 */
public class Swinger {

    private static final List<Consumer<Throwable>> GEC = new ArrayList<>();

    public static void registerGlobalErrorConsumer(Consumer<Throwable> cons) {
        synchronized (GEC) {
            GEC.add(cons);
        }
    }

    public static void unregisterGlobalErrorConsumer(Consumer<Throwable> cons) {
        synchronized (GEC) {
            GEC.remove(cons);

        }
    }

    public static void invokeSafe(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable thr) {
            synchronized (GEC) {
                GEC.forEach((cons) -> cons.accept(thr));
            }
        }
    }

    public static Runnable makeRunnableSafe(final Runnable runnable) {
        return () -> {
            invokeSafe(runnable);
        };

    }

    public static void doInUIThreadAndWaitSafe(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Throwable ex) {
                synchronized (GEC) {
                    GEC.forEach((cons) -> cons.accept(ex));
                }
            }
        }
    }

    public static void doInUIThreadAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeAndWait(runnable);
        }
    }

    public static void doInUIThreadLaterSafe(Runnable runnable) {
        SwingUtilities.invokeLater(makeRunnableSafe(runnable));
    }

    public static void doInUIThreadLater(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    public static void doWithEntireComponentTree(Component component, Consumer<Component> consumer) {
        consumer.accept(component);
        if (component instanceof JComponent) {
            JComponent jcomp = (JComponent) component;
            for (Component child : jcomp.getComponents()) {
                doWithEntireComponentTree(child, consumer);
            }
        }
    }

    public static void doIfJComponent(Component component, Consumer<JComponent> consumer) {
        if (component instanceof JComponent) {
            try {
                JComponent jcom = (JComponent) component;
                if (jcom != null) {
                    consumer.accept(jcom);
                }
            } catch (Throwable ex) {
                // empty
            }
        }
    }

    public static void loadFontAsMainSafe(File fontFile, JComponent... components) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font newFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            ge.registerFont(newFont);
            for (Component comp : components) {
                doWithEntireComponentTree(comp, (cmp) -> {
                    cmp.setFont(newFont);
                    doIfJComponent(cmp, (jcmp) -> jcmp.updateUI());
                });
            }
        } catch (IOException | FontFormatException e) {
            synchronized (GEC) {
                GEC.forEach((cons) -> cons.accept(e));
            }
        }
    }

    public static void loadFontAsMain(File fontFile, Component... components) throws FontFormatException, IOException {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font newFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
        ge.registerFont(newFont);
        for (Component comp : components) {
            doWithEntireComponentTree(comp, (cmp) -> {
                cmp.setFont(newFont);
                doIfJComponent(cmp, (jcmp) -> jcmp.updateUI());
            });
        }
    }

    public static void loadFontAsMainSafe(InputStream fontInputStream, Component... components) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font newFont = Font.createFont(Font.TRUETYPE_FONT, fontInputStream);
            ge.registerFont(newFont);
            for (Component comp : components) {
                doWithEntireComponentTree(comp, (cmp) -> {
                    cmp.setFont(newFont);
                    doIfJComponent(cmp, (jcmp) -> jcmp.updateUI());
                });
            }
        } catch (IOException | FontFormatException e) {
            synchronized (GEC) {
                GEC.forEach((cons) -> cons.accept(e));
            }
        }
    }

    public static void loadFontAsMain(InputStream fontInputStream, Component... components) throws FontFormatException, IOException {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font newFont = Font.createFont(Font.TRUETYPE_FONT, fontInputStream);
        ge.registerFont(newFont);
        for (Component comp : components) {
            doWithEntireComponentTree(comp, (cmp) -> {
                cmp.setFont(newFont);
                doIfJComponent(cmp, (jcmp) -> jcmp.updateUI());
            });
        }
    }

    public static void loadFontAsMainSafe(URL fontURL, Component... components) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font newFont = Font.createFont(Font.TRUETYPE_FONT, fontURL.openStream());
            ge.registerFont(newFont);
            for (Component comp : components) {
                doWithEntireComponentTree(comp, (cmp) -> {
                    cmp.setFont(newFont);
                    doIfJComponent(cmp, (jcmp) -> jcmp.updateUI());
                });
            }
        } catch (IOException | FontFormatException e) {
            synchronized (GEC) {
                GEC.forEach((cons) -> cons.accept(e));
            }
        }
    }

    public static void loadFontAsMain(URL fontURL, Component... components) throws FontFormatException, IOException {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font newFont = Font.createFont(Font.TRUETYPE_FONT, fontURL.openStream());
        ge.registerFont(newFont);
        for (Component comp : components) {
            doWithEntireComponentTree(comp, (cmp) -> {
                cmp.setFont(newFont);
                doIfJComponent(cmp, (jcmp) -> jcmp.updateUI());
            });
        }
    }

}

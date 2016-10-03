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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Swing UI tools and utilities
 *
 * @author psyriccio
 */
public class Swinger {

    /**
     * Threading subsystem
     */
    public static class ThreadPool {

        /**
         * Callable with progress-notification consumer
         */
        public static class CallableWithCallback<P, T> implements Callable<T> {

            private final Consumer<P> progressConsumer;
            private final Function<Consumer<P>, T> work;

            public CallableWithCallback(Function<Consumer<P>, T> work, Consumer<P> progressConsumer) {
                this.progressConsumer = progressConsumer;
                this.work = work;
            }

            @Override
            public T call() throws Exception {
                return work.apply(progressConsumer);
            }

        }

        /**
         * Threading pool work modes
         */
        public enum Mode {
            Single, SingleScheduled, Fixed, Scheduled, Cached, WorkStealing
        }

        private static ExecutorService executorService;
        private static boolean scheduled;

        private static ScheduledExecutorService getServiceAsScheduled() {
            return isScheduled() ? (ScheduledExecutorService) executorService : null;
        }

        /**
         * Returns true, if thread pool in scheduled mode
         */
        public static boolean isScheduled() {
            return scheduled;
        }

        /**
         * Initializes thread pool with mode-specified type
         *
         * @param mode Thread pool work mode
         */
        public static void init(Mode mode) {
            switch (mode) {
                case Single:
                    executorService = Executors.newSingleThreadExecutor();
                    scheduled = false;
                    break;
                case SingleScheduled:
                    executorService = Executors.newSingleThreadScheduledExecutor();
                    scheduled = true;
                    break;
                case Fixed:
                    executorService = Executors.newSingleThreadExecutor();
                    scheduled = false;
                    break;
                case Scheduled:
                    throw new RuntimeException("nThreads parameter required for scheduled thread pool mode");
                case Cached:
                    executorService = Executors.newCachedThreadPool();
                    scheduled = false;
                    break;
                case WorkStealing:
                    throw new RuntimeException("parallelismLvl parameter required for work-stealing thread pool mode");
            }
        }

        /**
         * Initializes thread pool with mode-specified type and number of thread
         * or parallelism level
         *
         * @param mode Thread pool work mode
         * @param nThreadsOrParallelismLvl number of thread for scheduled mode
         * or parallelism level for work-stealing mode
         */
        public static void init(Mode mode, int nThreadsOrParallelismLvl) {
            switch (mode) {
                case Single:
                    executorService = Executors.newSingleThreadExecutor();
                    scheduled = false;
                    break;
                case SingleScheduled:
                    executorService = Executors.newSingleThreadScheduledExecutor();
                    scheduled = true;
                    break;
                case Fixed:
                    executorService = Executors.newSingleThreadExecutor();
                    scheduled = false;
                    break;
                case Scheduled:
                    executorService = Executors.newScheduledThreadPool(nThreadsOrParallelismLvl);
                    scheduled = true;
                    break;
                case Cached:
                    executorService = Executors.newCachedThreadPool();
                    scheduled = false;
                    break;
                case WorkStealing:
                    executorService = Executors.newWorkStealingPool(nThreadsOrParallelismLvl);
                    scheduled = false;
                    break;
            }
        }

        /**
         * Submits a piece of work to thread pool
         *
         * @param task Callable to submit
         * @param <T> Return type for callable
         */
        public static <T> Future<T> submit(Callable<T> task) {
            return executorService.submit(task);
        }

        /**
         * Submits a piece of work to thread pool
         *
         * @param task Callable to submit
         */
        public static Future<?> submit(Runnable task) {
            return executorService.submit(task);
        }

        /**
         * Submits a piece of work to thread pool
         *
         * @param task Runnable to submit
         * @param result Return value
         * @param <T> Return type
         */
        public static <T> Future<T> submit(Runnable task, T result) {
            return executorService.submit(task, result);
        }

        /**
         * Executes callable, then invoke Consumer in UI-thread
         *
         * @param work Callable to execute
         * @param uiCallBack Consumer for UI-thread invocation
         * @param <T> Return type
         */
        public static <T> void doWorkThenInvokeUI(final Callable<T> work, Consumer<T> uiCallBack) {
            Callable<T> subcl = () -> {
                Future<T> future = submit(work);
                uiCallBack.accept(future.get(1, TimeUnit.DAYS));
                return future.get();
            };
            submit(subcl);
        }

    }

    private static final List<Consumer<Throwable>> GEC = new ArrayList<>();

    /**
     * Register a Global Exception Consumer, all exception raised in *Safe
     * methods are dispatched in GECs
     *
     * @param cons Consumer to register in GEC
     */
    public static void registerGlobalErrorConsumer(Consumer<Throwable> cons) {
        synchronized (GEC) {
            GEC.add(cons);
        }
    }

    /**
     * Unregister previously registered Global Exception Consumer
     *
     * @param cons Consumer to unregister
     */
    public static void unregisterGlobalErrorConsumer(Consumer<Throwable> cons) {
        synchronized (GEC) {
            GEC.remove(cons);

        }
    }

    /**
     * Invoke runnable and dispatch all thrown exceptions to GEC
     *
     * @param runnable Runnable to invoke
     */
    public static void invokeSafe(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable thr) {
            synchronized (GEC) {
                GEC.forEach((cons) -> cons.accept(thr));
            }
        }
    }

    /**
     * Decorate runnable with another safe-runnable (with GEC)
     *
     * @param runnable Any unsafe Runnable
     * @return Safe Runnable (GEC-enabled)
     */
    public static Runnable makeRunnableSafe(final Runnable runnable) {
        return () -> {
            invokeSafe(runnable);
        };

    }

    /**
     * Safe UI-thread (AWT dispatcher thread) invoker joins UI-thread and
     * execute provided runnable. And performs an additional checking: simply
     * invoke runnable, if already in UI-thread. Blocks current thread and wait
     * to runnable done.
     *
     * @param runnable Runnable to execute
     */
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

    /**
     * Unsafe analog of doUnUIThreadAndWaitSafe(), but still perform UI-thread
     * checking
     *
     * @param runnable Runnable to execute
     * @throws InterruptedException Runnable execution was interrupted
     * @throws InvocationTargetException Exception while Runnable invocation
     */
    public static void doInUIThreadAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeAndWait(runnable);
        }
    }

    /**
     * Decorate runnable with Exception-safe decorator and put it to AWT-event
     * queue. Runnable may executed in AWT-dispatcher thread.
     *
     * @param runnable Runnable to decorate and execute
     */
    public static void doInUIThreadLaterSafe(Runnable runnable) {
        SwingUtilities.invokeLater(makeRunnableSafe(runnable));
    }

    /**
     * Simply put runnable in AWT-event queue "as is" Unsafe and may raise
     * unchecked exceptions
     *
     * @param runnable Runnable to execute
     */
    public static void doInUIThreadLater(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * Process all of components in tree with consumer. Accept parent component
     * with consumer and if component is JComponent subclass then recursively
     * accept all of child components
     *
     * @param component Root-component in tree
     * @param consumer Consumer that accepts components
     */
    public static void doWithEntireComponentTree(Component component, Consumer<Component> consumer) {
        consumer.accept(component);
        if (component instanceof JComponent) {
            JComponent jcomp = (JComponent) component;
            for (Component child : jcomp.getComponents()) {
                doWithEntireComponentTree(child, consumer);
            }
        }
    }

    /**
     * Accepts consumer, only if component is JComponent subclass and not null
     *
     * @param component Component to check and accept
     * @param consumer Consumer that accepts component
     */
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

    /**
     * Load font, and set as main font for all components in components tree.
     * Any raised exceptions are dispatched to GEC.
     *
     * @param fontFile Font-file to load
     * @param components Component-tree roots
     */
    public static void loadFontAsMainSafe(File fontFile, Component... components) {
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

    /**
     * Unsafe (without GEC) variant of loadFontAsMainSafe(File....) method
     *
     * @param fontFile Font-file to load
     * @param components Component-tree roots
     * @throws FontFormatException Wrong or unknown font format
     * @throws IOException IO-error when reading font
     */
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

    /**
     * Load font from InputStream, and set as main font for all components in
     * components tree. Any raised exceptions are dispatched to GEC.
     *
     * @param fontInputStream InputStream
     * @param components Component-tree roots
     */
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

    /**
     * Unsafe (without GEC) variant of loadFontAsMainSafe(InputStream....)
     * method
     *
     * @param fontInputStream InputStream
     * @param components Component-tree roots
     * @throws FontFormatException Wrong or unknown font format
     * @throws IOException IO-error when reading font
     */
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

    /**
     * Load font from URL, and set as main font for all components in components
     * tree. Any raised exceptions are dispatched to GEC.
     *
     * @param fontURL URL of font to load
     * @param components Component-tree roots
     */
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

    /**
     * Unsafe (without GEC) variant of loadFontAsMainSafe(URL....) method
     *
     * @param fontURL URL of font to load
     * @param components Component-tree roots
     * @throws FontFormatException Wrong or unknown font format
     * @throws IOException IO-error when reading font
     */
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

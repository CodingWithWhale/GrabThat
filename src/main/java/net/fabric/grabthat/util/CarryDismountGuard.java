package net.fabric.grabthat.util;

public final class CarryDismountGuard {
    private static final ThreadLocal<Integer> FORCE_COUNT = ThreadLocal.withInitial(() -> 0);

    private CarryDismountGuard() {}

    public static void force() {
        FORCE_COUNT.set(FORCE_COUNT.get() + 1);
    }

    public static void release() {
        FORCE_COUNT.set(FORCE_COUNT.get() - 1);
    }

    public static boolean forced() {
        return FORCE_COUNT.get() > 0;
    }
}
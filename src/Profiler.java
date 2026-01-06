public final class Profiler {

    private Profiler() {
        throw new IllegalStateException("Pas instantiable");
    }

    public static void profile(Runnable r) {
        long start = System.nanoTime();
        r.run();
        long nano = (System.nanoTime() - start);
        System.out.println("Temps: " + (nano / 1_000_000) + "ms");
    }

}

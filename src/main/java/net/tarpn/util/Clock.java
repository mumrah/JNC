package net.tarpn.util;

public interface Clock {
    long millis();

    void sleep(int millis) throws InterruptedException;

    static Clock getRealClock() {
        return new Clock() {
            @Override
            public long millis() {
                return System.currentTimeMillis();
            }

            @Override
            public void sleep(int millis) throws InterruptedException {
                Thread.sleep(millis);
            }
        };
    }
}

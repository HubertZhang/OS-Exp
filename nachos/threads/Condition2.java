package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * <p/>
 * <p/>
 * You must implement this.
 *
 * @see    nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param    conditionLock    the lock associated with this condition
     * variable. The current thread must hold this
     * lock whenever it uses <tt>sleep()</tt>,
     * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {

        // For Q2
        // Get Thread queue and do not transfer priority.
        this.waitList = new RoundRobinScheduler().newThreadQueue(false);

        this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        conditionLock.release();

        waitList.waitForAccess(KThread.currentThread());
        KThread.currentThread().sleep();

        conditionLock.acquire();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {

        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        KThread nextThread = waitList.nextThread();
        if(nextThread != null) {
            nextThread.ready();
        }
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {

        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        KThread nextThread = waitList.nextThread();
        while (nextThread != null) {
            nextThread.ready();
            nextThread = waitList.nextThread();
        }
        Machine.interrupt().restore(intStatus);
    }

    private Lock conditionLock;
    private static final char dbgThread = 't';

    // For Q2
    ThreadQueue waitList;

    // Q2 test cases
    public static void selfTest() {

        Lib.debug(dbgThread, "Condition2 Test begin!");

        Lock lock = new Lock();
        ConditionValue condValue = new ConditionValue(0);
        Condition2 cond = new Condition2(lock);

        KThread thd_sleeper = new KThread(new Sleeper(cond, lock, condValue, 1));
        KThread thd_waker = new KThread(new Waker(cond, lock, condValue, 1));

        thd_sleeper.fork();
        thd_waker.fork();
        thd_sleeper.join();
        thd_waker.join();
    }

    private static class ConditionValue {
        public ConditionValue(int value) {
            this.value = value;
        }

        public void set(int value) {
            this.value = value;
        }

        public int get() {
            return this.value;
        }

        int value;
    }

    private static class Sleeper implements Runnable {

        public Sleeper(Condition2 cond, Lock conditionLock, ConditionValue value, int which) {
            this.cond = cond;
            this.conditionLock = conditionLock;
            this.value = value;
            this.which = which;
        }

        @Override
        public void run() {

            conditionLock.acquire();
            Lib.debug(dbgThread, "Sleeper " + which + " enters critical section.");
            System.out.print("Sleeper " + which + " enters critical section.\n");

            while(value.get() < 1) {
                Lib.debug(dbgThread, "Sleeper " + which + " hang up.");
                System.out.print("Sleeper " + which + " hang up.\n");
                cond.sleep();
                Lib.debug(dbgThread, "Sleeper " + which + " awaken with value=" + value.get());
                System.out.print("Sleeper " + which + " awaken with value=" + value.get() + "\n");
            }

            Lib.debug(dbgThread, "Sleeper " + which + " exits critical section with value=" + value.get());
            System.out.print("Sleeper " + which + " exits critical section with value=" + value.get() + "\n");
            conditionLock.release();

        }

        private Condition2 cond;
        private Lock conditionLock;
        private ConditionValue value;
        private int which;
    }

    private static class Waker implements Runnable {

        public Waker(Condition2 cond, Lock lock, ConditionValue value, int which) {
            this.cond = cond;
            this.lock = lock;
            this.value = value;
            this.which = which;
        }

        @Override
        public void run() {

            lock.acquire();
            Lib.debug(dbgThread, "Waker " + which + " enters critical section.");
            System.out.print("Waker " + which + " enters critical section.\n");
            value.set(2);
            cond.wake();
            Lib.debug(dbgThread, "Waker " + which + " exits critical section with value=" + value.get());
            System.out.print("Waker " + which + " exits critical section with value=." + value.get() + "\n");
            lock.release();

        }

        private Condition2 cond;
        private Lock lock;
        private ConditionValue value;
        private int which;

    }
}

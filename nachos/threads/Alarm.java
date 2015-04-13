package nachos.threads;

import nachos.machine.*;

import java.util.Random;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     * <p/>
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });
    }

    /**
     * Heap should be accessed ATOMICALLY
     */

    private class Heap {
        public class Entry {
            public KThread thread;
            public long time;
            public Entry(KThread thread, long time){
                this.thread = thread; this.time = time;
            }
        }

        private Entry[] array;
        private int size = 0;

        public Heap(int len){
            array = new Entry[len];
        }

        private void up(int pos){
            while(pos > 0){
                int nxt = pos >> 1;
                if(array[nxt].time > array[pos].time){
                    Entry tmp = array[pos]; array[pos] = array[nxt]; array[nxt] = tmp;
                    pos = nxt;
                }
                else break;
            }
        }

        private void down(int pos){
            while(2*pos+1 <= size-1){
                if(2*pos+1 == size-1){
                    if(array[pos].time > array[2*pos+1].time){
                        Entry tmp = array[pos]; array[pos] = array[size-1]; array[size-1] = tmp;
                    }
                    break;
                }
                else {
                    if(array[pos].time <= array[2*pos+1].time && array[pos].time <= array[2*pos+2].time) break;
                    else {
                        if(array[2*pos+1].time < array[2*pos+2].time){
                            Entry tmp = array[pos]; array[pos] = array[2*pos+1]; array[2*pos+1] = tmp;
                            pos = 2*pos+1;
                        }
                        else {
                            Entry tmp = array[pos]; array[pos] = array[2*pos+2]; array[2*pos+2] = tmp;
                            pos = 2*pos+2;
                        }
                    }
                }
            }
        }

        public void add(KThread thread, long time){
            Entry entry = new Entry(thread, time);
            array[size++] = entry;
            up(size-1);
        }

        public KThread pop(){
            if(size == 0) return null;
            KThread ret = array[0].thread;
            array[0] = array[--size];
            down(0);
            return ret;
        }

        public long peek(){
            if(size == 0) return -1;
            else return array[0].time;
        }

        public boolean empty(){
            return size==0;
        }
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt(){
        long curTime = Machine.timer().getTime();
        for(; !heap.empty() && heap.peek() <= curTime; ) {
            KThread thread = heap.pop();
            thread.ready();
            System.out.println(thread.getName());
        }
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     * <p/>
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param    x    the minimum number of clock ticks to wait.
     * @see    nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        // for now, cheat just to get something working (busy waiting is bad)
        long wakeTime = Machine.timer().getTime() + x;
        /*
        while (wakeTime > Machine.timer().getTime())
            KThread.yield();
        */
        lock.acquire();
        heap.add(KThread.currentThread(), wakeTime);
        lock.release();

        boolean intStatus = Machine.interrupt().disable();
        KThread.currentThread().sleep();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Alarm simple testcase
     */
    private static class WaitTest implements Runnable {
        WaitTest(int num, int wait){
            this.num = num;
            this.wait = wait;
        }

        public void run(){
            System.out.println("fall asleep " + num + " at " + Machine.timer().getTime());
            ThreadedKernel.alarm.waitUntil(wait);
            System.out.println("wake up: " + num + " at " + Machine.timer().getTime());
        }

        private int num;
        private int wait;
    }

    public static void selfTest(){
        System.out.print('\n');
        System.out.println("Begin task3");
        int size = 10;
        KThread[] threads = new KThread[size];

        // 10 thds all wait 1000
        System.out.println("alarm test #1 begins.");
        for(int i=0; i<size; i++) {
            threads[i] = new KThread(new WaitTest(i,1000)).setName("wait thread"+i);
            threads[i].fork();
        }
        for(int i=0; i<size; i++) threads[i].join();
        System.out.println("alarm test #1 ends.");

        // 10 thds wait reverse time
        System.out.println("alarm test #2 begins.");
        for(int i=0; i<size; i++) {
            threads[i] = new KThread(new WaitTest(i, 10000-i*1000)).setName("wait thread"+i);
            threads[i].fork();
        }
        for(int i=0; i<size; i++) threads[i].join();
        System.out.println("alarm test #2 ends.");

        //10 thds wait 100
        System.out.println("alarm test #3 begins.");
        for(int i=0; i<size; i++) {
            threads[i] = new KThread(new WaitTest(i,100)).setName("wait thread"+i);
            threads[i].fork();
            KThread.yield();
        }
        for(int i=0; i<size; i++) threads[i].join();
        System.out.println("alarm test #3 ends.");

        //50 thds wait random time with yields
        System.out.println("alarm test #4 begins.");
        Random d = new Random();
        size = 50;
        threads = new KThread[size];
        for(int i=0; i<size; i++) {
            threads[i] = new KThread(new WaitTest(i, d.nextInt()%1500)).setName("wait thread"+i);
            threads[i].fork();
            KThread.yield();
        }
        for(int i=0; i<size; i++) threads[i].join();
        System.out.println("alarm test #4 ends.");

        System.out.println("End task3");
    }

    private Heap heap = new Heap(5000);
    private Lock lock = new Lock();
}

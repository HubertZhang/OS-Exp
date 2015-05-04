package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 * <p/>
 * <p/>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 * <p/>
 * <p/>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 * <p/>
 * <p/>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param    transferPriority    <tt>true</tt> if this queue should
     * transfer priority from waiting threads
     * to the owning thread.
     * @return a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority + 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority - 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param    thread    the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */
    protected static ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        public class ListNode {
            public ThreadState val;
            public ListNode next;
            public ListNode(ThreadState val, ListNode next){
                this.val = val; this.next = next;
            }
        }

        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            threads = new ListNode[priorityMaximum - priorityMinimum +1];
            for(int i=0; i<threads.length; i++){
                threads[i] = new ListNode(null, null);
            }
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            if(lockholder != null){
                if(transferPriority) for(int i=0; i<counts.length; i++) lockholder.counts[i] -= counts[i];
                lockholder = null;
            }
            ThreadState ret = null;
            for(int i=threads.length-1; i>=0; i--){
                if(threads[i].next != null){
                    ListNode prnt = threads[i], cur = prnt.next;
                    for(; cur.next != null; prnt = cur, cur = cur.next);
                    ret = cur.val; prnt.next = null;
                    for(int j=0; j<threads.length; j++){
                        counts[j] -= ret.counts[j];
                        if(transferPriority) ret.counts[j] += counts[j];
                    }
                    break;
                }
            }
            if(ret == null) return null;

            ret.queue = null;
            ret.acquire(this);
            return ret.thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would
         * return.
         */
        protected ThreadState pickNextThread() {
            // implement me
            for(int i=threads.length-1; i>=0; i--){
                if(threads[i].next!=null){
                    return threads[i].val;
                }
            }
            return null;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        public void add(ThreadState node){
            // for(int i=0; i<counts.length; i++) counts[i] += node.counts[i];
            int prio = node.getEffectivePriority();
            threads[prio].next = new ListNode(node, threads[prio].next);
        }

        public void swap(int oldval, int newval, ThreadState thread){
            ListNode prnt = threads[oldval];
            ListNode cur = threads[oldval].next;
            Lib.assertTrue(cur != null);
            for(; cur != null && cur.val != thread; prnt = cur, cur = cur.next);
            Lib.assertTrue(cur != null && cur.val == thread);
            prnt.next = cur.next;
            cur.next = threads[newval].next; threads[newval].next = cur;
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        private ListNode[] threads;
        public int[] counts = new int[priorityMaximum - priorityMinimum +1];

        public boolean transferPriority;
        public ThreadState lockholder = null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see    nachos.threads.KThread#schedulingState
     */

    protected static class ThreadState {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param    thread    the thread this state belongs to.
         */

        public ThreadState(KThread thread) {
            this.thread = thread;
            priority = priorityDefault; counts[priorityDefault] = 1;
            // setPriority(priorityDefault);
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            // implement me
            // update effectivePriority each-time
            for(int i=counts.length-1; i>0; i--){
                if(counts[i] > 0) return i;
            }
            return 0;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param    priority    the new priority.
         */
        public void setPriority(int priority) {
            if(loop) return;
            if(priority == this.priority) return;
            int old = this.priority;
            this.priority = priority;

            ThreadState cur = this;
            do{
                PriorityQueue Q = cur.queue;
                if(Q == null) {
                    cur.counts[old]--; cur.counts[priority]++;
                    break;
                }
                else {
                    Q.counts[old]--; Q.counts[priority]++;
                    int oldval = cur.getEffectivePriority();
                    cur.counts[old]--; cur.counts[priority]++;
                    int newval = cur.getEffectivePriority();
                    if(oldval != newval) Q.swap(oldval, newval, cur);
                    if(Q.transferPriority) cur = Q.lockholder;
                    else break;
                }
            }while(cur != this && cur != null);
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param    waitQueue    the queue that the associated thread is
         * now waiting on.
         * @see    nachos.threads.ThreadQueue#waitForAccess
         */

        private boolean checkLoop(){
            ThreadState fast = this, slow = this;
            while(fast.queue != null && fast.queue.transferPriority && fast.queue.lockholder != null){
                slow = slow.queue.lockholder;
                fast = fast.queue.lockholder;
                if(fast.queue == null || !fast.queue.transferPriority || fast.queue.lockholder == null) break;
                fast = fast.queue.lockholder;
                if(fast == slow) return true;
            }
            return false;
        }

        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
            queue = waitQueue;
            // come-back to the waitQueue
            if(this == waitQueue.lockholder){
                waitQueue.lockholder = null;
                if(waitQueue.transferPriority) for(int i=0; i<counts.length; i++) counts[i] -= waitQueue.counts[i];
            }
            waitQueue.add(this);

            if(checkLoop()) {
                System.out.println("user deadlock");
                loop = true;
                return;
            }

            PriorityQueue Q = queue;
            while(Q != null){
                for(int i=0; i<counts.length; i++) Q.counts[i] += counts[i];
                ThreadState cur = Q.lockholder;
                if(cur == null || !Q.transferPriority || cur == this) break;
                else {
                    Q = cur.queue;
                    if(Q == null){
                        for(int i=0; i<counts.length; i++) cur.counts[i] += counts[i];
                        break;
                    }
                    int oldval = cur.getEffectivePriority();
                    for(int i=0; i<counts.length; i++) cur.counts[i] += counts[i];
                    int newval = cur.getEffectivePriority();
                    if(oldval != newval) Q.swap(oldval, newval, cur);
                }
            }
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see    nachos.threads.ThreadQueue#acquire
         * @see    nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            // implement me
            waitQueue.lockholder = this;
        }

        /**
         * The thread with which this object is associated.
         */
        protected KThread thread;
        private boolean loop = false;
        /**
         * The priority of the associated thread.
         */
        protected int priority;

        public PriorityQueue queue;
        public int[] counts = new int[priorityMaximum - priorityMinimum + 1];
    }

    private static class PingTest implements Runnable {
        PingTest(int which) {
            this.which = which;
        }

        public void run() {
            for (int i = 0; i < 5; i++) {
                System.out.println("*** thread " + which + " looped "
                        + i + " times");
                KThread.currentThread().yield();
            }
        }

        private int which;
    }

    private static class ChainTest implements Runnable {
        public ChainTest(int len){this.len = len;}

        public void run(){
            if(len > 0){
                KThread lo = new KThread(new ChainTest(len-1)).setName("low priority");
                getThreadState(lo).setPriority(0);
                lo.fork();
                KThread.yield();
                lo.join();
            }
            for (int i = 0; i < 5; i++) {
                System.out.println("*** thread " + (100+len) + " looped "
                        + i + " times");
                KThread.currentThread().yield();
            }
        }

        private int len;
    }

    private static class ChainTestR implements Runnable {
        public ChainTestR(int prio, KThread lo){
            this.prio = prio;
            this.lo = lo;
        }

        public void run(){
            System.out.println("thd with prio "+prio+" starts");
            if(lo != null){
                lo.join();
            }
            else {
                for (int i = 0; i < 10; i ++){
                    System.out.println("inner loop "+i);
                    KThread.yield();
                }
            }
            System.out.println("thd with prio "+prio+" finishes.");
        }
        private int prio;

        private KThread lo;
    }

    private static class GetLock implements Runnable {
        public GetLock(Lock lock, int prio){this.lock = lock; this.prio = prio;}

        public void run(){
            getThreadState(KThread.currentThread()).setPriority(prio);
            lock.acquire();
            for (int i = 0; i < 5; i ++){
                System.out.println(KThread.currentThread().getName());
                KThread.currentThread().yield();
            }
            lock.release();
        }

        private Lock lock;
        private int prio;
    }

    private static class JoinThis implements Runnable{
        public JoinThis(Lock lock, KThread thd){
            this.thd = thd; lk = lock;
        }

        public void run(){
            lk.acquire();
            thd.join();
        }

        private Lock lk;
        private KThread thd;
    }

    public static void selfTest(){
        System.out.println("Begin task5 test");

        // chain test 10 len
        System.out.println("priority scheduling test #1 begin.");
        KThread t = new KThread(new ChainTest(10));
        t.fork();
        t.join();
        System.out.println("priority scheduling test #1 end.");

        // chain test 10 len reversed
        System.out.println("priority scheduling test #2 begin.");
        int size = 10;
        Vector<KThread> thds = new Vector<KThread>();
        thds.add(new KThread(new ChainTestR(0,null)));
        for(int i = 1; i < size; i++) {
            thds.add(new KThread(new ChainTestR(i, thds.get(i - 1))));
        }
        for(int i = 0; i < size; i++) {
            thds.get(i).fork();
            KThread.yield();
        }
        for(KThread k:thds) k.join();

        System.out.println("priority scheduling test #2 end.");

        // get lock test
        System.out.println("priority scheduling test #3 begin.");
        Lock lock = new Lock();
        KThread hi = new KThread(new GetLock(lock, 7)).setName("high priority");
        KThread lo = new KThread(new GetLock(lock, 0)).setName("low priority");
        getThreadState(hi).setPriority(7);
        getThreadState(lo).setPriority(7);
        KThread[] mids = new KThread[10];
        for(int i=0; i<10; i++) {
            mids[i] = new KThread(new PingTest(i+1)).setName("mid priority");
            getThreadState(mids[i]).setPriority(4);
        }
        lo.fork();
        hi.fork();
        for(int i=0; i<10; i++) mids[i].fork();
        lo.join();
        hi.join();
        for(int i=0; i<10; i++) mids[i].join();
        System.out.println("priority scheduling test #3 end.");
        System.out.println("End task5 test");

        System.out.println("priority scheduling test #4 begin.");
        KThread A = new KThread().setName("thd A");
        KThread B = new KThread().setName("thd B");
        KThread C = new KThread().setName("thd C");
        A.setTarget(new ChainTestR(7, B));
        B.setTarget(new ChainTestR(7, A));
        C.setTarget(new ChainTestR(7, A));
        A.fork(); B.fork(); C.fork();
        System.out.println("priority scheduling test #4 end.");
    }
}

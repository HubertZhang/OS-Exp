package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

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
            ListNode newnode = new ListNode(node, threads[prio].next);
            threads[prio].next = newnode;
        }

        public void swap(int oldval, int newval, ThreadState thread){
            ListNode prnt = threads[oldval];
            ListNode cur = threads[oldval].next;
            Lib.assertTrue(cur != null);
            for(; cur.val != thread; prnt = cur, cur = cur.next);
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
            priority = 0; counts[0] = 1;
            setPriority(priorityDefault);
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
            int old = this.priority;
            this.priority = priority;

            ThreadState cur = this;
            while(cur != null){
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
                    if(Q.transferPriority){
                        cur = Q.lockholder;
                    }
                    else break;
                }
            }
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
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
            queue = waitQueue;
            // come-back to the waitQueue
            if(this == waitQueue.lockholder){
                waitQueue.lockholder = null;
                if(waitQueue.transferPriority) for(int i=0; i<counts.length; i++) counts[i] -= waitQueue.counts[i];
            }
            waitQueue.add(this);

            PriorityQueue Q = queue;
            while(Q != null){
                for(int i=0; i<counts.length; i++) Q.counts[i] += counts[i];
                ThreadState cur = Q.lockholder;
                if(cur == null || !Q.transferPriority) break;
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

    public static void selfTest(){
        System.out.println("Begin task5 test");
        KThread hi = new KThread(new Runnable() {
            public void run(){
                KThread lo = new KThread(new PingTest(11)).setName("low priority");
                getThreadState(lo).setPriority(0);
                lo.fork();
                lo.join();
                for (int i = 0; i < 5; i++) {
                    System.out.println("*** thread " + 0 + " looped "
                            + i + " times");
                    KThread.currentThread().yield();
                }
            }
        }).setName("high priority");
        getThreadState(hi).setPriority(7);
        KThread[] mids = new KThread[10];
        for(int i=0; i<10; i++) {
            mids[i] = new KThread(new PingTest(i+1)).setName("mid priority");
            getThreadState(mids[i]).setPriority(4);
        }
        hi.fork();
        for(int i=0; i<10; i++) mids[i].fork();
        hi.join();
        for(int i=0; i<10; i++) mids[i].join();
        System.out.println("End task5 test");
    }
}

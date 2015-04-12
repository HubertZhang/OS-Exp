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
    protected ThreadState getThreadState(KThread thread) {
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
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            Lib.assertTrue(lockholder.thread.schedulingState != null);
            expose(lockholder); lockholder.checkout(this);

            ThreadState ret = null;
            for(int i=threads.length-1; i>=0; i--){
                if(threads[i]!=null){
                    ret = threads[i].val;
                    threads[i] = threads[i].next;
                    break;
                }
            }
            splay(ret);
            Lib.assertTrue(ret.parent == null);

            for(int i=0; i<threads.length; i++){
                for(ListNode cur = threads[i]; cur!=null; cur=cur.next){
                    splay(cur.val);
                    cur.val.parent = ret;
                }
            }

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
                if(threads[i]!=null){
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
            int prio;
            if(transferPriority) prio = node.getEffectivePriority();
            else prio = node.getPriority();
            ListNode newEntry = new ListNode(node, threads[prio]);
            threads[prio] = newEntry;
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        private ListNode[] threads = null;

        public boolean transferPriority;
        public ThreadState lockholder = null;

        public int getMaxPriority(){
            for(int i=threads.length-1; i>0; i--){
                if(threads[i] != null) return i;
            }
            return 0;
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see    nachos.threads.KThread#schedulingState
     */

    private void rotL(ThreadState node){
        ThreadState prnt = node.getParent();
        Lib.assertTrue(prnt != null && prnt.right == node);

        ThreadState grnd = prnt.getParent();
        if(grnd != null){
            node.parent = grnd;
            if(grnd.left == prnt) grnd.left = node;
            else grnd.right = node;
        }
        ThreadState l = node.left;

        prnt.parent = node; node.left = prnt;

        prnt.right = l;
        if(l != null) l.parent = prnt;
    }

    private void rotR(ThreadState node){
        ThreadState prnt = node.getParent();
        Lib.assertTrue(prnt != null && prnt.left == node);

        ThreadState grnd = prnt.getParent();
        if(grnd != null){
            node.parent = grnd;
            if(grnd.left == prnt) grnd.left = node;
            else grnd.right = node;
        }
        ThreadState r = node.right;

        prnt.parent = node; node.right = prnt;

        prnt.left = r;
        if(r != null) r.parent = prnt;
    }

    private void splay(ThreadState node){
        ThreadState prnt = null;
        while((prnt = node.getParent()) != null){
            ThreadState grnd = prnt.getParent();
            if(grnd == null){
                if(prnt.left == node) rotR(node);
                else rotL(node);
                break;
            }
            else {
                if(grnd.left == prnt){
                    if(prnt.left == node) rotR(node);
                    else rotL(node);
                    rotR(node);
                }
                else {
                    if(prnt.right == node) rotL(node);
                    else rotR(node);
                    rotL(node);
                }
            }
        }
    }

    private ThreadState expose(ThreadState node){
        ThreadState ret = null;
        for(; node!=null; node = node.parent){
            splay(node);
            node.right = ret;
            (ret = node).update();
        }
        return ret;
    }

    protected class ThreadState {
        private List<PriorityQueue> kids;

        public void checkout(PriorityQueue waitQueue){
            boolean flag = false;
            for(PriorityQueue iter : kids){
                if(iter == waitQueue){
                    kids.remove(iter);
                    flag = true;
                    break;
                }
            }
            Lib.assertTrue(flag);
        }

        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param    thread    the thread this state belongs to.
         */

        public ThreadState(KThread thread) {
            kids = new LinkedList<PriorityQueue>();
            this.thread = thread;
            submax = priorityDefault;
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
            if(reset){
                splay(this);
                reset = false;
                effectivePriority = right.submax > priority? right.submax : priority;
            }
            return effectivePriority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param    priority    the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;
            effectivePriority = priority;
            for(PriorityQueue iter : kids){
                int prio = iter.getMaxPriority();
                if(effectivePriority < prio) effectivePriority = prio;
            }
            expose(this);
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
            parent = waitQueue.lockholder;
            expose(this);
            this.kids.add(waitQueue);
            waitQueue.add(this);
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

        public int effectivePriority;

        public ThreadState parent;
        public ThreadState left;
        public ThreadState right;

        public int submax;
        public boolean reset;

        public ThreadState getParent(){
            if(parent == null || (parent.left != this && parent.right != this))
                return null;
            else return parent;
        }

        public void update(){
            reset = true;
            submax = priority;
            if(left != null){
                submax = left.submax>submax? left.submax : submax;
            }
            if(right != null){
                submax = right.submax>submax? right.submax : submax;
            }
        }
    }
}

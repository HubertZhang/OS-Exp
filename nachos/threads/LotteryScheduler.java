package nachos.threads;

import nachos.machine.*;

import java.util.Random;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 * <p/>
 * <p/>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * <p/>
 * <p/>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * <p/>
 * <p/>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }

    /**
     * Allocate a new lottery thread queue.
     *
     * @param    transferPriority    <tt>true</tt> if this queue should
     * transfer tickets from waiting threads
     * to the owning thread.
     * @return a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        // implement me
        return new LotteryQueue(transferPriority);
    }

    protected static LotteryState getThreadState(KThread thread){
        if(thread.schedulingState == null)
            thread.schedulingState = new LotteryState(thread);
        return (LotteryState) thread.schedulingState;
    }

    public int getPriority(KThread thread){
        Lib.assertTrue(Machine.interrupt().disabled());
        return getThreadState(thread).tickets;
    }

    public void setPriority(KThread thread, int priority){
        boolean intStatus = Machine.interrupt().disable();

        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(priority > 0 && priority <= Integer.MAX_VALUE);
        getThreadState(thread).setTickets(priority);

        Machine.interrupt().restore(intStatus);
    }

    public boolean increasePriority(){
        boolean intStatus = Machine.interrupt().disable();
        KThread thread = KThread.currentThread();
        int prio = getPriority(thread);
        if(prio == Integer.MAX_VALUE) return false;
        setPriority(thread, prio + 1);
        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority(){
        boolean intStatus = Machine.interrupt().disable();
        KThread thread = KThread.currentThread();
        int prio = getPriority(thread);
        if(prio == 1) return false;
        setPriority(thread, prio-1);
        Machine.interrupt().restore(intStatus);
        return true;
    }

    private class LotteryQueue extends ThreadQueue {
        public LotteryState holder;
        public LotteryState root = null;

        public boolean transferLottery;

        LotteryQueue(boolean transferLottery){
            this.transferLottery = transferLottery;
        }

        public void print(){}

        public void waitForAccess(KThread thread){
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread){
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public void add(LotteryState node){
            node.left = root;
            if(root != null) {
                node.subsum += root.subsum;
                root.parent = node;
            }
            root = node;
        }

        public KThread nextThread(){
            if(holder != null){
                if(transferLottery && root != null)
                    holder.tickets -= root.subsum;
                holder = null;
            }
            if(root == null) return null;

            int nxt = 1 + new Random().nextInt(root.subsum);
            LotteryState cur = root;
            while(true){
                assert(cur != null);
                int ls = cur.left!=null? cur.left.subsum : 0;
                int rs = cur.right!=null? cur.right.subsum : 0;
                if(nxt > ls + cur.tickets){
                    nxt -= ls + cur.tickets;
                    cur = cur.right;
                }
                else if(nxt <= ls){
                    cur = cur.left;
                }
                else {
                    break;
                }
            }
            root = remove(cur);
            cur.queue = null;
            cur.acquire(this);
            return cur.thread;
        }
    }

    private LotteryState remove(LotteryState node){
        splay(node);
        LotteryState lft = node.left;
        LotteryState rght= node.right;
        node.left = null; node.right = null;
        if(lft != null) lft.parent = null;
        if(rght != null) rght.parent = null;
        node.subsum = node.tickets;

        if(lft == null) return rght;
        if(rght == null) return lft;

        LotteryState cur = lft;
        for(; cur.right != null; cur = cur.right);

        splay(cur);
        cur.right = rght;
        rght.parent = cur;
        cur.subsum += rght.subsum;

        return cur;
    }

    private void splay(LotteryState cur){
        LotteryState prnt;
        while((prnt = cur.parent) != null){
            LotteryState grnd = prnt.parent;
            if(grnd == null){
                if(prnt.left == cur) rotR(cur);
                else rotL(cur);
                return;
            }
            if(grnd.left == prnt){
                if(prnt.left == cur) rotR(prnt);
                else rotL(cur);
                rotR(cur);
            }
            else {
                if(prnt.right == cur) rotL(prnt);
                else rotR(cur);
                rotL(cur);
            }
        }
    }

    private void rotL(LotteryState cur){
        LotteryState prnt = cur.parent;
        assert(prnt != null && prnt.right == cur);
        LotteryState grnd = prnt.parent;
        if(grnd != null){
            if(grnd.left == prnt) grnd.left = cur;
            else grnd.right = cur;
        }
        cur.parent = grnd;
        prnt.parent = cur;
        LotteryState lft = cur.left;
        cur.left = prnt;

        prnt.subsum -= cur.subsum;
        cur.subsum += prnt.subsum;

        prnt.right = lft;
        if(lft != null){
            prnt.subsum += lft.subsum;
            lft.parent = prnt;
        }
    }

    private void rotR(LotteryState cur){
        LotteryState prnt = cur.parent;
        assert(prnt != null && prnt.left == cur);
        LotteryState grnd = prnt.parent;
        if(grnd != null){
            if(grnd.right == prnt) grnd.right = cur;
            else grnd.left = cur;
        }
        cur.parent = grnd;
        prnt.parent = cur;
        LotteryState rght = cur.right;
        cur.right = prnt;

        prnt.subsum -= cur.subsum;
        cur.subsum += prnt.subsum;

        prnt.left = rght;
        if(rght != null){
            prnt.subsum += rght.subsum;
            rght.parent = prnt;
        }
    }

    private static class LotteryState extends ThreadState{
        // public KThread thread;
        public LotteryQueue queue = null;
        public int tickets;
        public int subsum;
        private boolean loop = false;

        public LotteryState parent = null, left = null, right = null;

        public LotteryState(KThread thread){
            super(thread);
            // this.thread = thread;
            tickets = 1; subsum = tickets;
        }

        public void setTickets(int num){
            int diff = num - tickets;
            tickets = num;
            if(diff == 0) return;
            LotteryQueue Q = queue;
            LotteryState cur = this;
            while(true){
                for(LotteryState nd = cur; nd != null; nd = nd.parent)
                    nd.subsum += diff;
                if(Q == null) break;
                cur = Q.holder;
                if(Q.transferLottery && cur != null){
                    cur.tickets += diff;
                    Q = cur.queue;
                }
                else break;
            }
        }

        public void acquire(LotteryQueue waitQueue){
            waitQueue.holder = this;
        }

        private boolean checkLoop(){
            LotteryState fast = this, slow = this;
            while(fast.queue != null
                    && fast.queue.transferLottery
                    && fast.queue.holder != null)
            {
                slow = slow.queue.holder;
                fast = fast.queue.holder;
                if(fast.queue == null
                        || !fast.queue.transferLottery
                        || fast.queue.holder == null) break;
                fast = fast.queue.holder;
                if(fast == slow) return true;
            }
            return false;
        }

        public void waitForAccess(LotteryQueue waitQueue){
            queue = waitQueue;
            if(this == waitQueue.holder){
                waitQueue.holder = null;
                if(waitQueue.transferLottery) {
                    tickets -= waitQueue.root.subsum;
                    subsum = tickets;
                }
            }
            if(checkLoop()){
                System.out.println("user deadlock");
                loop = true;
                return;
            }

            waitQueue.add(this);
            LotteryState cur = queue.holder;
            LotteryQueue Q = queue;
            while(Q.transferLottery && cur != null){
                cur.tickets += tickets;
                for(LotteryState nd = cur; nd != null; nd = nd.parent)
                    nd.subsum += tickets;
                if(cur.queue != null){
                    Q = cur.queue;
                    cur = Q.holder;
                }
                else break;
            }
        }
    }

    private static class PrintTest implements Runnable {
        public void run(){
            for(int i=0; i<5; i++){
                System.out.println(KThread.currentThread().getName());
                KThread.yield();
            }
        }
    }

    private static class JoinTest implements Runnable {
        public void run(){
            KThread lo = new KThread(new PrintTest()).setName("lo");
            lo.fork();
            lo.join();
            for(int i=0; i<5; i++){
                System.out.println(KThread.currentThread().getName());
                KThread.yield();
            }
        }
    }

    public static void selfTest(){
        System.out.println("Begin Lottery test");

        KThread hi = new KThread(new JoinTest()).setName("hi");
        ThreadedKernel.scheduler.setPriority(hi, 200);
        hi.fork();
        KThread[] mids = new KThread[10];
        for(int i=0; i<10; i++){
            mids[i] = new KThread(new PrintTest()).setName("mid " + i);
            ThreadedKernel.scheduler.setPriority(mids[i], 5);
            mids[i].fork();
        }
        hi.join();
        for(int i=0; i<10; i++) mids[i].join();

        System.out.println("End Lottery test");
    }
}

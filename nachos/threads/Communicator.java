package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lockCntr = new Lock();
        SCounter = 0;
        LCounter = 0;
        msg = 0;
        cond = new Condition(lockCntr);
        cond_l = new Condition(lockCntr);
        cond_f = new Condition(lockCntr);
        read = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     * <p/>
     * <p/>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param    word    the integer to transfer.
     */
    public void speak(int word) {
        Lib.debug(dbgThread, "Enter speak");

        lockCntr.acquire();

        /**
         * While There is a message on going, stop and wait.
         */
        while (!read) {
            cond_s.sleep();
        }

        SCounter++;
        if (LCounter > 0) {
            /**
             * If there are some listeners waiting, just leave the message and wake up a listener.
             */
            msg = word;
            read = false;
            cond.wake();
        }
        else {
            /**
             * If there is no listener, wait until one listener comes.
             */
            while (LCounter == 0) {
                cond.sleep();
            }
            msg = word;
            read = false;
            cond_l.wake();
        }

        /**
         * Before the message is read, stop and read.
         */
        while (!read) {
            cond_f.sleep();
        }

        /**
         * Wake up another speaker if there is any and leave.
         */
        cond_s.wake();
        LCounter--;
        SCounter--;

        lockCntr.release();

    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        int rnt_val;
        lockCntr.acquire();

        LCounter++;
        if (SCounter > 0) {
            /**
             * If there are some speakers waiting, just wake up one and waiting for him to write.
             */
            cond.wake();
            cond_l.sleep();
        }
        else {
            /**
             * If there is no speakers waiting, stop and wait.
             */
            while (SCounter == 0) {
                cond.sleep();
            }
        }
        /**
         * Get the speaker's message and report read.
         */
        rnt_val = msg;
        read = true;
        cond_f.wake();

        lockCntr.release();
        return rnt_val;
    }

    private static final char dbgThread = 't';

    private Lock lockCntr;
    private int SCounter;
    private int LCounter;
    private int msg;
    private Condition cond;
    private Condition cond_l;
    private Condition cond_f;
    private Condition cond_s;
    private boolean read;
}

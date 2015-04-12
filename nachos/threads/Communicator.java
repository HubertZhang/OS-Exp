package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Vector;

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
        cond_s = new Condition(lockCntr);
        read = true;
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
            System.out.println("Sleep on cond_s.");
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
                System.out.println("Speaker sleep on cond.");
                cond.sleep();
            }
            msg = word;
            read = false;
            cond_l.wake();
        }

        /**
         * Before the message is read, stop and read.
         */
        System.out.println("Sleep on cond_f.");
        cond_f.sleep();

        /**
         * Wake up another speaker if there is any and leave.
         */
        cond_s.wake();
        read = true;

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
            System.out.println("Sleep on cond_l.");
            cond_l.sleep();
        }
        else {
            /**
             * If there is no speakers waiting, stop and wait.
             */
            while (SCounter == 0) {
                System.out.println("Listener sleep on cond.");
                cond.sleep();
            }
        }
        /**
         * Get the speaker's message and report read.
         */
        rnt_val = msg;
        cond_f.wake();
        LCounter--;
        SCounter--;

        lockCntr.release();
        return rnt_val;
    }

    public static void selfTest() {

        Lib.debug(dbgThread, "Communicator tests begin!");

        Communicator channel = new Communicator();

        // sl
        System.out.print("Communicator tests #1 begin. Speaker first.\n");
        KThread thd_speaker = new KThread(new Speaker(channel, 1));
        KThread thd_listener = new KThread(new Listener(channel, 1));
        thd_speaker.fork();
        thd_listener.fork();
        thd_speaker.join();
        thd_listener.join();
        System.out.print("Communicator tests #1 finishes.\n");

        // ls
        System.out.print("Communicator tests #2 begin!\n");
        thd_listener = new KThread(new Listener(channel, 2));
        thd_speaker = new KThread(new Speaker(channel, 2));
        thd_listener.fork();
        thd_speaker.fork();
        thd_listener.join();
        thd_speaker.join();
        System.out.print("Communicator tests #2 finishes.\n");

        // lllllsssss with speakers much faster than reader
        System.out.print("Communicator tests #3 begin!\n");
        Vector<KThread> thd_listeners = new Vector<KThread>();
        Vector<KThread> thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (KThread k: thd_listeners) {
            k.fork();
        }
        for (KThread k: thd_speakers) {
            k.fork();
        }
        for (KThread k: thd_listeners){
            k.join();
        }
        for (KThread k: thd_speakers) {
            k.join();
        }
        System.out.print("Communicator tests #3 finishes.\n");

        // lllllsssss with speakers slower than reader
        System.out.print("Communicator tests #4 begin!\n");
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (KThread k: thd_listeners) {
            k.fork();
        }
        for (KThread k: thd_speakers) {
            k.fork();
            KThread.yield();
        }
        for (KThread k: thd_listeners){
            k.join();
        }
        for (KThread k: thd_speakers) {
            k.join();
        }
        System.out.print("Communicator tests #4 finishes.\n");

        // ssssslllll with listener much faster than speaker
        System.out.print("Communicator tests #5 begin!\n");
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (KThread k: thd_speakers) {
            k.fork();
        }
        for (KThread k: thd_listeners) {
            k.fork();
        }
        for (KThread k: thd_speakers) {
            k.join();
        }
        for (KThread k: thd_listeners){
            k.join();
        }
        System.out.print("Communicator tests #5 finishes.\n");

        // ssssslllll with listener slower than speaker
        System.out.print("Communicator tests #6 begin!\n");
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (KThread k: thd_speakers) {
            k.fork();
        }
        for (KThread k: thd_listeners) {
            k.fork();
            KThread.yield();
        }
        for (KThread k: thd_speakers) {
            k.join();
        }
        for (KThread k: thd_listeners){
            k.join();
        }
        System.out.print("Communicator tests #6 finishes.\n");

        /*  bug here
        // lslslslsls
        System.out.print("Communicator tests #7 begin!\n");
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (int i = 0; i < 5; i++){
            thd_listeners.get(i).fork();
            thd_speakers.get(i).fork();
        }
        for (int i = 0; i < 5; i++){
            thd_listeners.get(i).join();
            thd_speakers.get(i).join();
        }
        System.out.print("Communicator tests #7 finishes.\n");

        // slslslslsl
        System.out.print("Communicator tests #8 begin!\n");
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (int i = 0; i < 5; i++){
            thd_speakers.get(i).fork();
            thd_listeners.get(i).fork();
        }
        for (int i = 0; i < 5; i++){
            thd_speakers.get(i).join();
            thd_listeners.get(i).join();
        }
        System.out.print("Communicator tests #8 finishes.\n");
        */
    }

    private static class Speaker implements Runnable{

        public Speaker(Communicator channel, int which) {
            this.which = which;
            this.channel = channel;
        }

        @Override
        public void run() {
            Lib.debug(dbgThread, "Speaker " + which + " runs.");
            System.out.print("Speaker " + which + " runs\n");

            //for (int i = 0; i != 5; i++) {
                System.out.print("Speaker " + which + " speaks "+ which  + "\n");
                channel.speak(which);
                System.out.print("Speaker " + which + " ends speaking.\n");
            //}

            Lib.debug(dbgThread, "Speaker " + which + " termiantes.");
            System.out.print("Speaker " + which + " terminates.\n");
        }

        private int which;
        private Communicator channel;
    }

    private static class Listener implements Runnable {

        public Listener(Communicator channel, int which) {
            this.which = which;
            this.channel = channel;
        }

        @Override
        public void run() {
            Lib.debug(dbgThread, "Listener " + which + " runs.");
            System.out.print("Listener " + which + " runs.\n");

            //for (int i = 0; i != 5; i++) {
                System.out.print("Listener " + which + " begin listening.\n");
                int msg = channel.listen();
                System.out.print("Listener " + which + " listened " + msg + "\n");
            //}

            Lib.debug(dbgThread, "Listener " + which + " terminates.");
            System.out.print("Listener " + which + " terminates.\n");
        }

        private int which;
        private Communicator channel;
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

package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Random;
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
        msg = 0;
        flag_l = false;
        flag_s = false;
        wait_l = false;
        wait_s = false;
        cond = new Condition(lockCntr);
        cond_l = new Condition(lockCntr);
        cond_s = new Condition(lockCntr);
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

        while (flag_s) {
            System.out.println("Speaker sleep on cond_s.");
            cond_s.sleep();
        }

        flag_s = true;

        if (wait_l) {
            cond.wake();
        }
        else {
            wait_s = true;
            System.out.println("Speaker sleep on cond");
            cond.sleep();
            cond.wake();
            wait_s = false;
        }

        msg = word;
        System.out.println("Speaker sleep on cond to wait for listen");
        cond.sleep();

        flag_s = false;
        flag_l = false;

        cond_s.wake();
        cond_l.wake();

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

        while (flag_l) {
            System.out.println("Listener sleep on cond_l.");
            cond_l.sleep();
        }

        flag_l = true;

        if (wait_s) {
            cond.wake();
            System.out.println("Listener sleep on cond to wait for message.");
            cond.sleep();
        }
        else {
            wait_l = true;
            System.out.println("Listener sleep on cond.");
            cond.sleep();
            wait_l = false;
        }

        rnt_val = msg;
        cond.wake();

        lockCntr.release();
        return rnt_val;
    }

    public static void selfTest() {

        Lib.debug(dbgThread, "Communicator tests begin!");
        System.out.println("Communicator tests begin.");

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

        // lslslslsls with yield
        System.out.print("Communicator tests #9 begin!\n");
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (int i = 0; i < 5; i++){
            thd_listeners.get(i).fork();
            thd_speakers.get(i).fork();
            KThread.yield();
        }
        for (int i = 0; i < 5; i++){
            thd_listeners.get(i).join();
            thd_speakers.get(i).join();
        }
        System.out.print("Communicator tests #9 finishes.\n");

        // slslslslsl with yield
        System.out.print("Communicator tests #10 begin!\n");
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0; i < 5; i++){
            thd_listeners.add(new KThread(new Listener(channel, i)));
            thd_speakers.add(new KThread(new Speaker(channel, i)));
        }
        for (int i = 0; i < 5; i++){
            thd_speakers.get(i).fork();
            thd_listeners.get(i).fork();
            KThread.yield();
        }
        for (int i = 0; i < 5; i++){
            thd_speakers.get(i).join();
            thd_listeners.get(i).join();
        }
        System.out.print("Communicator tests #10 finishes.\n");

        // random test
        System.out.print("Communicator tests #11 begin!\n");
        int size = 20;
        int nl = size, ns = size;
        Random d = new Random();
        thd_listeners = new Vector<KThread>();
        thd_speakers = new Vector<KThread>();
        for (int i = 0;i < 2*size; i++) {
            if (ns == 0 || (d.nextInt()%2 == 1 && nl > 0)) {
                thd_listener = new KThread(new Listener(channel, size - nl));
                thd_listeners.add(thd_listener);
                nl--;
                thd_listener.fork();
                KThread.yield();
            }
            else {
                thd_speaker = new KThread(new Speaker(channel, size - ns));
                thd_speakers.add(thd_speaker);
                ns--;
                thd_speaker.fork();
                KThread.yield();
            }
        }
        for (KThread k: thd_listeners) k.join();
        for (KThread k: thd_speakers) k.join();
        System.out.print("Communicator tests #11 finishes.\n");

        System.out.println("Communicator tests over.\n");
    }

    private static class Speaker implements Runnable{

        public Speaker(Communicator channel, int which) {
            this.which = which;
            this.channel = channel;
        }

        @Override
        public void run() {

            //for (int i = 0; i != 5; i++) {
            System.out.print("Speaker " + which + " speaks "+ which  + "\n");
            channel.speak(which);
            System.out.print("Speaker " + which + " ends speaking.\n");
            //}
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

            //for (int i = 0; i != 5; i++) {
            System.out.print("Listener " + which + " begin listening.\n");
            int msg = channel.listen();
            System.out.print("Listener " + which + " listened " + msg + "\n");
            //}

        }

        private int which;
        private Communicator channel;
    }

    private static final char dbgThread = 't';

    private Lock lockCntr;
    private int msg;

    private boolean flag_s, flag_l, wait_l, wait_s;
    private Condition cond_s, cond_l, cond;
}

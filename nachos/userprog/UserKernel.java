package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
        super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
        super.initialize(args);

        console = new SynchConsole(Machine.console());
        memLock = new Lock();
        Machine.processor().setExceptionHandler(new Runnable() {
            public void run() {
                exceptionHandler();
            }
        });
    }

    /**
     * Test the console device.
     */
    public void selfTest() {
//        super.selfTest();

        System.out.println("Testing the console device. Typed characters");
        System.out.println("will be echoed until q is typed.");

        char c;

        do {
            c = (char) console.readByte(true);
            console.writeByte(c);
        }
        while (c != 'q');

        System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
        if (!(KThread.currentThread() instanceof UThread))
            return null;

        return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     * <p/>
     * <p/>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
        Lib.assertTrue(KThread.currentThread() instanceof UThread);

        UserProcess process = ((UThread) KThread.currentThread()).process;
        int cause = Machine.processor().readRegister(Processor.regCause);
        process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see    nachos.machine.Machine#getShellProgramName
     */
    public void run() {
        super.run();

        UserProcess process = UserProcess.newUserProcess();

        String shellProgram = Machine.getShellProgramName();
        Lib.assertTrue(process.execute(shellProgram, new String[]{}));

        KThread.currentThread().finish();
    }

    private static class MemNode {
        public int ppn;
        public MemNode next;
        public MemNode(int ppn, MemNode next){
            this.ppn = ppn; this.next = next;
        }
    }

    private static final int MAX_PPN = 1000000;

    private static Lock memLock = null;
    // linked-list of USED memory
    private static MemNode head = new MemNode(-1, null);

    // first fit
    public static int allocPPN(){
        int ret = -1;
        memLock.acquire();
        boolean found = false;
        MemNode prnt = head;
        for(MemNode cur = prnt.next; cur != null; prnt = cur, cur = cur.next){
            if(prnt.ppn+1 < cur.ppn){
                found = true; ret = prnt.ppn+1;
                prnt.next = new MemNode(ret, cur);
                break;
            }
        }
        if(!found){
            if(prnt.ppn < MAX_PPN){
                ret = prnt.ppn+1;
                prnt.next = new MemNode(ret, null);
            }
        }
        memLock.release();
        // -1 if no free page
        return ret;
    }

    // linear search
    public static boolean recyclPPN(int ppn){
        boolean ret = false;
        memLock.acquire();
        MemNode prnt = head;
        for(MemNode cur = head.next; cur != null; prnt = cur, cur = cur.next){
            if(cur.ppn == ppn){
                ret = true;
                prnt.next = cur.next;
                break;
            }
        }
        memLock.release();
        return ret;
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        super.terminate();
    }

    /**
     * Globally accessible reference to the synchronized console.
     */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}

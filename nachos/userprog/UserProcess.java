package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 * <p/>
 * <p/>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see    nachos.vm.VMProcess
 * @see    nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];

        fileDescriptors = new FileDescriptor[maxFDN];
        fileDescriptors[0] = new FileDescriptor(UserKernel.console.openForReading());
        fileDescriptors[1] = new FileDescriptor(UserKernel.console.openForWriting());

        for (int i = 0; i < numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i, i, false, false, false, false);

        if (nextPid == 0) {
            this.pid = 0;
            nextPid ++;
            processesSet.add(0);
        }
    }

    static{/*
        fdMap = new Map<String, Pair<Integer, Integer>>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean containsKey(Object key) {
                return false;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public Pair<Integer, Integer> get(Object key) {
                return null;
            }

            @Override
            public Pair<Integer, Integer> put(String key, Pair<Integer, Integer> value) {
                return null;
            }

            @Override
            public Pair<Integer, Integer> remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends Pair<Integer, Integer>> m) {

            }

            @Override
            public void clear() {

            }

            @Override
            public Set<String> keySet() {
                return null;
            }

            @Override
            public Collection<Pair<Integer, Integer>> values() {
                return null;
            }

            @Override
            public Set<Entry<String, Pair<Integer, Integer>>> entrySet() {
                return null;
            }
        }; */
    }


    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param    name    the name of the file containing the executable.
     * @param    args    the arguments to pass to the executable.
     * @return    <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        new UThread(this).setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param    vaddr    the starting virtual address of the null-terminated
     * string.
     * @param    maxLength    the maximum number of characters in the string,
     * not including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     * found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0) {
                // Lib.debug(dbgProcess, "Read virtual memory string: ");
                // Lib.debug(dbgProcess, new String(bytes, 0, length));
                return new String(bytes, 0, length);
            }
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param    vaddr    the first byte of virtual memory to read.
     * @param    data    the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param    vaddr    the first byte of virtual memory to read.
     * @param    data    the array where the data will be stored.
     * @param    offset    the first byte to write in the array.
     * @param    length    the number of bytes to transfer from virtual memory to
     * the array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                                 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        int amount = 0;

        int idx = vaddr / pageSize;
        int pageOffset = vaddr - idx * pageSize;//page offset
        int bytesInCurrentPage= Math.min(length, Processor.pageSize-pageOffset);

        while (length !=0) {
            Lib.assertTrue(pageTable[idx] != null);
            int ppn = pageTable[idx].ppn;
            if(ppn < 0 || ppn >= memory.length) return 0;

            System.arraycopy(memory, ppn*Processor.pageSize +pageOffset, data, offset, bytesInCurrentPage);
            amount += bytesInCurrentPage;
            length -= bytesInCurrentPage;
            offset += bytesInCurrentPage;
            idx ++;
            pageOffset = 0;
            bytesInCurrentPage = Math.min(length, Processor.pageSize);
        }

        return amount;

        /*
        int amount = Math.min(length, memory.length - vaddr);
        System.arraycopy(memory, vaddr, data, offset, amount);

        return amount;
        */
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param    vaddr    the first byte of virtual memory to write.
     * @param    data    the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param    vaddr    the first byte of virtual memory to write.
     * @param    data    the array containing the data to transfer.
     * @param    offset    the first byte to transfer from the array.
     * @param    length    the number of bytes to transfer from the array to
     * virtual memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                                  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        int amount = 0;

        int idx = vaddr / pageSize;
        if (pageTable[idx].readOnly) return amount;
        int pageOffset = vaddr - idx * pageSize;//page offset
        int bytesInCurrentPage= Math.min(length, Processor.pageSize - pageOffset);

        while (length !=0) {
            Lib.assertTrue(pageTable[idx] != null);
            int ppn = pageTable[idx].ppn;
            if(ppn < 0 || ppn >= memory.length) return 0;

            System.arraycopy(data, offset+amount, memory, ppn*Processor.pageSize +pageOffset, bytesInCurrentPage);
            amount += bytesInCurrentPage;
            length -= bytesInCurrentPage;
            offset += bytesInCurrentPage;
            idx ++;
            if (pageTable[idx].readOnly) return amount;
            pageOffset = 0;
            bytesInCurrentPage = Math.min(length, Processor.pageSize);
        }

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param    name    the name of the file containing the executable.
     * @param    args    the arguments to pass to the executable.
     * @return    <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        for (int i = 0; i < stackPages; i++) {
            int vpn = numPages+i;
            int newPage = UserKernel.allocPPN();
            if (newPage != -1) {
                pageTable[vpn].ppn = newPage;
                pageTable[vpn].used = true;
                pageTable[vpn].readOnly = false;
                pageTable[vpn].valid = true;

            } else {
                Lib.debug(dbgProcess, "\tinsufficient physical memory");
                return false;
            }
        }
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        int arguVpn = numPages;
        int newPage = UserKernel.allocPPN();
        if (newPage != -1) {
            pageTable[arguVpn].ppn = newPage;
            pageTable[arguVpn].used = true;
            pageTable[arguVpn].readOnly = false;
            pageTable[arguVpn].valid = true;

        } else {
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                    argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[]{0}) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return    <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;
                int newPage = UserKernel.allocPPN();
                if (newPage != -1) {
                    pageTable[vpn].ppn = newPage;
                    pageTable[vpn].used = true;
                    pageTable[vpn].readOnly = section.isReadOnly();
                    pageTable[vpn].valid = true;

                } else {
                    Lib.debug(dbgProcess, "\tinsufficient physical memory");
                    return false;
                }
                // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, pageTable[vpn].ppn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        // unload sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\treleasing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;
                UserKernel.recyclPPN(pageTable[vpn].ppn);
                pageTable[vpn].ppn = -1;
            }
        }
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    private class FileDescriptor {
        int pos;
        OpenFile openFile;
        public FileDescriptor(OpenFile openFile) {
            this.openFile = openFile;
            this.pos = 0;
        }
    }


    private int newFileDesc() {
        int i;
        for (i = 0;i < 16; i++) {
            if (fileDescriptors[i] == null)
                break;
        }
        if (i == 16) return -1;
        return i;
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
        Lib.debug(dbgProcess, "Handle halt.");
        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    private int handleOpen(int a0, boolean create) {
        Lib.debug(dbgProcess, "handleFileOpen::start");

        String name = readVirtualMemoryString(a0 ,256);
        // if (name.length() == 0) {
        //     System.out.println("Zero-lengthed file name.");
        // }

        if (name.length() == 0) {
            Lib.debug(dbgProcess, "handleFileOpen::Fail to read virtual memory.");
            return -1;
        }
        if (name.length() > 256) {
            Lib.debug(dbgProcess, "handleFileOpen::File name is too long.");
            return -1;
        }
        if (deleteList.contains(name)) {
            Lib.debug(dbgProcess, "handleFileOpen::Cannot open when there is a pending deletion");
            return -1;
        }

        int nfd = newFileDesc();
        if (nfd != -1) {
            OpenFile file = UserKernel.fileSystem.open(name, create);
            if (file == null) {
                Lib.debug(dbgProcess, "handleFileOpen::Cannot open file: fileSystem.open return null.");
                return -1;
            }

            fileDescriptors[nfd] = new FileDescriptor(file);

            if (statusMap.containsKey(name)) {
                int oldValue = statusMap.get(name);
                statusMap.put(name, ++oldValue);
            }
            else {
                statusMap.put(name, 1);
            }
        }
        return nfd;
    }


    private int handleRead(int fd, int buff, int count) {
        Lib.debug(dbgProcess, "Handle file read.");
        if (fileDescriptors[fd] == null)
            return -1;
        OpenFile fileDesc = fileDescriptors[fd].openFile;
        int pos = fileDescriptors[fd].pos;
        byte[] buffer = new byte[count];
        int rtn = fileDesc.read(pos, buffer, 0, count);
        fileDescriptors[fd].pos += rtn;
        writeVirtualMemory(buff, buffer);
        return rtn;
    }

    private int handleWrite(int fd, int buff, int count){
        Lib.debug(dbgProcess, "Handle file write.");
        if (fileDescriptors[fd] == null)
            return -1;
        OpenFile fileDesc = fileDescriptors[fd].openFile;
        int pos = fileDescriptors[fd].pos;
        byte[] buffer = new byte[count];
        readVirtualMemory(buff, buffer);
        int rtn = fileDesc.write(pos, buffer, 0, count);
        fileDescriptors[fd].pos += rtn;
        return rtn;
    }

    private int handleClose(int fd) {
        Lib.debug(dbgProcess, "Handle file close.");
        if (fd < 0 || fd >= maxFDN) {
            Lib.debug(dbgProcess, "handleClose::Invalid file descriptor");
            return -1;
        }
        FileDescriptor fileDesc = fileDescriptors[fd];

        if (fileDesc == null) {
            Lib.debug(dbgProcess, "handleClose::Null file descriptor got.");
            return -1;
        }
        fileDescriptors[fd] = null;

        String file_name = fileDesc.openFile.getName();

        // boolean flag = false;
        // for (int i = 0; i != maxFDN; i++) {
        //     if (fileDescriptors[i] != null && fileDescriptors[i].openFile.getName().equals(file_name)) {
        //         flag = true;
        //         break;
        //     }
        // }

        if (!statusMap.containsKey(file_name)) {
            Lib.debug(dbgProcess, "handleClose::statusMap Key error!");
            return -1;
        }
        int oldVal = statusMap.get(file_name);
        statusMap.put(file_name, oldVal-1);

        if (statusMap.get(file_name) == 0) {
            fileDesc.openFile.close();
            if (deleteList.contains(file_name)) {
                Lib.debug(dbgProcess, "handleClose::Execute postponed unlink operation.");
                if (ThreadedKernel.fileSystem.remove(file_name)) {
                    deleteList.remove(file_name);
                    Lib.debug(dbgProcess, "handleClose::Unlink success");
                    return 0;
                }
                else {
                    Lib.debug(dbgProcess, "handleClose::Unlink fails.");
                    return -1;
                }
            }
        }
        else if (statusMap.get(file_name) < 0) {
            Lib.debug(dbgProcess, "handleUnlink::Wrong status value: MINUS ZERO!");
            return -1;
        }

        // fileDesc.openFile.close();
        return 0;
    }

    private int handleUnlink(int vpn) {
        Lib.debug(dbgProcess, "handleUnlink::Start");
        String name = readVirtualMemoryString(vpn, 100);
        if (name == null) {
            Lib.debug(dbgProcess, "handleUnlink::Read virtual memory fails.");
            return -1;
        }
        if (name.length() > 256) {
            Lib.debug(dbgProcess, "handleUnlink::File name is too long.");
            return -1;
        }

        // boolean flag = false;
        // for (int i = 0; i < maxFDN; i++) {
        //    if (fileDescriptors[i] != null && fileDescriptors[i].openFile.getName().equals(name)) {
        //        flag = true;
        //        break;
        //    }
        //  }
        if (!statusMap.containsKey(name)) {
            Lib.debug(dbgProcess, "handleUnlink::statusMap Key error!");
        }
        if (statusMap.get(name) > 0) {
            if (!deleteList.contains(name)) {
                deleteList.add(name);
            }
            Lib.debug(dbgProcess, "handleUnlink::Postponing deletion.");
            return 0;
        }
        else {
            Lib.debug(dbgProcess, "handleUnlink::Directly Deletion.");
            if (ThreadedKernel.fileSystem.remove(name)) {
                Lib.debug(dbgProcess, "handleUnlink::Successfully deleted");
                return 0;
            }
        }
        return -1;
    }

    private int handleExit(int status) {
        Lib.debug(dbgProcess, "Handle exit.");
        lock.acquire();
        Lib.debug(dbgProcess, "Process " + this.pid + " exit with status:" + status);

        //dealloc page for arguments
        UserKernel.recyclPPN(this.pageTable[numPages-1].ppn);
        numPages --;

        //dealloc page for stack
        for (int i = 0; i < stackPages; i++) {
            UserKernel.recyclPPN(this.pageTable[numPages-1-i].ppn);
        }
        numPages -= stackPages;

        unloadSections();
        
        //deal with opened files
        for (int i = 0; i < maxFDN; i++) {
            FileDescriptor fd = fileDescriptors[i];
            if (fd != null) {
                fd.openFile.close();
            }
        }

        //remove this pid
        if (processesSet.contains(this.parentPid)) {
            Lib.debug(dbgProcess, "Parent still alive, record exit status");
            processExitStatusMap.put(this.pid, status);
        }
        processesSet.remove(this.pid);
        if (processesSet.isEmpty()) {
            handleHalt();
            lock.release();
        }
        cond.wakeAll();
        lock.release();
        KThread.finish();
        return 0;
    }

    private int handleExec(int charPointerToName, int argc, int charPointerPointerToArgv) {
        Lib.debug(dbgProcess, "Handle execute");
        String name = readVirtualMemoryString(charPointerToName, 256);
        if (argc < 0) {
            return -1;
        }
        String[] argvs = new String[argc];
        for (int i = 0; i < argc; i++) {
            byte[] address = new byte[4];
            readVirtualMemory(charPointerPointerToArgv + 4 * i, address, 0, 4);
            int virtualAddress = Lib.bytesToInt(address, 0);
            argvs[i] = readVirtualMemoryString(virtualAddress, 256);
        }
        if (nextPid == 0) {
            return -1;
        }
        lock.acquire();
        int returnPid = nextPid;
        UserProcess p = new UserProcess();
        p.pid = nextPid;
        nextPid++;
        p.parentPid = this.pid;
        this.childrenSet.add(p.pid);
        processesSet.add(p.pid);
        if (!p.execute(name, argvs)) {
            this.childrenSet.remove(p.pid);
            processesSet.remove(p.pid);
            nextPid--;
            returnPid = -1;
        }
        lock.release();
        return returnPid;
    }

    private int handleJoin(int pid, int intPointerToStatus) {
        Lib.debug(dbgProcess, "Handle join");
        if (!childrenSet.contains(pid)) {
            return -1;
        }
        lock.acquire();
        while(processesSet.contains(pid)) {
            cond.sleep();
        }
        int returnStatus = processExitStatusMap.get(pid);
        writeVirtualMemory(intPointerToStatus, Lib.bytesFromInt(returnStatus));
        processExitStatusMap.remove(pid);
        childrenSet.remove(pid);
        lock.release();
        return 1;
    }

    private static final int
            syscallHalt = 0,
            syscallExit = 1,
            syscallExec = 2,
            syscallJoin = 3,
            syscallCreate = 4,
            syscallOpen = 5,
            syscallRead = 6,
            syscallWrite = 7,
            syscallClose = 8,
            syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     * <p/>
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     * </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     * </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param    syscall    the syscall number.
     * @param    a0    the first syscall argument.
     * @param    a1    the second syscall argument.
     * @param    a2    the third syscall argument.
     * @param    a3    the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallExit:
                return handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallCreate:
                return handleOpen(a0, true);
            case syscallOpen:
                return handleOpen(a0, false);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleUnlink(a0);


            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param    cause    the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0),
                        processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2),
                        processor.readRegister(Processor.regA3)
                );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                        Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /**
     * The program being run by this process.
     */
    protected Coff coff;

    /**
     * This process's page table.
     */
    protected TranslationEntry[] pageTable;
    protected int tableSize = 0;
    /**
     * The number of contiguous pages occupied by the program.
     */
    protected int numPages;

    /**
     * The number of pages in the program's stack.
     */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    private static int maxFDN = 16;
    private FileDescriptor[] fileDescriptors;
    private static LinkedList<String> deleteList = new LinkedList<String>();
    private static HashMap<String, Integer> statusMap = new HashMap<String, Integer>();

    /**
     * Variables for task 3;
     */
    int pid;
    int parentPid;
    Set<Integer> childrenSet = new HashSet<Integer>();

    private static int nextPid = 0;
    private static Lock lock = new Lock();
    private static Condition cond = new Condition(lock);
    static Set<Integer> processesSet = new HashSet<Integer>();
    static Map<Integer, Integer> processExitStatusMap = new HashMap<Integer, Integer>();
}

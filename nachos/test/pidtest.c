//
// Created by Hubert Zhang on 15/5/5.
//

#include "syscall.h"

int main(int argc, char const *argv[])
{
    printf("pid test start\n");
    unsigned int current_pid;
    unsigned int last_pid;
    int return_state;
    while (1) {
        int current_pid = exec("halt.coff", 0, 0);
        if (current_pid < 4000) {
            join(current_pid, &return_state);
            if (current_pid % 1000 == 0)
                printf("Pid %u finished\n", current_pid);
            last_pid = current_pid;
        } else {
            printf("The last pid is %u\n", last_pid);
            halt();
            printf("This line should not be printed\n", last_pid);
            break;
        }
    }

    return 0;
}

#include "stdio.h"
#include "syscall.h"

int main() {
    int fds[15];
    for (int i = 0; i != 15; i++) {
        char file_name = 'a' + i;
        fds[i] = creat(&file_name);
        printf("File %d, Descriptor %d\n", i, fds[i]);
    }
    for (int i = 0; i != 15; i++) {
        if (fds[i] != -1) {
            close(fds[i]);
            char file_name = 'a' + 1;
            unlink(&file_name);
        }
    }
    return 0;
}
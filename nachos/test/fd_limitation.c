#include "stdio.h"
#include "syscall.h"

int main() {
    int fds[15];
    int i = 0;
    char file_name;
    for (i = 0; i != 15; i++) {
        file_name = 'a' + i;
        fds[i] = creat(&file_name);
        printf("File %d, Descriptor %d\n", i, fds[i]);
    }
    for (i = 0; i != 15; i++) {
        if (fds[i] != -1) {
            close(fds[i]);
            file_name = 'a' + i;
            unlink(&file_name);
        }
    }
    return 0;
}

#include "stdio.h"
#include "syscall.h"

int main() {
    char file_name[10] = "hello";
    int fd = open(file_name);

    char buffer[10] = "000000000";
    read(fd, buffer, 10);

    for (int i = 0; i != 10; i++) {
        printf(buffer[i]);
    }

    unlink(file_name);
    close(fd);
    return 0;
}
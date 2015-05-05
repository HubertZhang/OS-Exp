#include "stdio.h"
#include "syscall.h"

int main() {
    char file_name[10] = "hello";
    int fd = open(file_name);

    char buffer[12] = "00000000000";
    int rtn = read(fd, buffer, 10);
    printf("Read return: %d\n", rtn);

    int i = 0;
    printf("Read content:");
    for (i = 0; i != 10; i++) {
        printf("%c", buffer[i]);
    }
    printf("\n");

    unlink(file_name);
    close(fd);
    return 0;
}

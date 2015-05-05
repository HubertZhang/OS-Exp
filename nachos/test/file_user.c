#include "stdio.h"
#include "syscall.h"

int main() {
    char file_name[10] = "test_file"
    int fd_1 = creat(file_name);
    int rtn = unlink(file_name);
    printf("Unlink return %d\n", rtn);

    int fd_2 = open(file_name);
    printf("Open return %d\n", fd_2);

    close(fd_1);
    return 0;
}
#include "stdio.h"
#include "syscall.h"

int main() {
    char file_name[10] = "hello";
    int fd = creat(file_name);
    printf(file_name);
    printf("File: %d\n", fd);
    return 0;
}

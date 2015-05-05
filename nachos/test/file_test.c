#include "stdio.h"
#include "syscall.h"

int main() {
    int fd = creat("hello");
    printf("File: %d", fd);
    return 0;
}

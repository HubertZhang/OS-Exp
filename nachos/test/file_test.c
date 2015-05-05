#include "stdio.h"
#include "syscall.h"

int main() {
    int fd = create("hello");
    printf("File: %d", fd);
    return 0;
}

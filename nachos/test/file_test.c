#include "stdio.h"
#include "syscall.h"

int main() {
    char file_name[10] = "hello";
    int fd = creat(file_name);

    char file_content[10] = "Contents"
    write(fd, file_content, 8);

    close(fd);

    return 0;
}

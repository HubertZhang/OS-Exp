//
// Created by Hubert Zhang on 15/5/5.
//
#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"
int main() {
    char c[2];
    printf("Type 0 or 1 or 2\n");

    c[0] = getch();
    c[1] = '\0';
    int cint = atoi(c);

    switch (cint) {
        case 0:
            printf("Raising read-only exception.\n");
            int * a0 = 0;
            a0[5] = 2;
            return 0;
        case 1:
            printf("Raising bad address exception.\n");
            int * a1 = 1;
            if (*a1 == 0) {
                printf("ooo\n");
            }
        case 2:
            printf("Raising overflow exception.\n");
            int b = 0;
            int c = 1;
            return c/b;
        case 3:
            printf("Raising bus error exception.\n");
            int * a3 = 65532;
            if (*a3 == 0) {
                printf("ooo\n");
            }
        default:
            printf("No exception raised\n");
    }
    return 0;
}

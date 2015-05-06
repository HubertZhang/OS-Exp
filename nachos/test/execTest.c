#include "syscall.h"

int main(int argc, char const *argv[])
{
	printf("start\n");
	int a1 = exec("execTest.coff", 0, 0);
	int a2 = exec("execTest.coff", 0, 0);
	if (a1 != -1) {
		printf("waiting for %d", a1);
		join(a1, &a1);
	}
	if (a2 != -1) {
		printf("waiting for %d\n", a2);
		join(a2, &a2);
	}
	return 0;
}
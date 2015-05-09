#include "defs.qh"

int arr[1];

void main() {
    int TEST = 2;
    int idx = 0;
    int i;
	arr[idx] = i = TEST;
	print(ftos(i));
	print(ftos(arr[idx]));
	assert(arr[idx] == i, "Array and assignment don't match");
	assert(i == TEST, "Assignment doesn't match");
	assert(arr[idx] == TEST, "Array doesn't match");
}

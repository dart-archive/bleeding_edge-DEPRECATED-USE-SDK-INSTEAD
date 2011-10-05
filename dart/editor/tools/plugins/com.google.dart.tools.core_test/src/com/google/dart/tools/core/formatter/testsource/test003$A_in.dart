class X {
void x() {
label: do {
continue label;
} while (false);
}
void y() {
label: do {
break label;
} while (true);
}
int x;
int y;
int z;
int w;
double sum(double a, double b) {
	return a+b+a+b+a+b;
}
}
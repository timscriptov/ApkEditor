#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv) {
  if (argc != 3) {
    fprintf(stderr, "Invalid parameters.");
    return -1;
  }

  FILE *src = fopen(argv[1], "r");
  if (src == NULL) {
    fprintf(stderr, "Cannot open source file: %s", argv[1]);
    return -1;
  }

  FILE *dst = fopen(argv[2], "w");
  if (dst == NULL) {
    fprintf(stderr, "Cannot open target file: %s", argv[2]);
    fclose(src);
    return -1;
  }

  int ret;
  char *buf = (char *)malloc(4096);
  while ((ret = fread(buf, 1, 4096, src)) > 0) {
    fwrite(buf, 1, ret, dst);
  }

  fclose(dst);
  fclose(src);

  return 0;
}

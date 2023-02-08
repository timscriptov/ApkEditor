#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <string>
#include <vector>

using namespace std;

// xor password
static char str_xor_val[8];

void print_str_decrypt() {
  printf("\nstatic void str_decrypt(char *buf, int len) {\n");
  printf("  int i;\n");
  printf("  for (i = 0; i < len; ++i) {\n");
  printf("    buf[i] ^= %s;\n", str_xor_val);
  printf("  }\n");
  printf("}\n");
}

int main(int argc, char **argv) {
  vector<string> names;
  vector<int> strLens;

  srand(time(NULL));

  // Generate xor password
  int val = rand() % 128;
  while (val < 20) {
    val = rand() % 128;
  }
  sprintf(str_xor_val, "0x%02x", val);

  // Generate .h or .cpp
  bool generate_cpp = false;
  if (argc > 1) {
    generate_cpp = true;
  }

  int maxPadSize = 40;
  int padded = 0;
  FILE *fp = fopen("protect_rawstr.h", "r");
  char buf[2048];
  while (fgets(buf, sizeof(buf), fp)) {
    char *starPos = strchr(buf, '*');
    if (starPos == NULL) {
      continue;
    }
    char *equalPos = strchr(buf, '=');
    if (equalPos == NULL) {
      continue;
    }
    char *startQuoPos = strchr(buf, '\"');
    char *endQuoPos = strrchr(buf, '\"');

    *(equalPos - 1) = '\0';
    string name(starPos + 1);
    names.push_back(name);

    if (generate_cpp) {
      printf("char %s[] = {", starPos + 1);
      strLens.push_back(endQuoPos - startQuoPos - 1);
      for (startQuoPos += 1; startQuoPos < endQuoPos; startQuoPos++) {
        printf("'%c'^%s, ", *startQuoPos, str_xor_val);
      }
      printf("0, ");
      int padSize = rand() % 10;
      if (padded < maxPadSize) {
        int i = 0;
        if (padded + padSize > maxPadSize) {
          padSize = maxPadSize - padded;
        }
        for (; i < padSize; ++i) {
          printf("0x%02x, ", rand() % 128);
	}
        padded += padSize;
      }
      printf("};\n");
    }
    // Gererate definition in .h
    else {
      printf("extern char %s[];\n", starPos + 1);
    }
  }
  fclose(fp);
  fprintf(stderr, "padded=%d\n", padded);

  if (generate_cpp) {
    // utility function
    print_str_decrypt();

    // function
    printf("\nvoid protect_str_decrypt() {\n");
    for (int i = 0; i < names.size(); ++i) {
      printf("  str_decrypt(%s, %d);\n", names[i].c_str(), strLens[i]);
    }
    printf("}\n");
  }

  return 0;
}


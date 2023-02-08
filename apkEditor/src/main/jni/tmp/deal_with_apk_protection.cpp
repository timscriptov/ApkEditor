#include <cstdio>
#include <cstdlib>

static void copyContent(FILE *inFile, FILE *outFile, int len) {
  char *buf = new char[len];
  int ret = fread(buf, 1, len, inFile);
  if (ret < len) {
    printf("Copy Error: cannot read %d bytes from input file, ret=%d\n", len, ret);
    exit(-1);
  }
  fwrite(buf, 1, len, outFile);
  //printf("Copied %d bytes\n", len);
}

static void writeToFile(FILE *outFile, const char *buf, int len) {
  if (outFile != NULL) {
    fwrite(buf, 1, len, outFile);
  }
}
static void writeToFile(FILE *outFile, unsigned char *buf, int len) {
  if (outFile != NULL) {
    fwrite(buf, 1, len, outFile);
  }
}

// Play tricks: make sure (minutes + return_value) % 29 == 0
static int getTrickValue(int minutes) {
  for (int i = 0; i < 60; ++i) {
    if ((minutes + i) % 29 == 0) {
      return i;
    }
  }
  return 0;
}

void dealWithLocalFileHeader(FILE *fp, FILE *outFile) {
  unsigned char buf[26];
  char name[512];

  // Write the signature
  writeToFile(outFile, "\x50\x4b\x03\x04", 4);

  // To read header
  int offset = (int)ftell(fp);
  int ret = fread(buf, 1, sizeof(buf), fp);
  if (ret != sizeof(buf)) {
    printf("Cannot read local file header.\n");
    exit(-1);
  }

  int flags = (buf[3] << 8) | buf[2];
  int timeVal = (buf[7] << 8) | buf[6];
  int compressedSize = (buf[17] << 24) | (buf[16] << 16) | (buf[15] << 8) | buf[14];
  int filenameLen = (buf[23] << 8) | buf[22];
  int extraLen = (buf[25] << 8) | buf[24];

  int hours = timeVal >> 11;
  int minutes = (timeVal >> 5) & 63;
  int seconds = (timeVal & 31) * 2;
  printf("%04x, %d:%d:%d\n", timeVal, hours, minutes, seconds);

  // Revise the time to satisfy some condition
  seconds = getTrickValue(minutes);
  timeVal = (timeVal & 0xffe0) | seconds;
  buf[6] = timeVal & 0xff;
  buf[7] = (timeVal >> 8) &0xff;

  // To write header
  writeToFile(outFile, buf, ret);

  // To read file name
  if (sizeof(name) - 1 < filenameLen) {
    printf("File name length is too big: %d\n", filenameLen);
    exit(-1);
  }
  ret = fread(name, 1, filenameLen, fp);
  if (ret < filenameLen) {
    printf("Cannot read filename.\n");
    exit(-1);
  }

  // To write file name
  writeToFile(outFile, name, ret);

  // Print the file name and others
  name[ret] = '\0';
  //printf("%s:\n", name);
  //printf("\toffset=%d, compressed size=%d, filename len=%d, extra len=%d, data descriptor=%d\n", 
  //         offset, compressedSize, filenameLen, extraLen, flags & 0x08);
  
  // To read extra field
  if (extraLen > 0) {
    char *extraBuf = new char[extraLen];
    ret = fread(extraBuf, 1, extraLen, fp);
    if (ret < extraLen) {
      printf("Cannot read extra field.\n");
      exit(-1);
    }
    // Write to file
    writeToFile(outFile, extraBuf, ret);
    delete[] extraBuf;
  }

  if (outFile == NULL) {
    if (flags & 0x08) {
      fseek(fp, compressedSize + 16, SEEK_CUR);
    } else {
      fseek(fp, compressedSize, SEEK_CUR);
    }
  } else {
    int len = compressedSize;
    if (flags & 0x08) { len += 16; }
    copyContent(fp, outFile, len);
  }
}

void dealWithCentralDirFileHeader(FILE *fp, FILE *outFile) {
  unsigned char buf[42];
  char name[512];

  // Write the signature
  writeToFile(outFile, "\x50\x4b\x01\x02", 4);

  // To read header
  int ret = fread(buf, 1, sizeof(buf), fp);
  if (ret != sizeof(buf)) {
    printf("Cannot read Central directory file header.\n");
    exit(-1);
  }


  int timeVal = (buf[9] << 8) | buf[8];
  int filenameLen = (buf[25] << 8) | buf[24];
  int extraLen = (buf[27] << 8) | buf[26];
  int commentLen = (buf[29] << 8) | buf[28];

  int hours = timeVal >> 11;
  int minutes = (timeVal >> 5) & 63;
  int seconds = (timeVal & 31) * 2;

  // Revise the time to satisfy some condition
  seconds = getTrickValue(minutes);
  timeVal = (timeVal & 0xffe0) | seconds;
  buf[8] = timeVal & 0xff;
  buf[9] = (timeVal >> 8) &0xff;

  // Write header
  writeToFile(outFile, buf, ret);

  // To read file name
  if (sizeof(name) - 1 < filenameLen) {
    printf("File name length is too big: %d\n", filenameLen);
    exit(-1);
  }
  ret = fread(name, 1, filenameLen, fp);
  if (ret < filenameLen) {
    printf("Cannot read filename.\n");
    exit(-1);
  }

  name[ret] = '\0';
  //printf("In directory file header, %s:\n", name);

  // To write file name
  writeToFile(outFile, name, filenameLen);

  int remainLen = extraLen + commentLen;
  if (remainLen > 0) {
    if (outFile == NULL) {
      fseek(fp, remainLen, SEEK_CUR);
    } else {
      copyContent(fp, outFile, remainLen);
    }
  }
}

// append "wb" in the end
void dealWithEndDirRecord(FILE *fp, FILE *outFile) {
  unsigned char buf[18];

  // Write the signature
  writeToFile(outFile, "\x50\x4b\x05\x06", 4);

  int offset = (int)ftell(fp);
  int ret = fread(buf, 1, sizeof(buf), fp);
  if (ret != sizeof(buf)) {
    printf("Cannot read End of central directory record.\n");
    exit(-1);
  }

  // Write header
  int commentLen = (buf[17] << 8) | buf[16];
  int revisedLen = commentLen + 2;
  buf[17] = (revisedLen >> 8) & 0xff;
  buf[16] = revisedLen & 0xff;

  writeToFile(outFile, buf, ret);

  // Copy comment
  if (commentLen > 0) {
    copyContent(fp, outFile, commentLen);
  }

  // Append last 2 bytes
  if (outFile != NULL) {
    fwrite("wb", 1, 2, outFile);
  }

  int totalEntry = (buf[7] << 8) | buf[6];
  printf("End of central directory record, #total entry=%d, comment len=%d\n", 
         totalEntry, commentLen);
}

bool isInterestedSignature(int sig) {
  return sig == 0x04034b50 ||
         sig == 0x02014b50 ||
         sig == 0x06054b50;
}

int main(int argc, char **argv) {
  if (argc < 2) {
    printf("Usage: %s in_file [out_file]\n", argv[0]);
    return -1;
  }

  FILE *fp = fopen(argv[1], "rb+");
  if (fp == NULL) {
    printf("Error: Cannot open %s\n", argv[1]);
    return -1;
  }

  FILE *outFile = NULL;
  if (argc >= 3) {
    outFile = fopen(argv[2], "wb");
    if (outFile == NULL) {
      printf("Error: Cannot open %s\n", argv[2]);
      return -1;
    }
  }

/*
  unsigned char buf[2] = { 0x10, 0x10 };
  fseek(fp, 0, SEEK_END);
  fseek(fp, 49, SEEK_SET);
  fwrite(buf, 1, 2, fp);
  */

  unsigned int signature = 0;
  unsigned char buf[4];

  int ret = fread(buf, 1, sizeof(buf), fp);

  while (ret == sizeof(buf)) {

    signature = (buf[3] << 24) | (buf[2] << 16) | (buf[1] << 8) | buf[0];

    int skippedBytes = 0;
    while (!isInterestedSignature(signature)) {
      // read another byte
      ret = fread(buf, 1, 1, fp);
      if (ret != 1) {
        break;
      }
      skippedBytes += 1;
      signature = (signature >> 8) | (buf[0] << 24);
    }
    if (skippedBytes > 0) {
      printf("skipped %d bytes.\n", skippedBytes);
      char *tmpBuf = new char[skippedBytes];
      writeToFile(outFile, tmpBuf, skippedBytes);
      delete[] tmpBuf;
    }

    // Local file header
    switch (signature) {
      case 0x04034b50:
        dealWithLocalFileHeader(fp, outFile);
        break;
      case 0x02014b50:
        dealWithCentralDirFileHeader(fp, outFile);
        break;
      case 0x06054b50:
        dealWithEndDirRecord(fp, outFile);
        break;
      default:
        printf("Unknown signature: 0x%08x\n", signature);
        //exit(-1);
    }

    ret = fread(buf, 1, sizeof(buf), fp);
  }

  fclose(fp);
  if (outFile != NULL) { fclose(outFile); }

  return 0;
}

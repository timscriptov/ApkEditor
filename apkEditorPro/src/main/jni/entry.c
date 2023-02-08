#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <sys/ptrace.h>
#include "util.h"

#include "libzip/config.h"

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifndef HAVE_GETOPT
#include "getopt.h"
#endif

#include "libzip/zip.h"
#include "libzip/compat.h"

// Apk path of current apk
static char g_apkpath[256];
static char g_datadir[256];
int dex_size;
int manifest_size;
int lib_size;
int g_sdkInt = 0;
int g_verifyCert;

static void checkFileSize()
{
    /*
    zip_t *za;
    zip_stat_t sb;
    int err;
    int ret;
    char name1[] = {'c'^0x55,'l'^0x55,'a'^0x55,'s'^0x55,'s'^0x55,'e'^0x55,'s'^0x55,'.'^0x55,'d'^0x55,'e'^0x55,'x'^0x55,'\0'};
    char name2[] = {'A'^0x55,'n'^0x55,'d'^0x55,'r'^0x55,'o'^0x55,'i'^0x55,'d'^0x55,'M'^0x55,'a'^0x55,'n'^0x55,'i'^0x55,'f'^0x55,'e'^0x55,'s'^0x55,'t'^0x55,'.'^0x55,'x'^0x55,'m'^0x55,'l'^0x55,'\0'};
    char name3[] = {'l'^0x55,'i'^0x55,'b'^0x55,'/'^0x55,'a'^0x55,'r'^0x55,'m'^0x55,'e'^0x55,'a'^0x55,'b'^0x55,'i'^0x55,'/'^0x55,'l'^0x55,'i'^0x55,'b'^0x55,'s'^0x55,'y'^0x55,'s'^0x55,'c'^0x55,'h'^0x55,'e'^0x55,'c'^0x55,'k'^0x55,'.'^0x55,'s'^0x55,'o'^0x55,'\0'};
    int i;

    FILE *fp = fopen(g_apkpath, "r");
    if (fp != NULL) {
        fseek(fp, 0, SEEK_END);
        LOGD("File size: %d", ftell(fp));
        if (ftell(fp) < 4000000) {
            raise(3);
        }
        fclose(fp);
    } else {
        exit(-1);
    }

    if ((za=zip_open(g_apkpath, ZIP_RDONLY, &err)) == NULL) {
        zip_error_t error;
        zip_error_init_with_code(&error, err);
        LOGE("Can't open '%s': %s\n", g_apkpath, zip_error_strerror(&error));
        zip_error_fini(&error);
        ret = 0;
    }
    for (i = 0; i < sizeof(name1) - 1; ++i) {
        name1[i] ^= 0x55;
    }
    zip_stat(za, name1, 0, &sb);
    dex_size = (int)sb.size;
    LOGD("dex_size=%d", dex_size);

    // Open "classes.dex" and check the string at offset
    int target_offset = 0;
    if (dex_size == correct_dex_size[0]) {
        target_offset = correct_dex_size[2];
    } else if (dex_size == correct_dex_size[1]) {
        target_offset = correct_dex_size[3];
    }
    struct zip_file *zf = zip_fopen(za, name1, 0);
    char *buf = (char *)malloc(4096);
    int read_len = 0;
    while (read_len < target_offset) {
        int remain = target_offset - read_len;
        int len = zip_fread(zf, buf, (remain < 4096 ? remain : 4096));
        LOGD("read %d bytes", len);
        if (len <= 0) { // Error or no data
            break;
        }
        read_len += len;
    }
    LOGD("Out of while loop");
    if (read_len == target_offset) {
        zip_fread(zf, buf, 4096);
    }
    zip_fclose(zf);
    LOGD("%c%c%c%c%c%c", buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]);

    // AndroidManifest.xml
    for (i = 0; i < sizeof(name2) - 1; ++i) {
        name2[i] ^= 0x55;
    }
    zip_stat(za, name2, 0, &sb);
    manifest_size = (int)sb.size;
    LOGD("manifest_size=%d", manifest_size);

    // lib/armeabi/libsyscheck.so
    for (i = 0; i < sizeof(name3) - 1; ++i) {
        name3[i] ^= 0x55;
    }
    zip_stat(za, name3, 0, &sb);
    lib_size = (int)sb.size;
    LOGD("lib_size=%d", lib_size);

    zip_close(za);

#if defined(RELEASE)
    for (i = 0; i < 2; ++i) {
        if (dex_size == correct_dex_size[i])  break;
    }
    if (i >= 2) {
        LOGD("exit at 1");
        exit(-1);
    }
    for (i = 0; i < 2; ++i) {
        if (manifest_size == correct_manifest_size[i])  break;
    }
    if (i >= 2) {
        LOGD("exit at 2");
        exit(-1);
    }
    if (lib_size != correct_lib_size[0]) {
        LOGD("exit at 3");
        exit(-1);
    }

    // Check the string at the offset, must be "ifbhpp"
    if (buf[0] != 'i' || buf[1] != 'f' || buf[2] != 'b' ||
        buf[3] != 'h' || buf[4] != 'p' || buf[5] != 'p' ||
        buf[6] != '\0') {
        LOGD("exit at 4");
        exit(-1);
    }
#endif
*/
   // free(buf);
}

// Get the apk path of current app
/*
static void getapkfilepath(char *filepath, int len)
{
    char cmd[] = {'/'^0x55,'p'^0x55,'r'^0x55,'o'^0x55,'c'^0x55,'/'^0x55,'s'^0x55,'e'^0x55,'l'^0x55,'f'^0x55,'/'^0x55,'m'^0x55,'a'^0x55,'p'^0x55,'s'^0x55, '\0'};
    char buf[512] = {0};
    char path[512] = {0};
    int best_score = 0;
    int i;
    
    for (i = 0; i < sizeof(cmd) - 1; ++i) {
        cmd[i] = cmd[i] ^ 0x55;
    }

    buf[sizeof(buf) - 1] = '\0';

    FILE *fp = fopen(cmd, "r");
    if (fp != NULL) {
        while (fgets(buf, sizeof(buf) - 1, fp) != NULL) {
            char *start = strchr(buf, '/');
            if (start != NULL) {
                int score = 0;

                // end with .apk
                char *pos = strstr(start, ".apk");
                if (pos != NULL && pos[4] == '\0') {
                    score += 50;
                }
                // contain package name
                pos = strstr(start, "/com.gmail.heagoo.apkeditor");
                if (pos != NULL) {
                    score += 50;
                }
                // contain "/base."
                pos = strstr(start, "/base.");
                if (pos != NULL) {
                    score += 50;
                }

                if (score >= best_score) {
                    strncpy(path, start, sizeof(path));
                    best_score = score;
                }
            }
        } // end while
        fclose(fp);

        // Remove the ended '\r' '\n'
        for (i = 0; i < sizeof(path); ++i) {
            if (path[i] == '\r' || path[i] == '\n') {
                path[i] = '\0';
                break;
            }
        }

        // Copy the result
        strncpy(filepath, path, len);
    }
}
*/

static void extract_hidden_code(const char *target)
{
    zip_t *za;
    zip_file_t *zf;
    zip_int64_t n, i;
    char buf[2048];
    int err;
    FILE *fp;
    int total = 0;
    char entry_name[] = {'r'^0x55,'/'^0x55,'a'^0x55,'/'^0x55,'e'^0x55,'x'^0x55,'a'^0x55,'m'^0x55,'p'^0x55,'l'^0x55,'e'^0x55,'1'^0x55,'_'^0x55,'s'^0x55,'t'^0x55,'e'^0x55,'p'^0x55,'2'^0x55,'.'^0x55,'j'^0x55,'p'^0x55,'g'^0x55,'\0'};
    int k;
#define FILE_OFFSET 12224

    for (k = 0; k < sizeof(entry_name) - 1; ++k) {
        entry_name[k] ^= 0x55;
    }

    LOGD("filepath: %s", g_apkpath);

    if ((za=zip_open(g_apkpath, ZIP_RDONLY, &err)) == NULL) {
        zip_error_t error;
        zip_error_init_with_code(&error, err);
        LOGE("Can't open '%s': %s\n", g_apkpath, zip_error_strerror(&error));
        zip_error_fini(&error);
        return;
    }

    if ((zf=zip_fopen(za, entry_name, 0)) == NULL) {
        LOGE("ERROR: %s", zip_strerror(za));
        return;
    }

    if (zf != NULL) {
        fp = fopen(target, "w");
        if (fp != NULL) {
            while ((n=zip_fread(zf, buf, sizeof(buf))) > 0) {
                if (total >= FILE_OFFSET) { // all after the offset
                    fwrite(buf, (size_t)n, 1, fp);
                } else if (total + n > FILE_OFFSET) { // part after the offset
                    fwrite(buf+(FILE_OFFSET-total), (size_t)(total+n-FILE_OFFSET), 1, fp);
                }
                total += n;
            }
            fclose(fp);
        }
        zip_fclose(zf);
    }

    zip_close(za);
}

// Called in MainActivity::init
__attribute__((section (".mtext"))) void a(JNIEnv* env, jobject thiz, jobject ctx, jstring name, jstring data_path, jstring _apkpath)
{/*
    zip_t *za;
    int err;
    const char *pkgname = (*env)->GetStringUTFChars(env,name,NULL);
    const char *datadir = (*env)->GetStringUTFChars(env,data_path,NULL);
    const char *apkpath = (*env)->GetStringUTFChars(env,_apkpath,NULL);
    char name1[] = {'c'^0x55,'o'^0x55,'m'^0x55,'.'^0x55,'g'^0x55,'m'^0x55,'a'^0x55,'i'^0x55,'l'^0x55,'.'^0x55,'h'^0x55,'e'^0x55,'a'^0x55,'g'^0x55,'o'^0x55,'o'^0x55,'.'^0x55,'a'^0x55,'p'^0x55,'k'^0x55,'e'^0x55,'d'^0x55,'i'^0x55,'t'^0x55,'o'^0x55,'r'^0x55,'\0'};
    //char filepath[] = {'/'^0x55,'p'^0x55,'r'^0x55,'o'^0x55,'c'^0x55,'/'^0x55,'s'^0x55,'e'^0x55,'l'^0x55,'f'^0x55,'/'^0x55,'c'^0x55,'m'^0x55,'d'^0x55,'l'^0x55,'i'^0x55,'n'^0x55,'e'^0x55, '\0'};
    FILE *fp;
    int i;

    LOGD("pkgname: %s", pkgname);
    LOGD("datadir: %s", datadir);
    LOGD("apkpath: %s", apkpath);

    strncpy(g_apkpath, apkpath, sizeof(g_apkpath));
    strncpy(g_datadir, datadir, sizeof(g_datadir));

    for (i = 0; i < sizeof(name1) - 1; ++i) {
        name1[i] = name1[i] ^ 0x55;
    }
    //for (i = 0; i < sizeof(filepath) - 1; ++i) {
    //    filepath[i] = filepath[i] ^ 0x55;
    //}

    // Check /proc/self/cmdline, compare the package name from it
    /*
    fp = fopen(filepath, "r");
    if (fp != NULL) {
        char buf[44];
        int read = 0;
        while (read < 44) {
            int ret = fread(buf+read, 1, 44-read, fp);
            if (ret > 0) { read += ret; }
            else break;
        }
        fclose(fp);

        for (i = 0; i < 44; ++i) {
            if (buf[i] >= 'a' && buf[i] <= 'z') {
                continue;
            }
            if (buf[i] == '.') {
                continue;
            }
            buf[i] == '\0';
            break;
        }
        if (strcmp(pkgname, buf) != 0) {
            exit(-1);
        }
    }
    */
/*
    // Check the package name
    if (memcmp(pkgname, name1, sizeof(name1) - 1) == 0) {
        int len = sizeof(name1) - 1;
        if (pkgname[len] == '\0') { // free version
        } else if (pkgname[len] == '.' &&
                   pkgname[len+1] == 'p' && pkgname[len+2] == 'r' && 
                   pkgname[len+3] == 'o' && pkgname[len+4] == '\0') {
        } else if (pkgname[len] == '.' && pkgname[len+1] == 'p' &&
                   pkgname[len+2] == 'a' && pkgname[len+3] == 'r' &&
                   pkgname[len+4] == 's' && pkgname[len+5] == 'e' &&
                   pkgname[len+6] == 'r' && pkgname[len+7] == '\0') {
        } else {
            exit(-1);
        }
    } else {
        exit(-1);
    }


    // Extract the hidden dex
    {
    int len = strlen(datadir) + 10;
    char *target = (char *)malloc(len);
    strncpy(target, datadir, len);
    strncat(target, "/part", len);
    extract_hidden_code(target);
    free(target);
    }

    extern __attribute__((section (".mtext"))) jint verify(JNIEnv *env, jobject ctx, int seed);
    g_verifyCert = verify(env, ctx, 0);
    LOGD("g_vc=%d", g_verifyCert);
    */
}

extern __attribute__((section (".mtext"))) jint check_isX86(JNIEnv* env, jobject thiz);
extern __attribute__((section (".mtext"))) void m(JNIEnv* env, jobject thiz, jstring _tname, jstring _sname,
                                           jstring _replace, int len2, jstring _mapping, int len1);
__attribute__((section (".mtext")))
void zip_modify(JNIEnv* env, jobject thiz, jstring _tname, jstring _sname,
                jstring _added, int len1, jstring _removed, int len2, jstring _replaced, int len3);
extern __attribute__((section (".mtext"))) jint verifyCertificate(JNIEnv *env, jobject thiz, jobject ctx, int seed);

#if defined(APK_BUILDER)
jint
jni_link( JNIEnv* env,
          jobject thiz, jstring _oldpath, jstring _newpath )
{
    const char *oldpath = (*env)->GetStringUTFChars(env, _oldpath, NULL);
    const char *newpath = (*env)->GetStringUTFChars(env, _newpath, NULL);
    int ret = symlink(oldpath, newpath);
    (*env)->ReleaseStringUTFChars(env, _oldpath, oldpath);
    (*env)->ReleaseStringUTFChars(env, _newpath, newpath);
    return ret;
}
#endif

/**
* Table of methods associated with a single class.
*/
static JNINativeMethod gMethods[] = {
//绑定，注意，V,Z签名的返回值不能有分号“;”
//这里就是把JAVA层的check()函数绑定到Native层的a（）函数，就无需使用原生的Java_com_xx_xx_classname_methodname这种恶心的函数命名方式了
#if !defined(APK_BUILDER)
    { "vc", "(Ljava/lang/Object;I)I", (void*)verifyCertificate },
    { "it", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)a },
    { "isX86", "()I", (void*)check_isX86 },
    // Merge
    { "mg", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)V", (void*)m },
#endif
    // Zip modifiy
    { "md", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;ILjava/lang/String;I)V", (void*)zip_modify },
#if defined(APK_BUILDER)
    { "link", "(Ljava/lang/String;Ljava/lang/String;)I", (void*)jni_link },
#endif
};

static int registerNativeMethods(JNIEnv* env, const char* className,
            JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;
    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int registerNatives(JNIEnv* env)
{
    char name[40];
    name[0] = 'c';
    name[1] = 'o';
    name[2] = 'm';
    name[3] = '/';
    name[4] = 'g';
    name[5] = 'm';
    name[6] = 'a';
    name[7] = 'i';
    name[8] = 'l';
    name[9] = '/';
    name[10] = 'h';
    name[11] = 'e';
    name[12] = 'a';
    name[13] = 'g';
    name[14] = 'o';
    name[15] = 'o';
    name[16] = '/';
    name[17] = 'a';
    name[18] = 'p';
    name[19] = 'k';
    name[20] = 'e';
    name[21] = 'd';
    name[22] = 'i';
    name[23] = 't';
    name[24] = 'o';
    name[25] = 'r';
    name[26] = '/';
    name[27] = 'M';
    name[28] = 'a';
    name[29] = 'i';
    name[30] = 'n';
    name[31] = 'A';
    name[32] = 'c';
    name[33] = 't';
    name[34] = 'i';
    name[35] = 'v';
    name[36] = 'i';
    name[37] = 't';
    name[38] = 'y';
    name[39] = '\0';

    ptrace(PTRACE_TRACEME, 0, 0, 0);
    if (!registerNativeMethods(env,
#if defined(APK_BUILDER)
                               "com/gmail/heagoo/builder/API",
#else
                               name,
#endif
                               gMethods,
                               sizeof(gMethods) / sizeof(gMethods[0]))) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
void *thread_func(void *param)
{
    sleep(5);

#if defined(CHECK_FILESIZE)
    // Check file size
    checkFileSize();
#endif

#if defined(CHECK_SIGNATURE)
    LOGD("vc=%d", g_verifyCert);
    if (g_verifyCert != 0x55555555) {
        abort();
    }
#endif
}
*/

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    pthread_t thread;
    jint result = -1;
    bool success;

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    if (!registerNatives(env)) {
        return -1;
    }

    // Get SDK_INT
    do {
        jclass versionClass = (*env)->FindClass(env, "android/os/Build$VERSION");
        success = (versionClass != NULL);
        if (!success) break;

        jfieldID sdkIntFieldID = NULL;
        success = (NULL != (sdkIntFieldID = (*env)->GetStaticFieldID(env, versionClass, "SDK_INT", "I")));
        if (!success) break;

        if (success)
        {
            g_sdkInt = (*env)->GetStaticIntField(env, versionClass, sdkIntFieldID);
            LOGD("sdkInt = %d", g_sdkInt);
        }
    } while (false);
    //Вот здесть в отдельном потоке страрутет проверка
    //pthread_create(&thread, NULL, thread_func, NULL);

    result = JNI_VERSION_1_4;

    return result;
}


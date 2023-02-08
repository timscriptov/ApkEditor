#include <jni.h>
#include <android/log.h>
#include <string.h>
#include "util.h"
#include "protect_str.h"

#define ERROR -1

/*
char *rsa = "a28fde355ba0ce91870e308791f2c1e2f90bd1c8779c5659007cf9ed981bcf5ac97f943896391a9476c4c49ec42acbdee396a1789683977305347fcc7dda403bdb1732b6fb235eeb872d8358838b5d4917d1fa1c198814777328fd6eeb08a395102caa0319aca0680e1eab07d44ac3dea962e053525a5795296c2266fe1c2caa3e2c5afa20ba56d1f5257968aa4699fc3d0f232fba805bc63d34f64ca46db0df6a70a10cdf93fd2904a168ea4c7af84c1fa77ef5742b3377cb69fbc1998a3de5569158fb365febd74f3e258f986959905d578a684e6c711b936ac57334ddfa859dd88c50a759ffccd2adc98a577be427d69da83bcc29ecaf9ae8dba6edc983d7";
char *str_getPkgMgr = "getPackageManager";
char *str_pkgMgrPath = "()Landroid/content/pm/PackageManager;";
char *str_getPkgName = "getPackageName";
char *str_stringPath = "()Ljava/lang/String;";
char *str_getPkgInfo = "getPackageInfo";
char *str_funcProtoType1 = "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;";
char *str_signature = "signatures";
char *str_signaturePath = "[Landroid/content/pm/Signature;";
char *str_certFactory = "java/security/cert/CertificateFactory";
char *str_funcProtoType2 = "(Ljava/lang/String;)Ljava/security/cert/CertificateFactory;";
char *str_getInst = "getInstance";
char *str_x509 = "X509";
char *str_genCert = "generateCertificate";
char *str_funcPT_genCert = "(Ljava/io/InputStream;)Ljava/security/cert/Certificate;";
char *str_x509Cert = "java/security/cert/X509Certificate";
char *str_getPubKey = "getPublicKey";
char *str_funcProtoType3 = "()Ljava/security/PublicKey;";
*/
extern void protect_str_decrypt();

// return seed ^ 0x55555555 if the signature is unchanged
__attribute__((section (".mtext")))
jint verify(JNIEnv *env, jobject ctx, int seed) {
    static int str_decrypted = 0;
    if (!str_decrypted) {
        protect_str_decrypt();
        str_decrypted = 1;
    }

    int result = seed;

    jclass cls = (*env)->GetObjectClass(env, ctx);
    jmethodID mid = (*env)->GetMethodID(env, cls, str_getPkgMgr,
            str_pkgMgrPath);
    jmethodID pnid = (*env)->GetMethodID(env, cls, str_getPkgName,
            str_stringPath);
    if (mid == 0 || pnid == 0) {
        return ERROR;
    }

    jobject pkgMgr_o = (*env)->CallObjectMethod(env, ctx, mid);
    jclass pkgMgr = (*env)->GetObjectClass(env, pkgMgr_o);
    jstring pkgName = (jstring) (*env)->CallObjectMethod(env, ctx, pnid);

    /*flags = PackageManager.GET_SIGNATURES*/
    int flags = 0x40;
    mid = (*env)->GetMethodID(env, pkgMgr, str_getPkgInfo,
            str_funcProtoType1);
    if (mid == 0) {
        return ERROR;
    }
    jobject pack_inf_o = (jobject) (*env)->CallObjectMethod(env, pkgMgr_o, mid,
            pkgName, flags);

    jclass packinf = (*env)->GetObjectClass(env, pack_inf_o);
    jfieldID fid;
    fid = (*env)->GetFieldID(env, packinf, str_signature, str_signaturePath);
    jobjectArray signatures = (jobjectArray) (*env)->GetObjectField(env, pack_inf_o,
            fid);
    jobject signature0 = (*env)->GetObjectArrayElement(env, signatures, 0);
    mid = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, signature0), "toByteArray",
            "()[B");
    jbyteArray cert = (jbyteArray) (*env)->CallObjectMethod(env, signature0, mid);
    if (cert == 0) {
        return ERROR;
    }
    jclass BAIS = (*env)->FindClass(env, "java/io/ByteArrayInputStream");
    if (BAIS == 0) {
        return ERROR;
    }
    mid = (*env)->GetMethodID(env, BAIS, "<init>", "([B)V");
    if (mid == 0) {
        return ERROR;
    }
    jobject input = (*env)->NewObject(env, BAIS, mid, cert);

    jclass CF = (*env)->FindClass(env, str_certFactory);
    mid = (*env)->GetStaticMethodID(env, CF, str_getInst,
            str_funcProtoType2);

    jstring X509 = (*env)->NewStringUTF(env, str_x509);
    jobject cf = (*env)->CallStaticObjectMethod(env, CF, mid, X509);
    if (cf == 0) {
        return ERROR;
    }
    //"java/security/cert/X509Certificate"
    mid = (*env)->GetMethodID(env, CF, str_genCert, str_funcPT_genCert);
    if (mid == 0) {
        return ERROR;
    }
    jobject c = (*env)->CallObjectMethod(env, cf, mid, input);
    if (c == 0) {
        return ERROR;
    }
    jclass X509Cert = (*env)->FindClass(env, str_x509Cert);
    mid = (*env)->GetMethodID(env, X509Cert, str_getPubKey,
            str_funcProtoType3);
    jobject pk = (*env)->CallObjectMethod(env, c, mid);
    if (pk == 0) {
        return ERROR;
    }
    mid = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, pk), "toString",
            "()Ljava/lang/String;");
    if (mid == 0) {
        return ERROR;
    }
    jstring all = (jstring) (*env)->CallObjectMethod(env, pk, mid);
    const char * all_char = (*env)->GetStringUTFChars(env, all, NULL);
    char * out = NULL;
    if (all_char != NULL) {
        if (strstr(all_char, rsa) != NULL) {
            result = seed ^ 0x55555555;
            LOGD("found");
        } else {
            LOGD("not found");
        }
    }

    (*env)->ReleaseStringUTFChars(env, all, all_char);
    
    return result;
}

__attribute__((section (".mtext")))
jint verifyCertificate(JNIEnv *env, jobject thiz, jobject ctx, int seed) {
  return seed;
}

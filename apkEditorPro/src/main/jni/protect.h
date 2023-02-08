#ifndef __PROTECT_H_
#define __PROTECT_H_

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
char *str_x509Cert = "java/security/cert/X509Certificate";
char *str_getPubKey = "getPublicKey";
char *str_funcProtoType3 = "()Ljava/security/PublicKey;";


__attribute__((section (".mtext")))
jint verifyCertificate(JNIEnv *env, jobject thiz, jobject ctx, int seed) {

    return seed;
}

#endif

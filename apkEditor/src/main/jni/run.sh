#!/bin/sh

# For string padding
# Compuled from protect_transform.cpp
./transform >protect_str.h
./transform a >protect_str.c

# Collect the information
cd tmp
LIB_SIZE=`ls -l ../../libs/armeabi/libsyscheck.so |awk '{print $5}'`
# Pro version
unzip -o apkEditorPro-release.apk classes.dex AndroidManifest.xml lib/armeabi/libsyscheck.so
PRO_MANIFEST_SIZE=`ls -l AndroidManifest.xml |awk '{print $5}'`
PRO_DEX_SIZE=`ls -l classes.dex |awk '{print $5}'`
PRO_OFFSET=`strings --radix=d classes.dex |grep ifbhpp |awk '{print $1}'`
# Free version
unzip -o apkEditorFree-release.apk classes.dex AndroidManifest.xml
FREE_MANIFEST_SIZE=`ls -l AndroidManifest.xml |awk '{print $5}'`
FREE_DEX_SIZE=`ls -l classes.dex |awk '{print $5}'`
FREE_OFFSET=`strings --radix=d classes.dex |grep ifbhpp |awk '{print $1}'`
cd ..

# Fix the size if file does not exist
if [ -z "$LIB_SIZE" ]; then 
    LIB_SIZE=0 
fi

# For size prootection (Change the 1st one)
echo "int correct_lib_size[] = {" > data.c
echo ${LIB_SIZE} >> data.c
echo , >> data.c
echo 0 >> data.c
echo , >> data.c

for i in {1..10}; do
    v=`head -200 /dev/urandom | cksum | cut -f1 -d" "`;
    echo `expr $v % 200000` >> data.c;
    echo , >> data.c;
done
echo "};" >> data.c

# correct_dex_size[0, 1] = dex size
# correct_dex_size[2] = offset of 'ifbhpp' inside classes.dex
echo "int correct_dex_size[] = {" >> data.c
echo ${PRO_DEX_SIZE} >> data.c
echo , >> data.c
echo ${FREE_DEX_SIZE} >> data.c
echo , >> data.c
echo ${PRO_OFFSET} >> data.c
echo , >> data.c
echo ${FREE_OFFSET} >> data.c
echo , >> data.c

for i in {1..10}; do
    v=`head -200 /dev/urandom | cksum | cut -f1 -d" "`;
    echo `expr $v % 8000000` >> data.c;
    echo , >> data.c;
done
echo "};" >> data.c

# Set size of AndroidManifest.xml
echo "int correct_manifest_size[] = {" >> data.c
echo ${PRO_MANIFEST_SIZE} >> data.c
echo , >> data.c
echo ${FREE_MANIFEST_SIZE} >> data.c
echo , >> data.c
for i in {1..10}; do
    v=`head -200 /dev/urandom | cksum | cut -f1 -d" "`;
    echo `expr $v % 20000` >> data.c;
    echo , >> data.c;
done
echo "};" >> data.c


/home/pujiang/r10d/ndk-build

# Copy to the file transfer server
FILE=libsyscheck.so
#FILE=libab.so
ssh pujiang@192.168.14.112 rm -f /home/pujiang/tmp/libs/armeabi/*.so
ssh pujiang@192.168.14.112 rm -f /home/pujiang/tmp/libs/armeabi-v7a/*.so
ssh pujiang@192.168.14.112 rm -f /home/pujiang/tmp/libs/arm64-v8a/*.so
ssh pujiang@192.168.14.112 rm -f /home/pujiang/tmp/libs/x86/*.so
scp ../libs/armeabi/$FILE pujiang@192.168.14.112:/home/pujiang/tmp/libs/armeabi/$FILE
scp ../libs/armeabi-v7a/$FILE pujiang@192.168.14.112:/home/pujiang/tmp/libs/armeabi-v7a/$FILE
scp ../libs/arm64-v8a/$FILE pujiang@192.168.14.112:/home/pujiang/tmp/libs/arm64-v8a/$FILE
scp ../libs/x86/$FILE pujiang@192.168.14.112:/home/pujiang/tmp/libs/x86/$FILE

# Check the lib size
LIB_NEW_SIZE=`ls -l ../libs/armeabi/$FILE |awk '{print $5}'`
echo ${LIB_SIZE}
echo ${LIB_NEW_SIZE}
if [ $LIB_SIZE -eq $LIB_NEW_SIZE ]; then
	echo "Lib size is correct!"
else
	echo "Lib size is NOT equal, please build it again!"
fi

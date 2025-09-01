#include <jni.h>
#include <stdio.h>
#include <string.h>

JNIEXPORT jstring JNICALL
Java_iqbal_fahalwan_Terminal_Proses(JNIEnv *env, jobject thiz, jstring baca) {
    const char *cmd = (*env)->GetStringUTFChars(env, baca, 0);

    // Path proot + bash
    const char *proot = "/data/data/iqbal.fahalwan/files/logika/bin/proot";
    const char *bash  = "/data/data/iqbal.fahalwan/files/logika/ubuntu/bin/bash";

    // Buat command lengkap
    char fullcmd[2048];
    snprintf(fullcmd, sizeof(fullcmd),
             "%s %s -c \"%s\"",
             proot, bash, cmd);

    // Buffer hasil
    char buffer[1024];
    char result[8192] = "";

    // Jalankan command lewat popen
    FILE *fp = popen(fullcmd, "r");
    if (fp == NULL) {
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Gagal menjalankan proot + bash");
    }

    // Baca hasil output
    while (fgets(buffer, sizeof(buffer), fp) != NULL) {
        strcat(result, buffer);
    }

    pclose(fp);
    (*env)->ReleaseStringUTFChars(env, baca, cmd);

    // Return hasil ke Java
    return (*env)->NewStringUTF(env, result);
}
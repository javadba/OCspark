#include <string.h>
#include "includes/org_openchai_tensorflow_api_PcieDMAClient.h"

#ifdef __cplusplus
extern "C" {
#endif

// SHB: utility for lastIndexOf
int lastIndexOf(char *str, char c) {
  int ind = -1;
 for (int i=strlen(str)-1;i>=0;i--) {
  if (str[i] == c) {
    ind = i;
    break;
  }
 }
 return ind;
}

char *rfind(char *haystack, char c) {
  int ind = lastIndexOf(haystack, c);
  if (ind < 0) {
    return NULL;
  } else {
    return haystack+ind+1;
   }
 }


// SHB: Helper for generating an output string
jstring makeMsg(JNIEnv *env, jstring str, char* fname) {
  const char *name = (*env)->GetStringUTFChars(env,str, NULL);
   char msg[200];
   jstring result;

   char *fnameShort = rfind(fname, '_');
   sprintf(msg, "CServer %s: %s", fnameShort, name);
   (*env)->ReleaseStringUTFChars(env,str, name);
   puts(msg);
   result = (*env)->NewStringUTF(env,msg);
   return(result);
}

// SHB: Helper for generating an output byte array
jbyteArray makeArray(JNIEnv *env, jstring str, char* fname) {
  const char *name = (*env)->GetStringUTFChars(env,str, NULL);
   jbyte msg[200];

   sprintf(msg, "Server %s says: %s. Oh and have a nice day.", fname, name);
   (*env)->ReleaseStringUTFChars(env,str, name);
   puts(msg);
  jbyteArray result=(*env)->NewByteArray(env, strlen(msg));
  (*env)->SetByteArrayRegion(env, result, 0, strlen(msg), msg);
   return(result);
}


// SHB: Helper for generating an output byte array
  // TODO: figure out how to make return DMAStruct properly
  // Refer to http://stackoverflow.com/questions/7260376/how-to-create-an-object-with-jni
jobject makeObject(JNIEnv *env, jstring str, char* fname) {
  const char *name = (*env)->GetStringUTFChars(env,str, NULL);
   char msg[200];
   jstring result;

   sprintf(msg, "Server %s says: %s. Oh and have a nice day.", fname, name);
   (*env)->ReleaseStringUTFChars(env,str, name);
   puts(msg);
    // 	static class ReadResultStruct{Timestamp ts; DataStruct dataStruct;}
    jclass dataStructClass = (*env)->FindClass(env, "org/openchai/tensorflow/api/DMAStructures$DataStruct");
    // TODO: need to instantiate the class via constructor
    // jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "void(V)");
    // TODO: need to set data into the dataStruct
    // jobject outobj = (*env)->NewObject(env, cls, constructor, obj, 5, 6);
    jclass rcvResultClass = (*env)->FindClass(env, "org/openchai/tensorflow/api/DMAStructures$ReadResultStruct");
    // TODO: Need to do stuff to the rcvResult including setting the dataStruct into it..
//    rcvResultClass->
    jobject rcvResult;
  return(NULL);
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    setupChannelN
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_setupChannelN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    prepareWriteN
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_prepareWriteN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    writeN
 * Signature: (Ljava/lang/String;[B[B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_writeN
  (JNIEnv *env, jobject jobj, jstring str, jbyteArray jarr, jbyteArray jarr2) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeObject(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    completeWriteN
 * Signature: (Ljava/lang/String;)Lorg/openchai/tensorflow/api/DMAStructures/WriteResultStruct;
 */
JNIEXPORT jobject JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_completeWriteN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    prepareReadN
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_prepareReadN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    readN
 * Signature: (Ljava/lang/String;[B[B)Lorg/openchai/tensorflow/api/DMAStructures/ReadResultStruct;
 */
JNIEXPORT jobject JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_readN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeObject(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    completeReadN
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_completeReadN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    shutdownChannelN
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_shutdownChannelN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAClient
 * Method:    readLocalN
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_openchai_tensorflow_api_PcieDMAClient_readLocalN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

#ifdef __cplusplus
}
#endif
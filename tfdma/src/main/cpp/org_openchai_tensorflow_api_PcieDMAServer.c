#include <string.h>
#include "includes/org_openchai_tensorflow_api_PcieDMAServer.h"

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
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    setupChannelN
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_setupChannelN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    registerN
 * Signature: (Lorg/openchai/tensorflow/api/TensorFlowIf/DMACallback;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_registerN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_prepareWriteN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    writeN
 * Signature: (Ljava/lang/String;[B)Lorg/openchai/tensorflow/api/DMAStructures/WriteResultStruct;
 */
JNIEXPORT jobject JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_writeN
  (JNIEnv *env, jobject jobj, jstring str, jbyteArray jarr) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeObject(env, str, fname));
}

JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_completeWriteN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_prepareReadN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}
/*
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    readN
 * Signature: (Ljava/lang/String;)Lorg/openchai/tensorflow/api/DMAStructures/ReadResultStruct;
 */
JNIEXPORT jobject JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_readN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeObject(env, str, fname));
}

JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_completeReadN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    shutdownChannelN
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_shutdownChannelN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeMsg(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    readLocalN
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_readLocalN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);

  return(makeArray(env, str, fname));
}

#ifdef __cplusplus
}
#endif


#include <string.h>
#include "includes/org_openchai_tensorflow_api_PcieDMAServer.h"

#ifdef __cplusplus
extern "C" {
#endif

// SHB: Helper for generating an output string
jstring makeMsg(JNIEnv *env, jstring str, char* fname) {
  const char *name = (*env)->GetStringUTFChars(env,str, NULL);
   char msg[200];
   jstring result;

   sprintf(msg, "Server %s: %s", fname, name);
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
    // 	static class RcvResultStruct{Timestamp ts; DataStruct dataStruct;}
    jclass dataStructClass = (*env)->FindClass(env, "org/openchai/tensorflow/api/DMAStructures$DataStruct");
    // TODO: need to instantiate the class via constructor
    // jmethodID constructor = (*env)->GetMethodID(env, cls, "<init>", "void(V)");
    // TODO: need to set data into the dataStruct
    // jobject outobj = (*env)->NewObject(env, cls, constructor, obj, 5, 6);
    jclass rcvResultClass = (*env)->FindClass(env, "org/openchai/tensorflow/api/DMAStructures$RcvResultStruct");
    // TODO: Need to do stuff to the rcvResult including setting the dataStruct into it..
    jobject rcvResult;
  return(rcvResult);
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

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    sendDataN
 * Signature: (Ljava/lang/String;[B)Lorg/openchai/tensorflow/api/DMAStructures/SendResultStruct;
 */
JNIEXPORT jobject JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_sendDataN
  (JNIEnv *env, jobject jobj, jstring str, jbyteArray jarr) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeObject(env, str, fname));
}

/*
 * Class:     org_openchai_tensorflow_api_PcieDMAServer
 * Method:    rcvDataN
 * Signature: (Ljava/lang/String;)Lorg/openchai/tensorflow/api/DMAStructures/RcvResultStruct;
 */
JNIEXPORT jobject JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_rcvDataN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);
  return(makeObject(env, str, fname));
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
 * Method:    readDataN
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_openchai_tensorflow_api_PcieDMAServer_readDataN
  (JNIEnv *env, jobject jobj, jstring str) {
  char fname[100];
  sprintf(fname, "%s", __func__);

  return(makeArray(env, str, fname));
}

#ifdef __cplusplus
}
#endif


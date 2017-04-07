package org.openchai.tensorflow.api;
import java.sql.Timestamp;
   // DMA Server Side API
/* public static */ public interface DMAStructures {
	// type byte[] byte[]
	static class DataStruct{int dataLen; byte[] dataPtr; /* handle to data */ byte[] md5;}
	static class SendResultStruct{Timestamp ts; DataStruct dataStruct;}
	static class RcvResultStruct{Timestamp ts; DataStruct dataStruct;}
	static class WriteResultStruct{int rc; int nbytes; String exception;}
        static class ReadResultStruct{int rc; int nbytes; String exception; byte[] data; byte[] md5;}
}


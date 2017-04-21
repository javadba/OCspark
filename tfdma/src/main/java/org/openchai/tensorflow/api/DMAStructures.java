package org.openchai.tensorflow.api;
import java.sql.Timestamp;
   // DMA Server Side API
/* public static */ public interface DMAStructures {
	// type byte[] byte[]
	static class DataStruct{int rc; int dataLen; byte[] dataPtr; /* handle to data */ byte[] md5; }
	static class WriteResultStruct{int rc; Timestamp ts; String exception; DataStruct dataStruct;}
	static class ReadResultStruct{int rc; Timestamp ts; String exception; DataStruct dataStruct;}
}


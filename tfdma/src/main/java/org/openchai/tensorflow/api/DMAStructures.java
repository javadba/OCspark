package org.openchai.tensorflow.api;
import java.sql.Timestamp;
   // DMA Server Side API
/* public static */ public interface DMAStructures {
	// type byte[] byte[]
	public static class DataStruct{public int rc; public int dataLen; public byte[] data; /* handle to data */ public byte[] md5; }
	public static class WriteResultStruct{public int rc; public Timestamp ts; public String exception; public DataStruct dataStruct;}
	public static class ReadResultStruct{public int rc; public Timestamp ts; public String exception; public DataStruct dataStruct;}
}


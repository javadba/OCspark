package org.openchai.tensorflow.api;

import java.sql.Timestamp;

import org.openchai.tensorflow.api.DMAStructures.*;

import static org.openchai.tensorflow.api.Logger.*;

public interface TensorFlowIf {
// public static interface TensorFlowClientIf {

  // import DMAStructures.*;
// DMACallback Interface 
  public static interface DMACallback {
    SendResultStruct dataSent();

    RcvResultStruct dataReceived();
  }

  // DMA Interface
  public static interface DMAServer {
    String setupChannel(String setupJson);

    String register(DMACallback callbackIf);

    SendResultStruct sendData(String configJson, byte[] dataPtr);

    RcvResultStruct rcvData(String configJson);

    String shutdownChannel(String shutdownJson);

    byte[] readData(byte[] dataptr);// Retrieve *locally* from dma shared memory location
  }

  // DMA Client API
  public static interface DMAClient {
    String setupChannel(String setupJson);

    String prepareWrite(String configJson);  // Sends "prepare data xfer operation" command to server side

    String write(String configJson, byte[] data, byte[] md5);  // invoke on DMA channel

    WriteResultStruct completeWrite(String configJson);  // Sends "data xfer completed" notification to server

    String prepareRead(String configJson);  // Sends "prepare data xfer operation" command to server side

    ReadResultStruct read(String configJson, byte[] data, byte[] md5);  // invoke on DMA channel

    String completeRead(String configJson);  // Sends "data xfer completed" notification to server

    String shutdownChannel(String setupJson);

    byte[] readData(byte[] dataptr); // Retrieve *locally* from dma shared memory location
  }
}


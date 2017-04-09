package org.openchai.tensorflow.api;

public class PcieDMAClient extends DMAClientBase implements TensorFlowIf.DMAClient {

  @Override
  public String setupChannel(String setupJson) {
    super.setupChannel(setupJson);
    return setupChannelN(setupJson);
  }

  @Override
  public String prepareWrite(String configJson) {

    super.prepareWrite(configJson);
    return prepareWriteN(configJson);
  }

  @Override
  public String write(String configJson, byte[] data, byte[] md5) {

    super.write(configJson, data, md5);
    return writeN(configJson, data, md5);
  }

  @Override
  public DMAStructures.WriteResultStruct completeWrite(String configJson) {

    super.completeWrite(configJson);
    return completeWriteN(configJson);
  }

  @Override
  public String prepareRead(String configJson) {

    super.prepareRead(configJson);
    return prepareReadN(configJson);
  }

  @Override
  public DMAStructures.ReadResultStruct read(String configJson, byte[] data, byte[] md5) {
    super.read(configJson, data, md5);
    return readN(configJson, data, md5);
  }

  @Override
  public String completeRead(String configJson) {
    super.completeRead(configJson);
    return completeReadN(configJson);
  }

  @Override
  public String shutdownChannel(String shutdownJson) {
    shutdownChannel(shutdownJson);
    return shutdownChannelN(shutdownJson);
  }

  @Override
  public byte[] readData(byte[] dataptr) {
    super.readData(dataptr);
    return readDataN(dataptr);
  }

  native String setupChannelN(String setupJson);
  native String prepareWriteN(String configJson);
  native String writeN(String configJson, byte[] data, byte[] md5);
  native DMAStructures.WriteResultStruct completeWriteN(String configJson);
  native String prepareReadN(String configJson);
  native DMAStructures.ReadResultStruct readN(String configJson, byte[] data, byte[] md5);
  native String completeReadN(String setupJson);
  native String shutdownChannelN(String shutdownJson);
  native byte[] readDataN(byte[] dataptr);
}

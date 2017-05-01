package org.openchai.tensorflow.api;

public class PcieDMAClient extends DMAClientBase implements TensorFlowIf.DMAClient {


  public PcieDMAClient() {
    String parentDir = System.getProperty("user.dir");
    if (!parentDir.contains("tfdma")) {
      parentDir += "/tfdma";
    }
    String ext = System.getProperty("os.name").equals("Mac OS X") ? ".dylib" : ".so";
    String libpath = String.format("%s/%s%s",parentDir,"src/main/cpp/dmaclient",ext);
//    String libpath = "dmaserver.dylib";
    System.err.println("Loading DMA native library " + libpath + " ..");
    try {
      System.load(libpath);
    } catch(Exception e) {
      System.err.println("Unable to load native library %s: %s".format( libpath, e.getMessage()));
    }

//    System.loadLibrary(libpath);
  }

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
  public DMAStructures.WriteResultStruct write(String configJson, byte[] data, byte[] md5) {

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
  public DMAStructures.ReadResultStruct read(String configJson) {
    super.read(configJson);
    return readN(configJson);
  }

  @Override
  public String completeRead(String configJson) {
    super.completeRead(configJson);
    return completeReadN(configJson);
  }

  @Override
  public String shutdownChannel(String shutdownJson) {
    super.shutdownChannel(shutdownJson);
    return shutdownChannelN(shutdownJson);
  }

  @Override
  public byte[] readLocal(byte[] dataptr) {
    super.readLocal(dataptr);
    return readLocalN(dataptr);
  }

  native String setupChannelN(String setupJson);
  native String prepareWriteN(String configJson);
  native DMAStructures.WriteResultStruct writeN(String configJson, byte[] data, byte[] md5);
  native DMAStructures.WriteResultStruct completeWriteN(String configJson);
  native String prepareReadN(String configJson);
  native DMAStructures.ReadResultStruct readN(String configJson);
  native String completeReadN(String setupJson);
  native String shutdownChannelN(String shutdownJson);
  native byte[] readLocalN(byte[] dataptr);
}

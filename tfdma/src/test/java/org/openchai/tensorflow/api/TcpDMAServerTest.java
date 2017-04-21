package org.openchai.tensorflow.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.openchai.tensorflow.api.Logger.p;

public class TcpDMAServerTest {
  TcpDMAServer server = null;
  @Before
  public void setUp() throws Exception {
    server = new TcpDMAServer();
  }

  @After
  public void tearDown() throws Exception {
    server.shutdownChannel("blah");
    server = null;
  }

  @Test
  public void setupChannel() throws Exception {
    String res = server.setupChannel("blah");
    p("setupChannel result: %s", res);
  }

  @Test
  public void register() throws Exception {
    server.register(new TensorFlowIf.DMACallback() {
      @Override
      public DMAStructures.WriteResultStruct dataSent() {
        return null;
      }

      @Override
      public DMAStructures.ReadResultStruct dataReceived() {
        return null;
      }
    });
  }

  @Test
  public void write() throws Exception {
    DMAStructures.WriteResultStruct x = server.write("blah", "hello there".getBytes());
    p("sendata result: %s", x);
  }

  @Test
  public void read() throws Exception {
    DMAStructures.ReadResultStruct x = server.read("blah");
    p("rcvata result: %s", x);

  }

  @Test
  public void shutdownChannel() throws Exception {
    String x = server.shutdownChannel("blah");
    p("shutdownChannel result: %s", x);

  }

  @Test
  public void readLocal() throws Exception {
    byte[] dataPtr = "I am a dataPointer".getBytes();
    byte[] x = server.readLocal(dataPtr);
    p("setupChannel result: %s", x);

  }

}
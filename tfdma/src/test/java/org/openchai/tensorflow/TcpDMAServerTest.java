package org.openchai.tensorflow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openchai.tensorflow.api.DMAStructures;
import org.openchai.tensorflow.api.TcpDMAServer;
import org.openchai.tensorflow.api.TensorFlowIf;
import static org.openchai.tensorflow.api.Logger.*;

import static org.junit.Assert.*;

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
      public DMAStructures.SendResultStruct dataSent() {
        return null;
      }

      @Override
      public DMAStructures.RcvResultStruct dataReceived() {
        return null;
      }
    });
  }

  @Test
  public void sendData() throws Exception {
    DMAStructures.SendResultStruct x = server.sendData("blah", "hello there".getBytes());
    p("sendata result: %s", x);
  }

  @Test
  public void rcvData() throws Exception {
    DMAStructures.RcvResultStruct x = server.rcvData("blah");
    p("rcvata result: %s", x);

  }

  @Test
  public void shutdownChannel() throws Exception {
    String x = server.shutdownChannel("blah");
    p("shutdownChannel result: %s", x);

  }

  @Test
  public void readData() throws Exception {
    byte[] dataPtr = "I am a dataPointer".getBytes();
    byte[] x = server.readData(dataPtr);
    p("setupChannel result: %s", x);

  }

}
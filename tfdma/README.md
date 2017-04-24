# Tensorflow DMA Module 

BUILDING: 

<pre>
   ./build.sh
</pre> 

RUNNING: The <b>PcieDMAClientTest</b> runs both the PcieDMAClient and PcieDMAServer. It can be executed by:
    
<pre>
  $ cd tfdma
  $ mvn test
</pre>

Sample output from PcieDMAClientTest and PcieDMAServerTest
<pre>

  -------------------------------------------------------
   T E S T S
  -------------------------------------------------------
  Running org.openchai.tensorflow.api.PcieDMAClientTest
  Loading DMA native library /git/OCSpark/tfdma/src/main/cpp/dmaserver.dylib ..
  [0323 22:51:04.695] Info: SetupChannel for {"fn":"setupChannel","payload":"something"}
  [0323 22:51:04.695] Info: register DMACallback invoked for
  Loading DMA native library /git/OCSpark/tfdma/src/main/cpp/dmaclient.dylib ..
  [0323 22:51:04.695] Info: Starting battery ..
  Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.373 sec
  Running org.openchai.tensorflow.api.PcieDMAServerTest
  Loading DMA native library /git/OCSpark/tfdma/src/main/cpp/dmaserver.dylib ..
  [0323 22:51:04.695] Info: Starting Server battery ..
  Loading DMA native library /git/OCSpark/tfdma/src/main/cpp/dmaserver.dylib ..
  [0323 22:51:04.695] Info: SetupChannel for "blah"
  [0323 22:51:04.695] Info: setupChannel result: CServer setupChannelN: "blah"
  [0323 22:51:04.695] Info: prepareWrite for "PrepareSend"
  [0323 22:51:04.695] Info: prepareWrite result: CServer prepareWriteN: "PrepareSend"
  [0323 22:51:04.695] Info: write for "blah" and dataLen=11
  [0323 22:51:04.695] Info: write result: null
  [0323 22:51:04.695] Info: prepareWrite for "completeSend"
  [0323 22:51:04.695] Info: completeWrite  result: CServer prepareWriteN: "completeSend"
  [0323 22:51:04.695] Info: prepareWrite for "PrepareRcv"
  [0323 22:51:04.695] Info: prepareRead result: CServer prepareWriteN: "PrepareRcv"
  [0323 22:51:04.695] Info: read for "read"
  [0323 22:51:04.695] Info: read result: null
  [0323 22:51:04.695] Info: read for "completeRcv"
  [0323 22:51:04.695] Info: completeRead result: null
  [0323 22:51:04.695] Info: ShutdownCannel for "blah"
  [0323 22:51:04.695] Info: shutdownChannel result: CServer shutdownChannelN: "blah"
  Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 sec
  Running org.openchai.tensorflow.api.TcpDMAServerTest
  [0323 22:51:04.695] Info: SetupChannel for blah
  [0323 22:51:04.695] Info: setupChannel result: {"fn":"SetupChannel","rc":1,"msg":"Foo"}
  [0323 22:51:04.695] Info: ShutdownCannel for blah
  [0323 22:51:04.695] Info: readLocal for datalen=18
  [0323 22:51:04.695] Info: setupChannel result: [B@536aaa8d
  [0323 22:51:04.695] Info: ShutdownCannel for blah
  [0323 22:51:04.695] Info: ShutdownCannel for blah
  [0323 22:51:04.695] Info: shutdownChannel result: "ShutdownChannel completed for blah"
  [0323 22:51:04.695] Info: ShutdownCannel for blah
  [0323 22:51:04.695] Info: register DMACallback invoked for
  [0323 22:51:04.695] Info: ShutdownCannel for blah
  [0323 22:51:04.695] Info: read for blah
  [0323 22:51:04.695] Info: rcvata result: org.openchai.tensorflow.api.DMAStructures$ReadResultStruct@e320068
  [0323 22:51:04.695] Info: ShutdownCannel for blah
  [0323 22:51:04.695] Info: write for blah and dataLen=11
  [0323 22:51:04.695] Info: sendata result: org.openchai.tensorflow.api.DMAStructures$WriteResultStruct@1f57539
  [0323 22:51:04.695] Info: ShutdownCannel for blah
  Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 sec
  CServer setupChannelN: {"fn":"setupChannel","payload":"something"}
  CServer registerN: (null)
  CServer setupChannelN: {"fn":"setupChannel","payload":"something"}
  CServer prepareWriteN: {"fn":"prepareWrite","payload":"something"}
  Server Java_org_openchai_tensorflow_api_PcieDMAClient_writeN says: {"fn":"write","payload":"something"}. Oh and have a nice day.
  CServer completeWriteN: {"fn":"completeWrite","payload":"something"}
  CServer prepareReadN: {"fn":"prepareRead","payload":"something"}
  CServer completeReadN: {"fn":"completeRead","payload":"something"}
  CServer shutdownChannelN: blah
  CServer setupChannelN: "blah"
  CServer prepareWriteN: "PrepareSend"
  Server Java_org_openchai_tensorflow_api_PcieDMAServer_writeN says: "blah". Oh and have a nice day.
  CServer prepareWriteN: "completeSend"
  CServer prepareWriteN: "PrepareRcv"
  Server Java_org_openchai_tensorflow_api_PcieDMAServer_readN says: "read". Oh and have a nice day.
  Server Java_org_openchai_tensorflow_api_PcieDMAServer_readN says: "completeRcv". Oh and have a nice day.
  CServer shutdownChannelN: "blah"

  Results :

  Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

  [INFO] ------------------------------------------------------------------------
</pre>



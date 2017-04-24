# Tensorflow DMA Module 

BUILDING: Run the [build.sh](build.sh) in this directory.

RUNNING: The <b>PcieDMAClientTest</b> runs both the PcieDMAClient and PcieDMAServer. It can be executed by:
    
  $ cd tfdma
  $mvn test


Sample output from PcieDMAClientTest and PcieDMAServerTest


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



The <b>TFClientTest</b> sends images from TFClient to TFServer that runs TensorFlow and returns its <b>Image Tagging</b> results.

   mvn package -Dmaven.test.skip  exec:java -Dexec.mainClass="org.openchai.tensorflow.TfClient"

  
Sample Output from <b>TFClientTest</b>

  Created XferQServerIf
  QReader thread started
  Starting XferConServerIf on TCA0080ALKVTAGB:1234 ..
  Starting XferServerIf on TCA0080ALKVTAGB:1235 ..
  Starting TfServerIf on TCA0080ALKVTAGB:1236 ..
  TcpClient: Connecting XferCon to TCA0080ALKVTAGB:1234 ..
  TcpClient: Bound XferCon to TCA0080ALKVTAGB:1234
  TcpClient: Connecting Xfer to TCA0080ALKVTAGB:1235 ..
  TcpClient: Bound Xfer to TCA0080ALKVTAGB:1235
  Info: Received connection request from TCA0080ALKVTAGB@192.168.0.3 on socket 62713
  Info: Received connection request from TCA0080ALKVTAGB@192.168.0.3 on socket 62714
  Debug: Listening for messages..
  Debug: Listening for messages..
  TcpClient: Connecting TfClient to TCA0080ALKVTAGB:1236 ..
  TcpClient: Bound TfClient to TCA0080ALKVTAGB:1236
  Info: Received connection request from TCA0080ALKVTAGB@192.168.0.3 on socket 62715
  Debug: Listening for messages..
  LabelImg..
  PrepareWrite ..
  unpacked org.openchai.tcp.xfer.PrepWriteReq
  Debug: Message received: PrepWriteReq(TcpXferConfig(blah,/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg))
  Prepping the Datawrite config=TcpXferConfig(blah,/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg)
  Debug: Sending response:  PrepResp(PrepRespStruct(0,0,blah))
  Info: request: received 377 bytes
  unpacked org.openchai.tcp.xfer.PrepResp
  PrepareWrite response: PrepResp(PrepRespStruct(0,0,blah))
  Debug: XferIf: Sending request: XferWriteParams: config=TcpXferConfig(blah,/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg) datalen=111821} md5len=16}
  unpacked org.openchai.tcp.xfer.XferWriteReq
  Debug: Message received: XferWriteReq(XferWriteParams: config=TcpXferConfig(blah,/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg) datalen=111821} md5len=16})
  XferWriteReq! datalen=111821
  QReader: received TaggedEntry(TestWriteTag,[B@423328cb)
  Debug: Sending response:  XferWriteResp(abc,111821,0,[B@69ab9339)
  Info: request: received 439 bytes
  unpacked org.openchai.tcp.xfer.XferWriteResp
  Debug: XferIf: Result is XferWriteResp(abc,111821,0,[B@37efd131)
  unpacked org.openchai.tcp.xfer.CompleteWriteReq
  Debug: Message received: org.openchai.tcp.xfer.CompleteWriteReq@3a5ed70c
  Completed Write for TcpXferConfig(blah,/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg) the Datawrite config=TcpXferConfig(blah,/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg)
  Debug: Sending response:  CompletedResp(PrepRespStruct(0,0,blah))
  Info: request: received 386 bytes
  unpacked org.openchai.tcp.xfer.CompletedResp
  CompleteWrite response: CompletedResp(PrepRespStruct(0,0,blah))
  Client: beginning Write Controller for TcpXferConfig(/tmp/xferout1,/tmp/xferout2)
  PrepareWrite ..
  unpacked org.openchai.tcp.xfer.PrepWriteReq
  Debug: Message received: PrepWriteReq(TcpXferConfig(/tmp/xferout1,/tmp/xferout2))
  Prepping the Datawrite config=TcpXferConfig(/tmp/xferout1,/tmp/xferout2)
  Debug: Sending response:  PrepResp(PrepRespStruct(0,0,/tmp/xferout1))
  Info: request: received 386 bytes
  unpacked org.openchai.tcp.xfer.PrepResp
  PrepareWrite response: PrepResp(PrepRespStruct(0,0,/tmp/xferout1))
  Debug: XferIf: Sending request: XferWriteParams: config=TcpXferConfig(/tmp/xferout1,/tmp/xferout2) datalen=111956} md5len=16}
  unpacked org.openchai.tcp.xfer.XferWriteReq
  Debug: Message received: XferWriteReq(XferWriteParams: config=TcpXferConfig(/tmp/xferout1,/tmp/xferout2) datalen=111956} md5len=16})
  XferWriteReq! datalen=111956
  Debug: Sending response:  XferWriteResp(abc,111956,0,[B@128db6cf)
  QReader: received TaggedEntry(FunnyPicTag,[B@460ca455)
  Info: request: received 439 bytes
  unpacked org.openchai.tcp.xfer.XferWriteResp
  Debug: XferIf: Result is XferWriteResp(abc,111956,0,[B@681a8b4e)
  unpacked org.openchai.tcp.xfer.CompleteWriteReq
  Debug: Message received: org.openchai.tcp.xfer.CompleteWriteReq@26f97588
  Completed Write for TcpXferConfig(/tmp/xferout1,/tmp/xferout2) the Datawrite config=TcpXferConfig(/tmp/xferout1,/tmp/xferout2)
  Debug: Sending response:  CompletedResp(PrepRespStruct(0,0,/tmp/xferout1))
  Info: request: received 395 bytes
  unpacked org.openchai.tcp.xfer.CompletedResp
  CompleteWrite response: CompletedResp(PrepRespStruct(0,0,/tmp/xferout1))
  Client: got result XferWriteResp(abc,111956,0,[B@681a8b4e)
  unpacked org.openchai.tensorflow.LabelImgReq
  Debug: Message received: LabelImgReq(LabelImg: tag=funnyPic path=/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg datalen=111821 md5len=16)
  Service: Invoking LabelImg: struct=LabelImg: tag=funnyPic path=/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg datalen=111821 md5len=16
  LabelImg: processing LabelImg: tag=funnyPic path=/git/OCSpark/tf/src/main/resources//images/JohnNolteAndDad.jpg datalen=111821 md5len=16 ..
  FindInQ: looking for funnyPic: entries=0
  LabelImg: Found entry [empty]
  Writing 111821 bytes to /tmp/images/JohnNolteAndDad.jpg ..
  Exec: /shared/tensorflow//shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image--image=/tmp/images/JohnNolteAndDad.jpg  pbDir=/shared/tensorflow
  Process [/shared/tensorflow//shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image--image=/tmp/images/JohnNolteAndDad.jpg] completed in 2594 with rc=0 stdoutLen=0 stderrLen=1309
  Debug: Sending response:  LabelImgResp(LabelImgRespStruct(ExecResult(/shared/tensorflow//shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image--image=/tmp/images/JohnNolteAndDad.jpg,2594,0,,W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use SSE4.2 instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX2 instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use FMA instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/framework/op_def_util.cc:332] Op BatchNormWithGlobalNormalization is deprecated. It will cease to work in GraphDef version 9. Use tf.nn.batch_normalization().
  I tensorflow/examples/label_image/main.cc:206] dam (720): 0.227693
  I tensorflow/examples/label_image/main.cc:206] coho (448): 0.220099
  I tensorflow/examples/label_image/main.cc:206] stole (998): 0.110214
  I tensorflow/examples/label_image/main.cc:206] valley (360): 0.0660103
  I tensorflow/examples/label_image/main.cc:206] suspension bridge (681): 0.0555012)))
  Info: request: received 2430 bytes
  unpacked org.openchai.tensorflow.LabelImgResp
  LabelImg response: LabelImgResp(LabelImgRespStruct(ExecResult(/shared/tensorflow//shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image--image=/tmp/images/JohnNolteAndDad.jpg,2594,0,,W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use SSE4.2 instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX2 instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use FMA instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/framework/op_def_util.cc:332] Op BatchNormWithGlobalNormalization is deprecated. It will cease to work in GraphDef version 9. Use tf.nn.batch_normalization().
  I tensorflow/examples/label_image/main.cc:206] dam (720): 0.227693
  I tensorflow/examples/label_image/main.cc:206] coho (448): 0.220099
  I tensorflow/examples/label_image/main.cc:206] stole (998): 0.110214
  I tensorflow/examples/label_image/main.cc:206] valley (360): 0.0660103
  I tensorflow/examples/label_image/main.cc:206] suspension bridge (681): 0.0555012)))
  Received label result: LabelImgResp(LabelImgRespStruct(ExecResult(/shared/tensorflow//shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image--image=/tmp/images/JohnNolteAndDad.jpg,2594,0,,W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use SSE4.2 instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX2 instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use FMA instructions, but these are available on your machine and could speed up CPU computations.
  W tensorflow/core/framework/op_def_util.cc:332] Op BatchNormWithGlobalNormalization is deprecated. It will cease to work in GraphDef version 9. Use tf.nn.batch_normalization().
  I tensorflow/examples/label_image/main.cc:206] dam (720): 0.227693
  I tensorflow/examples/label_image/main.cc:206] coho (448): 0.220099
  I tensorflow/examples/label_image/main.cc:206] stole (998): 0.110214
  I tensorflow/examples/label_image/main.cc:206] valley (360): 0.0660103
  I tensorflow/examples/label_image/main.cc:206] suspension bridge (681): 0.0555012)))
  We're done!

       


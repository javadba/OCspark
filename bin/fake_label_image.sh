#!/usr/bin/env bash
cat<<-EOF
*** FAKE LABEL IMAGE OUTPUT***
W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use SSE4.2 instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX2 instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use FMA instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/framework/op_def_util.cc:332] Op BatchNormWithGlobalNormalization is deprecated. It will cease to work in GraphDef version 9. Use tf.nn.batch_normalization().
I tensorflow/examples/label_image/main.cc:206] dam (720): 0.227693
I tensorflow/examples/label_image/main.cc:206] coho (448): 0.220099
I tensorflow/examples/label_image/main.cc:206] stole (998): 0.110214
I tensorflow/examples/label_image/main.cc:206] valley (360): 0.0660103
I tensorflow/examples/label_image/main.cc:206] suspension bridge (681): 0.0555012)))
Received label result: LabelImgResp(LabelImgRespStruct(ExecResult(/shared/tensorflow//shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image--image=/tmp/images/JohnNolteAndDad.jpg,1369,0,,W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use SSE4.2 instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX2 instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use FMA instructions, but these are available on your machine and could speed up CPU computations.
W tensorflow/core/framework/op_def_util.cc:332] Op BatchNormWithGlobalNormalization is deprecated. It will cease to work in GraphDef version 9. Use tf.nn.batch_normalization().
I tensorflow/examples/label_image/main.cc:206] dam (720): 0.227693
I tensorflow/examples/label_image/main.cc:206] coho (448): 0.220099
I tensorflow/examples/label_image/main.cc:206] stole (998): 0.110214
I tensorflow/examples/label_image/main.cc:206] valley (360): 0.0660103
I tensorflow/examples/label_image/main.cc:206] suspension bridge (681): 0.0555012)))
*** END FAKE LABEL IMAGE OUTPUT***
EOF
## 适配rocketmq 4.4.0

## 模块自定义consumer消费进来时线程上下文的Classloader是基座的
### 遇到问题
模块自定义consumer消费进来时线程上下文的Classloader是基座的
### 问题原因
org.apache.rocketmq.client.impl.MQClientManager#instance是个静态字段，导致MQClientInstance是多模块与基座共用的。
开源版本rocketmq的consumer可以主动设置instanceName，但实际使用中很少有应用主动设置instanceName，导致不同模块的org.apache.rocketmq.client.ClientConfig.buildMQClientId返回值一样。
进而导致MQClientInstance、PullMessageService共用一个，拉消息的线程只在基座启动时创建了一次，这时线程的TCCL就是基座的，导致整个消费过程线程的TCCL都是基座的
### 改动点
重写覆盖掉rocketmq的ConsumeMessageConcurrentlyService、ConsumeMessageOrderlyService类，在类构造时保存当前的TCCL，在执行consumeExecutor.submit的地方切换线程的TCCL为类构造时Classloader

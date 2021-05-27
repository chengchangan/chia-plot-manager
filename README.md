# [Chia](https://www.chia.net/) [RPC](https://github.com/chengchangan/chia-websocket) plotClient for Java (1.8 and above)
### 项目功能
```
1、使用websocket连接chia，实现P盘等相关操作，因为没有发现相关rpc接口。
2、P盘的并发控制以服务器为级别，因为chia本身控制的并发是以队列为单位。
3、实现多服务器（矿机）控制P盘并发，监听进度，P盘状态。
```

### 准备工作
```
1、安装Openssl工具
2、win + r，输入[%USERPROFILE%\.chia\mainnet\config\ssl\daemon]后回车
3、打开cmd进入此目录，进行下面步骤证书转换
```


### 转换证书

```shell
openssl pkcs12 -export \
  -in private_daemon.crt \
  -inkey private_daemon.key \
  -out private_daemon.p12 \
  -name "chia.net"

将生成的证书[private_daemon.p12]，拷贝到项目资源目录，以便项目读取
```

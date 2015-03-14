_下载最新的hyk-proxy [地址](http://code.google.com/p/hyk-proxy/downloads/list)_

hyk-proxy - Release Notes

### Version 0.9.4 - 7月17日, 2011 ###
  * GAE的支持Android Client（Server端需要升级部署）

### Version 0.9.3 - 5月14日, 2011 ###
  * GAE的HTTPS代理场景下动态伪造X509证书
  * GAE支持配置站点自动注入Range头域（减少失败尝试）
  * SPAC支持中转到SOCKS代理
  * SPAC支持调用外部脚本

### Version 0.9.2.1 - 4月4日, 2011 ###
  * Bug修复:一个GAE Client导致CPU占用100%的bug


### Version 0.9.2 - 4月2日, 2011 ###
  * 更多，更完善的https选项支持，包括https proxy
  * 支持自定义的`<appid>`.appspot.com到任意域名/IP的映射设置（无需修改系统host文件）
  * 重用已连接的HTTP(S)长连接以提高性能
  * 支持设置本地proxy的下一跳Google服务地址（部分企业内网无法访问appspot情况可用）
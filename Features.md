# 简介 #

hyk-proxy按发布版本实现的特性列表

# hyk-proxy 0.9.4特性（released） #
  * GAE支持Android Client（Server需要升级部署）

# hyk-proxy 0.9.3特性（released） #
  * GAE的https代理场景下支持动态伪造证书
    * 浏览器可以导入`<hyk-proxy>`/etc/cert/Fake-ACRoot-Certificate.cer
    * 如果需要单独生成证书，执行`<hyk-proxy>`/bin/gen\_ca.bat或者`<hyk-proxy>`/bin/gen\_ca.sh，将生成的两个文件拷贝到`<hyk-proxy>`/etc/cert/下
  * GAE Client支持设置自动注入Range header
    * 修改`<hyk-proxy>`/plugins/gae/etc/hyk-proxy-gae-conf.xml, eg:
```
           <!-- 
               Sometimes, you already know you can inject a 'Range' header in  the proxy request  to avoid a fail-try request. Use ';' as the separator for sites
             -->
            <injectRangeHeaderSites>
                 youtube.com
            </injectRangeHeaderSites>
```
    * 配置在这里的站点模糊匹配成功的情况下，会自动插入Range头域避免失败尝试
  * SPAC支持中转到SOCKS代理
    * 在spac.csl脚本中定义socks代理格式为：
```
           #支持socks4/socks5
           $SSHD="socks5:127.0.0.1:7070";
```
    * SPAC支持调用外部脚本
      * 在spac.csl脚本中调用外部程序格式为：
```
           $output=system("./test.sh");
           #输出外部程序的标准输出到hyk-proxy的日志
           log($output);
```

# hyk-proxy 0.9.2特性（released） #
  * GAE Client支持自定义appid的域名映射到任意地址/IP, eg
    * 修改`<hyk-proxy>`/plugins/gae/etc/GoogleMappingHosts.txt
    * 添加映射规则，可填多项，注意"#"开头的行是注释，不生效：
```
           xyz.appspot.com=www.google.com.hk
           abc.appspot.com=203.208.39.104
```
  * GAE Client支持设置local proxy的下一跳Google服务地址
    * 某些内网环境下必须设置固定proxy才能访问外网，而此proxy无法访问appspot，不过却可以访问某些Google服务，这种情况下可以设置local proxy的下一跳Google服务地址
    * 编辑修改`<hyk-proxy>`/plugins/gae/etc/hyk-proxy-gae-conf.xml
    * 在local proxy配置中增加nextHopGoogleServer的配置，eg：
```
	   <localProxy>
		<host>proxy.abc.com</host>
		<port>80</port> 
		<nextHopGoogleServer>www.google.com.hk</nextHopGoogleServer>
	   </localProxy>
```
  * GAE Client完整支持HTTPS

# hyk-proxy 0.9.1特性（released） #
  * 更改图标/大量bug fix

# hyk-proxy 0.9.0特性（released） #
  * 支持自动升级
  * 重新设计实现的插件体系
  * Windows下的可执行安装包

# hyk-proxy 0.8.6特性（released） #
  * Client支持插件
  * SPAC插件 -- 完成更为强大的类似PAC功能
  * 多`AppId`情况下，支持`AppId`和固定site的绑定.编辑修改`<hyk-proxy>`/plugins/gae/etc/hyk-proxy-gae-conf.xml, eg:
```
	<AppIdBindings>
           <Binding appid="hyk-proxy-demo">
               <site>twitter</site>
               <site>youtube</site>
           </Binding>
       </AppIdBindings>
```


# hyk-proxy 0.8.5特性（released） #

  * Client重构以更容易支持其它类型server/network -- 100%
  * 支持appid分享计划 -- 100%
  * 支持普通HTTP连接的加密以及第三方加密(需要hack code), 默认开启加密  -- 100%
  * 支持自动更新检查（Client & Server） -- 100%
  * Server集成appcfgwrapper安装工具 -- 100%
  * 升级Base64实现（MIG64实现, encode加速三倍，decode无改善， 仅在XMPP模式下生效）-- 100%
  * 升级RPC框架,支持Remote Object Storage（大量的bugfix以及性能改善） -- 100%
  * 支持按用户流量控制（在流量统计开启时生效）-- 100%
  * 支持HTTP连接简单URL方式（一种针对特殊firewall的边缘情况）-- 100%
  * 支持Seattle云计算平台 -- 100%

# hyk-proxy已经实现的特性 #
  * 支持多AppId
  * 支持HTTP和XMPP两种模式
  * XMPP模式支持多XMPP帐号分担负载
  * 支持上行/下行压缩，压缩则可选择LZF，GZIP， ZIP
  * 下行压缩支持按content-type过滤
  * 多线程下载大文件
  * 支持local proxy for hyk-proxy-client
  * 支持IPV6
  * 支持用户/组管理，以及相关site黑名单配置
  * 支持按访问site的流量统计
  * Client支持CLI和GUI两种启动方式
  * Client提供仿UNIX的CLI管理工具，用于用户/组管理


# FAQ #
> ## 如何提交需求？ ##
> 在[Issue Tracker](http://code.google.com/p/hyk-proxy/issues/list)中提交一个Issue
> ## hyk-proxy发布有roadmap吗？ ##
> 目前由于是业余时间个人开发，暂时没有固定roadmap计划
> ## 我想hyk-proxy的xx版本提供xx功能，通过何种途径？ ##
> 优先提交Issue， 其次可在[hyk-proxy-discuss](http://groups.google.com/group/hyk-proxy)中提交讨论


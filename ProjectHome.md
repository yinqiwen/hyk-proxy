# **重要项目知会** #
  * 项目已经更名/迁移到[Snova](http://snova.googlecode.com)，并与hyk-proxy历史版本不再兼容
  * hyk-proxy不再更新，hyk-proxy的相关问题请转移到项目[Snova](http://snova.googlecode.com)
  * hyk-proxy的AppID共享服务端任然继续运行，不过由于代码并未更新加上Google的新策略，经常会出现CPU、DB超过配额的情况。同时[Snova](http://snova.googlecode.com)也支持AppID共享计划，不过与hyk-proxy不兼容

# **关于** #
  * hyk-proxy是一个web proxy框架，支持基于`Google AppEngine`平台和[Seattle平台](https://seattlegeni.cs.washington.edu)，以及PHP Web空间的proxy实现
  * hyk-proxy从V0.9.0开始结构更改为framework+plugins方式，具体proxy实现由各个plugin完成
  * 在基于`AppEngine`实现的proxy部分，展示了通过RPC来实现高性能web proxy的途径。基于目前基于`AppEngine`部分实现机制，将来还可以实现暴露更多GAE特有功能,实现为一个远程的GAE lib，而不仅仅作为简单web proxy（目前已经实现用户权限管理/统计/基于用户流量控制等功能）。
  * 在基于Seattle部分，目前仅实现web proxy功能，包括完整的https tunnel
  * 在基于PHP Web空间部分，实现了http proxy功能，以及满足特定网络条件下的完全https tunnel
  * hyk-proxy目前支持HTTP(S) to GAE/XMPP to GAE/TCP to Seattle/HTTP to PHP四种方式完成web proxy功能
  * hyk-proxy从V0.8.6开始支持plugin，理论上可以任意扩展而不仅仅局限于GAE/Seattle/PHP

# **hyk-proxy原理** #
  * hyk-proxy分为两部分，一部分运行于远程server上，如GAE/Seattle/PHP Web Space，属于remote server;另一部分运行于本地，属于local server,实质为一个中转的http server。
  * 当浏览器设置的代理服务器地址为local server地址时，local server将浏览器的请求编码发送给remote server执行proxy功能
    1. 若remote server为GAE平台，则local server将http请求通过一个通用RPC框架远程调用到remote server上，获取远程调用结果；
    1. 若remote server为Seattle平台， 则local server将http请求加密后直接转发给remote server
    1. 若remote server为PHP Space，实现原理类似Seattle部分，不同之处在于https tunnel实现是另外一种构建tunnel技术，对当前网络条件也有约束
  * local server在获取调用结果简单处理后，转给浏览器完成一次代理访问。
  * 一次web访问经过此代理的完整过程如下：
> > Browser`<-->`Local Server`<-->`Remote Server`<-->`Real Web Server


> 其中Local Server与Remote Server之间属于通信层部分，不局限于HTTP连接；如GAE Remote Server由于防火墙原因无法直接通过HTTP连接，可采用XMPP模式穿越
  * 由于基于`AppEngine`部分RPC框架通信层部分可以替换，因此local server与GAE remote server之间的通信方式可以视实际情况替换。由于GAE本身的限制，目前理论上只有三种方式可以选择：
    1. HTTP，local server作为HTTP client访问remote server
    1. XMPP，local server作为一个XMPP user与remote server进行通信
    1. EMAIL，local server 与remote server发送/接受email通信（由于EMail限制较大，不适于用于RPC通信协议，目前不会实现）
  * 而基于Seattle部分则在理论上支持TCP/UDP通信，目前实现TCP连接
  * 基于PHP Web Hosting部分，目前通过HTTP方式完成普通http proxy，以及一种tunnel技术完成https proxy实现
  * hyk-proxy目前支持的四种默认内部proxy模式比较
    * HTTP2GAE/HTTPS2GAE
> > > 由于服务器的先天优势，在性能上相比其它方式占有绝对优势；不足则在于严重依赖GAE/Google的服务直连可达情况
    * XMPP2GAE
> > > 由于是绕道XMPP服务，所以HTTP模式的不足在这里是不存在的；此外，由于这里默认是SSL加密通讯，通讯安全性更强；由于前面所说的原因，在速度上较HTTP有所欠缺
    * Seattle
> > > Seattle是另一个云计算服务，在编程模型上的限制较GAE以及PHP远少，所以可以按照标准模式实现HTTPS的proxy（没有证书问题），以及没有GAE的一些固有限制情况下（GAE的固有限制看[这里](http://code.google.com/p/hyk-proxy/wiki/SpecialProxyAutoConfig)）;当然，其弱势在于Seattle服务的稳定性以及Seattle服务器的带宽性能
    * PHP
> > > 基于PHP的实现相对于GAE的优势在于限制较少，以及在满足特定条件下可以实现完整https tunnel；相对于Seattle的优势在于服务的稳定性以及较快的带宽

# **hyk-proxy的插件** #
  * hyk-proxy中很多功能由插件支持，默认并不安装，如Seattle/PHP/SPAC等，所有支持的插件具体说明/下载请查看项目[hyk-proxy-plugins](http://hyk-proxy-plugins.googlecode.com)

# **如何使用** #
  * 安装运行依赖
    1. JRE/JDK 1.6+
    1. Google App Engine SDK(Java) ([最新版本](http://code.google.com/intl/en/appengine/downloads.html))
  * 创建自己的GAE应用 http://appengine.google.com/ (必须)
  * 创建自己的Seattle账户以及Vessels [Seattle](https://seattlegeni.cs.washington.edu)(可选，仅对Programmer推荐使用)
  * 申请自己的PHP空间（可选）
  * 部署Remote Server
    1. 部署应用到Google服务器 (必须)
      * GUI方式
        * appcfgwrapper安装程序已经集成到`hyk-proxy-gae-server-[version].zip`中，执行install.bat/install.sh即可，具体请参考此项目说明 http://code.google.com/p/appcfgwrapper/
      * 命令行方式
        * 下载并解压`hyk-proxy-gae-server-[version].zip`
        * 进入解压的目录, 修改war/WEB-INF/appengine-web.xml， 将`<application>`值改为自己创建的appid
        * 执行appcfg.cmd/appcfg.sh update war上传, 注意在解压后进入的目录执行(appcfg在'<Google App Engine SDK>/bin'下 )
    1. 部署应用到Seattle平台（可选，仅对Programmer推荐使用）
      * Seattle server程序在seattle plugin的deploy目录下，部署运行请参考([Seattle](https://seattlegeni.cs.washington.edu))
    1. 部署应用到PHP空间（可选）
      * 上传phptunnel plugin目录下deploy/tunnel.php到服务器上

  * 运行Local Server
    1. 解压`hyk-proxy-[version].zip` 或者执行`hyk-proxy-install_[version].exe`(0.9.0后)
    1. GUI方式
      * 执行bin/startgui.bat（windows）或者startgui.sh（linux/unix/mac）启动
    1. 命令行方式
      * 执行bin/start.bat（windows）或者start.sh（linux/unix/mac）启动local server，bin/stop.bat(stop.sh)停止
    1. 浏览器设置代理地址默认为 127.0.0.1:48100，可以修改
  * 安装plugins
    1. hyk-proxy中所有proxy功能均是由plugin来完成，包括基于GAE的proxy实现也是一个plugin。默认情况下，GAE的plugin包含在framework的安装包中，不需要单独下载
    1. plugin安装包一般是zip包，目前均在此项目的site上；安装一般有两种方式:
      * GUI方式：在GUI界面上选择Plugins tab然后选择Available tab，点击相应plugin的install button即可
      * CLI方式：下载相应zip包，放置到`<hyk-proxy>/plugins`下重启即可
  * 配置
    * Framework
> > > Framework的配置仅有两三项，具体涉及proxy的配置均在相应plugin的配置中;目前均在etc/hyk-proxy-conf.xml中（GUI启动则在Config对话框中配置）,简要说明如下：
      1. `<localserver host="localhost" port="48100" />`, 默认的proxy地址，相应的浏览器的proxy地址应该填"127.0.0.1:48100"
      1. `<proxyEventServiceFactory>GAE</proxyEventServiceFactory>`, 默认选择的proxy实现，若安装了其他的plugin，则可以在此修改为其它的proxy实现
> > > > ![http://hyk-proxy.googlecode.com/svn/wiki/images/maincfg.png](http://hyk-proxy.googlecode.com/svn/wiki/images/maincfg.png)
    * GAE Plugin

> > > 基于GAE实现大部分配置均在hyk-proxy-gae-conf.xml中（GUI启动则在GAE plugin的相应Config对话框中配置），简要说明如下：
      * `<hyk-proxy-server appid="" />` 为已部署的ApplicationID， 即`<appid>.appspot.com`中`<appid>`, eg:
```
                 <hyk-proxy-server appid="hyk-proxy-demo" /> 
```
> > > > GUI方式下参考下面方式
> > > > > ![http://hyk-proxy.googlecode.com/svn/wiki/images/gaecfg1.png](http://hyk-proxy.googlecode.com/svn/wiki/images/gaecfg1.png)
> > > > > ![http://hyk-proxy.googlecode.com/svn/wiki/images/gaecfg2.png](http://hyk-proxy.googlecode.com/svn/wiki/images/gaecfg2.png)
      * `<hyk-proxy-server appid="" />`可配置多个，此情况下， 代理过程中采用轮询策略选择, eg:
```
                <hyk-proxy-server appid="hyk-proxy-demo" /> 
                <hyk-proxy-server appid="myappid" /> 
```
      * `<hyk-proxy-server appid="" />` 中可以配置用户名/密码，具体用户名/密码概念请参考[Authorization](http://code.google.com/p/hyk-proxy/wiki/Authorization), eg:
```
               <hyk-proxy-server appid="hyk-proxy-demo" user="root" passwd="12345"/> 
```
      * 如果不配置appid的话，client会到master node上获取数个共享的appid用于自身启动，注意：
        * 共享的appid只能用于匿名用户使用（匿名用户的概念看这里[Authorization](http://code.google.com/p/hyk-proxy/wiki/Authorization)）
        * appid所有者可以针对匿名用户设置proxy黑名单，proxy流量控制，所以不配置自己的appid是无法保证应用稳定性
      * `<XMPPAccount user="" passwd="" />`为XMPP账户，是可选配置，在HTTP无法直连情况下可启用，目前可配置GTALK/jabber.org/OVI等几乎所有XMPP帐号（若有不支持的XMPP account，请提交[ISSUE](http://code.google.com/p/hyk-proxy/issues/list)）, eg:
```
              <XMPPAccount user="abc@gmail.com" passwd="123456" />
```

> > > GUI方式则需要在·Connection Tab下配置
> > > > ![http://hyk-proxy.googlecode.com/svn/wiki/images/xmpp.png](http://hyk-proxy.googlecode.com/svn/wiki/images/xmpp.png)
      * XMPP账户可配置多个，此情况下代理过程中采用轮询策略选择,eg:
```
             <XMPPAccount user="abc@gmail.com" passwd="123456" />
             <XMPPAccount user="xyz@ovi.com" passwd="123456" />
```
      * XMPP模式仅在connection mode模式为2（`<connectionMode>2</connectionMode>`）情况下生效
      * `<maxFetcherNumber>3</maxFetcherNumber>` 在下载文件/观看视频时生效，含义为并发的下载线程数，可以任意调整，建议在1～7之间调整
    * Seattle Plugin
      1. 启用Seattle平台作为proxy服务器有两种方式：
        * 和`AppEngine`平台混合使用(建议)
> > > > > 首先需要安装spac插件，并在etc/hyk-proxy-conf.xml修改启用spac功能：
```
               <proxyEventServiceFactory>
                  SPAC
	       </proxyEventServiceFactory>
```
> > > > > 此外按照[SpecialProxyAutoConfig](http://code.google.com/p/hyk-proxy/wiki/SpecialProxyAutoConfig)说明，配置何种HTTP请求由Seattle平台处理.
        * 独立使用(不推荐，Seattle平台的资源限制非常严格)
> > > > > 在etc/hyk-proxy-conf.xml启用SeattleGENI：
```
             <proxyEventServiceFactory>
                SeattleGENI
	     </proxyEventServiceFactory>
```
      1. 配置Setattle平台目的地址
        * 修改plugin目录下etc/seattle.conf，加入申请得到的ip地址，端口（注意ip是通过seash shell中执行browse得到的ip， 端口是你的seattle account profile中显示的port），eg:
```
            #128.129.41.211:63160
            150.254.212.138:63160
            213.131.1.121:63160
            198.175.122.108:63160
```
    * PHPTunnel Plugin
      1. 启用PHPTunnel也有两种方式：
        * 和`AppEngine`平台混合使用(建议)
> > > > > 方法参照Seattle部分
        * 独立使用
> > > > > 在etc/hyk-proxy-conf.xml启用PHPTunnel：
```
             <proxyEventServiceFactory>
                PHPTunnel
	     </proxyEventServiceFactory>
```
      1. 配置PHP服务端目的地址
        * 修改plugin目录下etc/phptunnel.conf，最后加入部署得到的php url地址，可配置多个，eg:
```
             #The Tunnel Server Port
             LocalTunnelPort=4810

             #List your PHP page urls below
             http://www.xyz.com/tunnel.php
```
      1. PHPTunnel作为https proxy需要满足的条件
        * 根据目前的技术实现，当选择phptunnel最为https proxy时需要当前网络满足以下任一条件：
          * 当前机器的IP为公网IP，即是非下面IP范围`10.0.0.0 ~ 10.255.255.255 172.16.0.0 ~ 172.31.255.255 192.168.0.0 ~ 192.168.255.255`
          * 若不满足上面条件，需要机器所直连的路由/NAT设备支持并开启UPNP功能
    * SPAC Plugin
      1. 安装后启用SPAC需要修改etc/hyk-proxy-conf.xml启用SPAC：
```
             <proxyEventServiceFactory>
                SPAC
	     </proxyEventServiceFactory>
```

> > > > GUI中启用SPAC需要指定SPAC为proxy实现，如下
> > > > > ![http://hyk-proxy.googlecode.com/svn/wiki/images/spac.png](http://hyk-proxy.googlecode.com/svn/wiki/images/spac.png)
      1. 修改proxy选择规则

> > > > 具体描述参考 [SpecialProxyAutoConfig](http://code.google.com/p/hyk-proxy/wiki/SpecialProxyAutoConfig)


# **`AppId`共享计划** #
  * 在hyk-proxy的GAE Plugin的GUI(V0.8.5之后)中提供有一个共享appid的功能，任何人可以据此共享自己的appid， 如下图:
　　  ![http://hyk-proxy.googlecode.com/svn/wiki/images/share.png](http://hyk-proxy.googlecode.com/svn/wiki/images/share.png)
  * 当hyk-proxy的用户由于某些原因（如不知道怎么安装server侧）没有配置自己的appid时，默认情况下，hyk-proxy-client会从服务器上随机获取几个共享appid，然后用匿名用户方式连接`<shareappid>.appspot.com`
  * 共享自己appid的贡献者需要据此考虑设置匿名用户的访问策略，在admin工具中提供有blacklist/traffic/stat工具用于监测/控制匿名用户的行为，具体参考[Authorization](http://code.google.com/p/hyk-proxy/wiki/Authorization)
  * 共享自己appid的贡献者建议在admin工具中执行下面命令设置默认匿名用户策略(root用户登录)：
    * stat on   -- 开启流量统计
    * traffic -u anonymouse -s `*` -r 25000000  -- 对匿名用户访问所有站点限制25m流量
    * stat reports 5  --查看流量前5的统计报告
  * 流量统计结果每天定时自动清零，所以若使用共享appid用户当天无法使用，可以过一天再次尝试

# **注意** #
  * 启用XMPP模式时，由于xmpp server限制，太频繁发送消息会被server拒绝，可采用配置多个XMPP账户规避
  * GUI中有一个Share appid功能， 你可以分享自己的appid到中心服务器，也可以取消分享；中心服务器则随机分派appid给没有安装自己的hyk-proxy-gae-server的用户
  * 分享自己的appid的用户需要注意配置anonymouse用户的访问策略，以免此appid被滥用， 配置用户策略请参考[Authorization](http://code.google.com/p/hyk-proxy/wiki/Authorization)
  * 应用Seattle平台相对于`Google AppEngine`平台的优势：由于Seattle平台提供了非常底层的socket操作，因此可以比较完美实现https支持，上传，以及其他局限于`Google AppEngine`的限制无法实现的功能。
  * PHPTunnel插件在一定条件下也可以实现完美https的tunnel，但若不满足条件是无法实现https的proxy功能
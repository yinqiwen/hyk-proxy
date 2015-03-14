# 简介 #
  * 由于基于GAE的proxy有一些固有的限制（如1m限制， DELETE with body等）或者其他一些proxy实现也有一些具体限制,在这些限制情况下无法完成proxy功能；因此需要一个自动选择proxy机制来简化用户频繁切换proxy的动作
  * 大部分情况下，用户可以用PAC脚本来配置选择合适的proxy；但PAC有自身的一些限制：
    * 只能匹配URL选择，致使无法完美匹配某些受限proxy的受限条件；
    * 一般情况下，有些受限proxy实现限制请求类型（如限制DELETE/TRACE请求），或者限制上传大小等等，这些都是PAC无法表达的
  * 目前基于GAE的proxy的限制
    * 上传/下载不能超过1m(下载在大部分情况下可以绕过)
    * 只支持GET/POST/PUT/DELETE/HEAD 请求
    * 只支持POST/PUT携带body
  * 鉴于PAC的限制，hyk-proxy提供了一个SPAC插件来完成更为强大的类PAC功能

# SPAC插件 #
  * hyk-proxy从V0.8.6开始支持插件，SPAC插件则是第一个插件实现.
  * 用户可以定义任意的第三方proxy client，不仅仅局限于hyk-proxy GAE本身;换句话说， hyk-proxy-client可以仅仅作为一个类PAC实现运行
  * SPAC插件中包含一个CSL脚本语言解释器，用户需要少量修改CSL脚本完成自定义PAC功能
  * SPAC插件请到[hyk-proxy-client插件项目](http://code.google.com/p/p4hpc/downloads/list)下载

# CSL脚本语言 #
  * 此脚本语言非常简单，语法类似C/shell
  * 修改编写CSL脚本注意几点即可：
    * if/elif/else/while后的代码必须用 "{ }" (与C不同)
    * 变量只有两个作用域：全局作用域以及函数作用域（第三方proxy都是全局变量表示）
    * 语法上支持几乎所有运算符，但语义上仅实现了字符串/整数的比较（">，<，！＝，==等"），此由SPAC插件决定
    * “＃ //"都可以作为注释符
    * 脚本的目前入口有三处：onRoutine/firstSelectProxy/reselectProxyWhenFailed, 入口的详细解释请参考后面"配置/脚本"章节
    * 目前支持的内置函数：
```
              1. 打印函数 print($content)：打印到标准输出
                  eg: print("hello,world!");
              2. 获取头域值 getHeader($req, $name) : 获取HTTP请求/响应中具体头域
                  eg： $host = getHeader($request, "Host");
              3. int转化函数 int($str): 将字符串转化为int
                  eg:  $contentLength = int(getHeader($errorRes, "Content-Length"));
              4. 调用外部程序 system($cmd): 执行外部程序
                  eg：$output=system("./my.sh");
              5. 日志函数 log($content): 记录内容到日志文件中
                  eg: log("hello, world");
```


# 安装使用 #
  * 安装非常简单，仅仅需要将在 hyk-proxy-client的plugin目录下解压即可
  * 按照下一节修改配置/脚本后，启动hyk-proxy-client即生效

# 配置&脚本 #
  * 首先启用SPAC需要修改配置文件etc/hyk-proxy-client.conf.xml
```
      <proxyEventServiceFactory>SPAC</proxyEventServiceFactory>
```
  * 其次修改脚本文件spac.csl, spac.csl位于spac插件目录下
    * 第一步定义需要的第三方proxy，需要在spac.csl的开头定义
```
          ##基本只需要定义IP+port，用变量表示
          ##Third proxy client
          $TOR="127.0.0.1:9050";
          $PUFF="127.0.0.1:1984";
          $MRZHANG="127.0.0.1:2010";
          $APJP="127.0.0.1:10000";
```
    * 默认的proxy都是HTTP proxy，若是socks类型需要特殊定义：
```
          $SSHD="socks5:127.0.0.1:7070";
```
    * spac.csl中定义了Routine接口方法onRoutine, 用户控制routine频率，若有定期执行某项动作的需要，可在此定义(如修改选择的proxy地址等):
```
              def onRoutine()
              {
               #Do your routine business here
               #eg: $output=system("./mysh.sh");
               #    log($output);
               #Tell the script engine to invoke this method 10s later
                return 10;
               }
```
    * 第二步修改选择proxy方法，spac.csl定义了两个proxy选择点：
      * 第一次proxy选择, 方法firstSelectProxy
```
          ##此示例方法在GAE的限制条件下选择PUFF作为proxy， 用户可以参考修改
          def firstSelectProxy(protocol, method, url, headers)
          {
              #Let sshd handle https request
              if $protocol=="https" 
              { 
                 return $SSHD;
              }
              #Let phptunnel handle this situation
              ##Google's limit for request body size 1m
              $contentLength = int(getHeader($headers, "content-length"));
              if $contentLength > 1024000
              {
                return $PHP;
              }
     
               # Let seattle handle this situation
              ##Only "GET/POST/PUT/DELETE/HEAD" supported in GAE
              if $method != "GET" && $method != "POST" 
                 && $method != "PUT" && $method != "DELETE"
                 && $method != "HEAD"
              {
                 return $SEATTLE;
              }
     
               #let tor handle this situation
               ##Request with body not allowed in GAE if it's not POST/PUT
              if $contentLength > 0
              {
                  if $method != "POST" && $method != "PUT"
                  {
                    return $TOR;
              }
           }
           #defaultly, GAE is the chosen one
           return $GAE;
         } 
```
      * proxy失败后重新选择（responseCode >=400）， 方法reselectProxyWhenFailed
```
         ##有些情况下proxy实现会出错，提供第二次选择机会（注意需要判断proxy类型，否则有死循环可能）
         def reselectProxyWhenFailed(errorRes,proxy)
         {
             $rescode = getResponseCode($errorRes);
             if $proxy == $GAE && $rescode != 503
             {
                return $SSHD;
             }
             return null;
          }
```
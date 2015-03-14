# 用户/组概念 #

从V0.8.0beta开始引入用户/组概念
  * hyk-proxy的用户组概念，参考了UNIX用户模型，每一个用户属于一个组
  * 在server部署成功后，系统会默认创建root/root，anonymouse/anonymouse用户以及public组
  * 初始创建的root用户密码是随机字符串
  * 只有root用户能够创建删除用户、组
  * 除anonymouse用户外，任何用户可以更改自己的密码
　
# 黑名单 #

  * 目前用户、组支持黑名单权限管理，root用户可以给某一个用户添加/删除访问web site的黑名单
  * 当某一个用户访问的web site在黑名单中时，server直接返回403错误

# root用户 #
  * root用户初始密码必须通过访问web方式才能得到
  * 访问 `http://<appid>.appspot.com`, 点击admin链接按步骤得到root密码

# 用户组管理 #
V0.8.0beta提供了一个admin(admin.sh/admin.bat)工具用于管理用户组管理，工具随client包分发，在`<hyk-proxy-client>/bin`下
  * 启动admin.sh/admin.bat后，依次输入appid， 用户名（一般为root）， 密码进入admin界面
  * 目前admin工具支持useradd/userdel/groupadd/groupdel/passwd/blacklist等多个命令用于管理用户、组，关于各个命令的具体用法参考help命令输出

# 访问控制 #
  * hyk-proxy-client启动时针对一个appid提供用户名、密码（编辑hyk-proxy-client.conf），为空情况下默认为anonymouse:anonymouse
  * hyk-proxy-server对此进行鉴权，并设置访问权限， blacklist
  * blacklist只支持`*`为通配符

# 流量控制 #
  * hyk-proxy V0.8.5引入用户流量控制，在流量统计开启时生效，若某用户访问的站点流量超过限制，则直接返回403错误
  * 流量控制按站点，用户控制，但流量统计结果只是按站点统计
  * 在admin工具中，开启流量统计用stat on; 控制用户流量用traffic命令
  * traffic只支持`*`为通配符

# 注意 #
  * 创建用户时，用户名必须是Email格式，随机创建的密码会发送到此邮箱
  * 对一个用户、组设置的blacklist在用户重新启动hyk-proxy-client时生效
  * blacklist不支持通配符，目前只可以用`*`代表所有site
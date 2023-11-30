# Capoo 网关



## 项目设计

- 运用DDD构建基于Netty的网关核心启动容器
- 运用SPI模式+责任链模式构建插拔式动态过滤器链条，使用caffeine缓存链条提高并发能力
- 采用Disrupotor辅助Netty，支持流量高峰时高可用
- 请求转发响应与插件过滤全异步化处理
- 整合Spring，实现服务快速接入网关
- 增强SPI，实现按需加载，优化缓存安全
- 使用Sentinel进行热点监测，实现热点缓存/降级
- ...



## 项目功能

- 路由谓词工厂：实现多粒度路由谓词工厂，检查请求的时间，支持请求进行全路径匹配、前缀路径匹配路由规则
- 负载均衡：支持权重随机、权重轮询 、最低活跃负载均衡，保障服务预热
- 统一鉴权：支持JWT授权方式 
- 多协议支持：整合后台基于REST，Dubbo等不同协议微服务
- 异构服务接入：自研中间件Tutu通过定时健康检查机制，实现融合异构下游微服务，如Node.js
- 注册中心和服务中心：支持Nacos ZooKeeper多选择，实现节点无状态，配置信息自动同步，动态增量更新
- 多粒度限流：RateLimiter本地限流，Redis实现分布式限流，细颗粒度的限流方式包含服务限流，接口限流
- 熔断降级：Hystrix线程隔离实现熔断保护，定时检查恢复服务
- 多维度监控：采用Prometheus插入链条监控，skywalking分布式链路追踪, 日志审计
- 灰度发布与版本环境控制：控制范围为环境>服务规则>服务版本>灰度 根据环境获取服务规则，一个规则控制多个版本，每个版本对应灰度与非灰度
- 服务异常处理机制：支持Failover、Failfast、Failsafe
- Mock接口：方便前端联调
- 流量安全：配置RBAC权限管控；Basic Auth，JWT，OAUTH2网关统一鉴权；服务隔离，支持内部服务外网隔离；SSL/TLS安全传输；网络攻击防护
- 对于特殊请求可以设置token实现粘性会话
- ...



## 项目计划

- 网络安全 
- ...


## 使用
- 更改Nacos Zookeeper Redis的注册地址（下游服务和Bootstrap里）
- 启动demo后端服务器backend-dubbo-server（Dubbo下游服务）, backend-http-server（SpringMVC下游服务）
- 启动Tutu中间件：gateway-diverse 与 test.js文件（异构微服务）
- 启动gateway-core#Bootstrap

执行参数：
uniqueId 选择的service以及对应的版本 
gray 是否调用灰度服务
method：rpc接口名
parameterTypes：参数类型
arguments：参数值
更多参数请看Config文件

例子：
![image](https://github.com/Wow-wang/Capoo-Api-gateway/assets/59164226/23eb14b0-e1d4-48bd-90b0-4d7e4852417b)
![image](https://github.com/Wow-wang/Capoo-Api-gateway/assets/59164226/caaafd14-4fb6-4a73-b08a-4791d125753f)





## 其他
一名研二计算机学生（正在寻找实习） 邮箱：wangao_2001@foxmail.com
微信 w1035920756

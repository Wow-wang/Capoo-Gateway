# api-gateway
1. 运用领域模型构建基于Netty的网关核心启动容器，支持请求转发响应与插件过滤异步化；实现注册中心和配置中心nacos zookeeper多种选择。
2. 基于Springboot实现自动装配HTTP SpringMVC-REST  Dubbo多种下游服务
3. 运用工厂模式+责任链模式构建插拔式动态过滤器链条，使用caffeine缓存链条提高并发能力，并实现了以下过滤器功能：负载均衡（轮询，权重随机），Ratelimiter实现本地限流和Redis+lua实现分布式限流（支持服务、路径多种限流），路由转发，异常重试处理机制+采用Hystrix线程隔离实现熔断保护，灰度发布与版本环境控制，采用jwt实现用户鉴权，并支持接口mock方便前端联调。
4. 运用Prometheus观察系统指标，skywalking分布式链路追踪，通过线程数与JVM调优，并且采用Disrupotor辅助Netty的MPMC支持流量高峰时高可用


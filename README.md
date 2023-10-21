# Capoo gateway

## Main Features
1. using domain model to build Netty-based gateway core startup container, multi-granular routing predicate factory, request forwarding response and plug-in filtering full asynchronous processing; for the registration center and configuration center with nacos zookeeper a variety of options
2. Based on Springboot to achieve automatic assembly of HTTP Dubbo multiple protocols downstream services, regular health checks to integrate heterogeneous microservices.
3. using the factory pattern + chain of responsibility pattern to build plug-and-play dynamic filter chain, using caffeine cache chain to improve concurrency, and implement the following filter functions: multiple load balancing strategies, Ratelimiter to achieve local flow restriction and Redis + lua to achieve distributed flow restriction (support services, paths, multiple flow restriction), exception retry processing, using the Hystrix thread isolation. Hystrix thread isolation to achieve meltdown protection, grayscale streaming and version environment control, using jwt to achieve user authentication, and support interface mock to facilitate front-end coordination.
4. using Prometheus and other real-time multi-dimensional monitoring, skywalking distributed link tracking. Through the number of threads and JVM tuning , and the use of Disrupotor auxiliary Netty, to support high availability during peak traffic .

## Other Feacher
Support for downstream services to accept only gateway messages(Spring Security)

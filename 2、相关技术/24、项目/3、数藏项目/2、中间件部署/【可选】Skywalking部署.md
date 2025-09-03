# SkyWalking介绍

Apache SkyWalking 是一款开源的分布式系统应用性能监控（APM）和可观测性分析平台，专为微服务、云原生架构及容器化（如 Docker、Kubernetes）环境设计。

核心功能与特性：

- **分布式追踪**与拓扑分析：通过探针自动化采集数据，*展示服务间调用链路的完整拓扑图*，支持跨多云平台的复杂依赖关系可视化。
- **多维度性能监控**：包括**服务实例、端点性能指标**（如响应时间、吞吐量）、JVM监控（内存、线程等）及慢服务检测。
- 低侵入性与高性能：通过**字节码增强技术**实现无代码侵入，采用gRPC协议传输数据，支持百亿级数据处理能力，对系统性能影响极小（采样率100%时吞吐量下降约11%）。
- 多语言支持：提供Java、.NET Core、Node.js、Go等语言的探针，兼容主流框架（如Spring Cloud、Dubbo、gRPC）及服务网格（如Istio）。
- 告警与扩展性：支持自定义告警规则，并可与ElasticSearch、Grafana等工具集成，存储和界面模块均支持灵活扩展。

# SkyWalking部署

下载SkyWalking：从[Apache SkyWalking官方页面](https://skywalking.apache.org/downloads/)下载最新版本的SkyWalking APM压缩包。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508251734421.png)

所谓`APM`其实是`Application Performance Management`的缩写，其中包括了`OAP`和`web ui`。
- `OAP`（Observalibity Analysis Platform）是**服务端的分析平台**，负责接收、存储、分析`Agent`上报的数据，并提供查询能力。**具体功能**：
- **数据接收**：通过`gRPC/HTTP`接口接收Agent上报的Trace、Metric、Topology等数据。
- **流式分析**：实时聚合、计算指标（如P99延迟、错误率）。
- **存储持久化**：将数据存储到后端数据库（如Elasticsearch、H2、MySQL等）。
- **拓扑构建**：根据调用关系生成服务依赖图。
- **告警触发**：基于预定义的规则（如响应时间超阈值）触发告警。
- **数据查询API**：为UI Dashboard（SkyWalking UI）或其他工具提供查询接口。

解压压缩包：将下载的压缩包解压到你选择的目录。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508251746421.png)

**1、启动OAP服务器**：在解压后的目录中，进入bin目录并运行启动脚本来启动OAP服务器。
- 对于Linux或MacOS，运行：`./startup.sh`
- 对于Windows，运行：`startup.bat`
**2、启动Web UI**：同样在bin目录下，上述脚本通常**同时也启动了Web UI服务**，你可以通过浏览器访问 http://localhost:8080 来查看。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508251747009.png)

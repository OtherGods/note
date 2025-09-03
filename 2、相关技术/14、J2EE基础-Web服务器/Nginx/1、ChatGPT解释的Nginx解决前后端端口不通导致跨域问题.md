
Nginx 服务器运行时需要指定端口。在大多数情况下，Nginx 服务器会监听 HTTP 请求的默认端口 80（或者 HTTPS 请求的默认端口 443），而你的前端服务器和后端服务器运行在不同的端口（例如 8081 和 8088，假设前后端、Nginx服务器在同一台机器上）。

### 详细解释 Nginx 配置中的端口

#### Nginx 配置示例

以下是一个详细的 Nginx 配置示例，用于代理前端服务器和后端服务器：

```nginx
server {
    listen 80;  # 监听80端口
    server_name your-domain.com;  # 你的域名或IP地址

    # 代理前端服务器
    location / {
        proxy_pass http://localhost:8081;  # 前端服务器地址
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 代理后端服务器
    location /api/ {
        proxy_pass http://localhost:8088;  # 后端服务器地址
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 运行逻辑

- **Nginx 服务器**：监听端口 80（或 443），这也是用户访问的端口。
- **前端服务器**：监听端口 8081。
- **后端服务器**：监听端口 8088。

用户访问 `http://your-domain.com` 时，Nginx 服务器会将请求代理到前端服务器 `http://localhost:8081`。当前端代码发起 API 请求到 `/api/...` 路径时，Nginx 会将这些请求代理到后端服务器 `http://localhost:8088`。

### 配置的核心思想

1. **Nginx 监听一个固定端口**（例如 80 或 443），用户通过该端口访问服务器。
2. **Nginx 根据路径将请求代理到不同的服务器和端口**。前端请求（根路径 `/`）代理到前端服务器端口，API 请求（以 `/api/` 开头）代理到后端服务器端口。

### 示例流程

1. 用户访问 `http://your-domain.com`。
2. Nginx 接收请求，代理到前端服务器 `http://localhost:8081`。
3. 前端服务器返回页面，页面中可能包含 API 请求，例如 `/api/data`。
4. 前端代码发送 API 请求到 `http://your-domain.com/api/data`。
5. Nginx 接收 `/api/data` 请求，代理到后端服务器 `http://localhost:8088/api/data`。
6. 后端服务器处理请求并返回数据，通过 Nginx 返回给前端。

### 总结

通过这种配置，Nginx 作为一个反向代理服务器，统一处理来自不同端口的请求，从而解决跨域问题。用户只需访问 Nginx 服务器的域名和端口（例如 80 或 443），而不需要知道实际的前端和后端服务器端口。

**配置完成后，重启 Nginx 服务器以应用更改：**

```sh
sudo systemctl restart nginx
```

或者（取决于你的系统）：

```sh
sudo service nginx restart
```

云效代码管理 （Codeup）的仓库支持 HTTP(S) 和 SSH 两种访问协议，SSH 协议可以实现安全的免密认证，且性能比 HTTP(S) 协议更好。本文介绍如何在 Codeup 中配置和使用 SSH 密钥进行代码推拉。

## **前提条件**

在使用 SSH 协议操作代码库前，请生成并上传你的 SSH 公钥，完成 SSH 公钥和云效账号的对应。

通过 SSH 协议访问 Codeup，需要满足如下条件。

- 本机已安装 Git（安装教程参见[安装Git](https://help.aliyun.com/document_detail/153800.html#topic-2405645)）并保证版本大于 1.9（通过`git --version`可获取本地的版本）。
- 本机需要安装 OpenSSH 客户端（GNU/Linux, macOS, 或 Windows 10 已内置 OpenSSH）。
- SSH 尽量保持最新，6.5之前的版本由于使用 MD5 签名，可能存在安全问题。

**重要**

如果你是 Windows 用户，在使用 Git 命令时，请**使用** [**WSL**](https://docs.microsoft.com/en-us/windows/wsl/install)**（需要Windows10或以上），或使用** [**Git Bash**](https://gitforwindows.org/)。

## **背景信息**

Codeup 支持的 SSH 加密算法类型如下所示：

| 算法类型             | 公钥             | 私钥         |
| ---------------- | -------------- | ---------- |
| **ED25519 （推荐**） | id_ed25519.pub | id_ed25519 |
| RSA （不推荐）        | id_rsa.pub     | id_rsa     |

## 步骤一：查看已存在的 SSH 密钥

在生成新的 SSH 密钥前，请先确认是否需要使用本地已生成的SSH密钥，SSH 密钥对一般存放在本地用户的根目录下。

Linux、Mac 请直接使用以下命令查看已存在的公钥，Windows 用户在 [WSL](https://docs.microsoft.com/en-us/windows/wsl/install)（需要 windows10 或以上）或 [Git Bash](https://gitforwindows.org/)下使用以下命令查看已生成公钥：

**ED25519 算法**

```plaintext
cat ~/.ssh/id_ed25519.pub
```

**RSA 算法**

```plaintext
cat ~/.ssh/id_rsa.pub
```

如果返回一长串以 ssh-ed25519 或 ssh-rsa 开头的字符串, 说明已存在本地公钥，你可以跳过步骤二**生成 SSH 密钥**，直接操作步骤三。

## 步骤二：生成 SSH 密钥

若步骤一未返回指定内容字符串，表示本地暂无可用 SSH 密钥，需要生成新的 SSH 密钥，请按如下步骤操作：

1. 访问终端（ Windows 请使用 [WSL](https://docs.microsoft.com/en-us/windows/wsl/install)或 [Git Bash](https://gitforwindows.org/)），运行`ssh-keygen -t`。
2. 输入密钥算法类型和可选的注释。

注释会出现在`.pub`文件中，一般可使用邮箱作为注释内容。

- 基于`ED25519`算法，生成密钥对命令如下：

```plaintext
ssh-keygen -t ed25519 -C "<注释内容>"
```

- 基于`RSA`算法，生成密钥对命令如下：

```plaintext
ssh-keygen -t rsa -C "<注释内容>"
```

3. **点击回车，** 选择 SSH 密钥生成路径。

- 以 ED25519 算法为例，默认路径如下：

```plaintext
Generating public/private ed25519 key pair.
Enter file in which to save the key (/home/user/.ssh/id_ed25519):
```

密钥默认生成路径：`/home/user/.ssh/id_ed25519`，公钥与之对应为：`/home/user/.ssh/id_ed25519.pub`。

- 以 RSA 算法为例，默认路径如下：

```plaintext
Generating public/private rsa key pair.
Enter file in which to save the key (/home/user/.ssh/id_rsa):
```

私钥默认生成路径：`/home/user/.ssh/id_rsa`，公钥与之对应为：`/home/user/.ssh/id_rsa.pub`。

4. 设置一个密钥[口令](https://www.ssh.com/academy/ssh/passphrase)。

```plaintext
Enter passphrase (empty for no passphrase):
Enter same passphrase again:
```

口令默认为空，你可以选择使用口令保护私钥文件。如果你不想在每次使用 SSH 协议访问仓库时，都要输入用于保护私钥文件的口令，可以在创建密钥时，输入空口令。

5. 点击回车，完成密钥对创建。

**警告**

密钥用于鉴权，请谨慎保管。公钥文件以 .pub 扩展名结尾，可以公开给其他人，而没有 .pub 扩展名的私钥文件不要泄露给任何人！

## 步骤三：拷贝公钥

除了在命令行打印出已生成的公钥信息手动复制外，可以使用命令拷贝公钥到粘贴板下，请参考操作系统使用以下命令进行拷贝：

Windows（**在**[**WSL**](https://docs.microsoft.com/en-us/windows/wsl/install)**或**[**Git Bash**](https://gitforwindows.org/)**下**）:

```plaintext
cat ~/.ssh/id_ed25519.pub | clip
```

Mac:

```plaintext
tr -d '\n' < ~/.ssh/id_ed25519.pub | pbcopy
```

GNU/Linux (requires xclip):

```plaintext
xclip -sel clip < ~/.ssh/id_ed25519.pub
```

## 步骤四：在 Codeup 上设置公钥

1. 登录云效 [Codeup 页面](https://codeup.aliyun.com/)，在页面右上角选择个人设置>SSH 公钥。
2. 添加生成的 SSH 公钥信息。

- SSH 公钥内容。

**说明**

请完整拷贝本机中公钥从 ssh- 开始直到邮箱为止的内容。

- 公钥标题：支持自定义公钥名称，用于区分管理。
- 作用范围：设置公钥的作用范围，包括读写或是只读，若设置为只读，该公钥只能用于拉取代码，不允许推送。
- 过期时间：设置公钥过期时间，到期后公钥将自动失效，不可使用。
![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202408251053947.png)

3. 单击**添加**保存设置，即完成 SSH 公钥的设置。

## 常见问题

### **本地存在多个密钥时，如何根据目标平台自动选择用于认证的密钥？**

当本地存在多个密钥，如果不设置认证规则，本机将随机选择一个密钥用于认证，可能造成认证失败。

因此，在如下场景中，需要自行定义认证密钥的路径：

- 本地存在多个密钥对应云效的不同账号。
- 本地存在多个密钥对应不同的代码平台（GitLab，GitHub，云效等）。

#### **定义认证密钥路径规则**

打开本地终端，按如下格式编辑`~/.ssh/config`文件，如 Windows 平台请使用[WSL](https://docs.microsoft.com/en-us/windows/wsl/install)（Windows10或以上）或 [Git Bash](https://gitforwindows.org/)：

```plaintext
# Codeup 示例用户1
HostName codeup.aliyun.com
  PreferredAuthentications publickey
  IdentityFile ~/.ssh/id_ed25519
  
# Codeup 示例用户2，设置别名 codeup-user-2
Host codeup-user-2
HostName codeup.aliyun.com
  PreferredAuthentications publickey
  IdentityFile ~/.ssh/codeup_user_2_ed25519

# GitLab 平台
HostName gitlab.com
  PreferredAuthentications publickey
  IdentityFile ~/.ssh/gitlab_ed25519
```

按照上述配置，使用SSH协议访问时，SSH 客户端会使用文件指定的密钥进行认证，实现访问不同平台或同一平台的不同账号使用本地不同的 SSH 密钥进行认证。

- 访问 Codeup ，由于 HostName 一致，使用别名进行区分使用不同的密钥。
- 访问 GitLab，根据 HostName 进行区分使用不同的密钥。

```plaintext
# 访问 Codeup，将使用 ~/.ssh/id_ed25519.pub 密钥
git clone git@codeup.aliyun.com:example/repo.com

# 以 codeup-user-2 别名访问 Codeup 时，将使用 ~/.ssh/codeup_user_2_ed25519 密钥 
git clone git@codeup-user-2:example/repo.com

# 访问 GitLab 平台，将使用 ~/.ssh/gitlab_ed25519 密钥
git clone git@gitlab.com:example/repo.com
```


转载自[配置 SSH 密钥](https://help.aliyun.com/document_detail/153709.html)

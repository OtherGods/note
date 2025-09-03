大家好，我是二哥呀。

今天由我来给大家讲一下技术派是如何渲染 markdown 的，主要涉及到两部分，一部分是编辑文章的时候，另外一部分是显示文章详情的时候。

我先截图说明一下在什么位置，这是编辑文章的时候：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031705288.png)

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031707990.png)

技术派的编辑器支持：

- LaTeX 科学公式（数学家和计算机科学家的写作工具）
- 任务列表（ToDoList）
- 识别和解析HTML标签（上图中的 iframe 视频播放器）
- 代码高亮（可复制）
- 表格（涉及到 markdown 转 HTML 的后端插件）
- 常规的 markdown 语法

这是展示文章详情的时候：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031709873.png)

几乎当下主流的文章编辑器都支持 markdown，并且很早就取代了富文本的霸主地位，如果大家想开发一款内容平台的话，markdown 转 HTML 基本上是绕不开的一个环节，那技术派是如何做到的呢？

# 1、文章编辑页
## 1.1、插件

技术派的前端用了两款插件，用户端是 editormd，admin 端用的掘金的插件 bytemd。

- editormd：https://pandao.github.io/editor.md/index.html
- bytemd：https://github.com/bytedance/bytemd

Editor.md 是一款开源的、可嵌入的 Markdown 在线编辑器（组件），基于 CodeMirror、jQuery 和 Marked 构建。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031711971.png)

代码应用路径：`/paicoding/paicoding-ui/src/main/resources/templates/views/article-edit`，见下图。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031711141.png)

ByteMD 是掘金社区开源的 Markdown 编辑器组件，可以优雅地集成在 React、Vue 和 Angular 框架中。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031712615.png)

代码应用路径：`/paicoding-admin/src/views/article/edit`
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031713632.png)

## 1.2、Editor.md的应用

第一步，下载 Editor.md 的源文件

https://github.com/pandao/editor.md/releases

最新 release 版本是 v1.5.0，技术派在此基础上进行了很多自定义。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031713267.png)

比如说图片上传，比如说 LaTeX 科学公式，我都直接在源码上进行更改了，因为原有的 Editor.md 支持的并不友好。大家需要的话，可以直接从技术派的源码中下载。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031714795.png)

第二步，把 CSS 和 JS 文件引入到项目当中
```html
<link rel="stylesheet" href="/editormd/css/editormd.css" th:href="${global.siteInfo.oss + '/editormd/css/editormd.css'}" />
<link rel="stylesheet" href="/css/views/article-edit.css" th:href="${global.siteInfo.oss + '/css/views/article-edit.css'}" />
<script src="/editormd
```

其中的 th:href 和 th:src 是为了做 CDN，技术派会把静态文件存入 OSS，然后通过 CDN 进行访问，这样可以加快静态文件的访问速度。

第三步，启用 Editor.md

首先，需要在页面中加入一个 DIV，里面放一个隐藏的 textarea，用来保存编辑的时候的 markdown。
```html
<div class="form-group" id="paiEditor">
  <textarea
    style="display: none"
    th:text="${vo.article != null ? vo.article.content: ''}"
  ></textarea>
</div>
```

然后在 JavaScript 中初始化 Editor.md。
```javascript
const oss = [[${global.siteInfo.oss}]];
const jspath = oss + '/editormd/lib/';
let simplemde = editormd("paiEditor", {
  path: jspath,
  width: "100%",
  height: calculateEditorHeight(),
  katexURL : {
      css : oss + "/katex/katex",
      js : oss + "/katex/katex",

  }, // KaTeX 的基本 URL
});
```

①、oss 为技术派位静态文件设置的 CDN 路径，本地环境时为空。

②、jspath 也就是我们前面引入的 Editor.md 路径，里面有用到的一些插件，比如说 markdown 实时预览、流程图、代码高亮等等。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031716853.png)

③、width 和 height 主要用来指定 Editor.md 的宽度和高度。

④、katexURL 用来设置编辑器所支持的 katex 科学公式的插件路径。

第四步，保存 markdown 到数据库

请求会调用后端的 ArticleRestController 控制器的 post 接口。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031716421.png)

调用 ArticleDao 的 saveArticleContent 方法：
```java
public Long saveArticleContent(Long articleId, String content) {
    ArticleDetailDO detail = new ArticleDetailDO();
    detail.setArticleId(articleId);
    detail.setContent(content);
    detail.setVersion(1L);
    articleDetailMapper.insert(detail);
    return detail.getId();
}
```

最终 markdown 内容会被保存在 article_detail 表中。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031717783.png)

## 1.3、ByteMD的应用

第一步，安装 @bytemd/react，技术派 admin 端用的 React。
```shell
npm install @bytemd/react
```

第二步，引入 ByteMD 组件和其组件。
```javascript
import gfm from '@bytemd/plugin-gfm'
import { Editor } from '@bytemd/react'
import zhHans from 'bytemd/locales/zh_Hans.json';

import 'bytemd/dist/index.css';
import 'juejin-markdown-themes/dist/juejin.css';
```

①、gfm 插件支持 ToDoList、表格、删除线、自动识别连接等，还有其他一众插件，比较重要的有代码高亮（@bytemd/plugin-highlight）、科学公式（@bytemd/plugin-math）、图片缩放（@bytemd/plugin-medium-zoom）等，需要哪一个就直接先 npm install，然后引入，像 gfm 那样。

> GitHub Flavored Markdown（简称 GFM）是 GitHub 对原始 Markdown 语法的一种扩展和变体。

![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031719061.png)

②、Editor bytemd 的主体。

③、zhHans 汉化。

④、bytemd/dist/index.css bytemd 的样式表。

⑤、juejin.css 掘金风格的主题。

第三步，启用 bytemd 插件，代码很简单，大家一看就懂，value 就是编辑的时候显示原有的内容，plugins 就是支持的插件，locale 就是汉化，onChange 事件就是当编辑的时候预览区域能给着更新，同步 content 的内容，handleChange 就是内容变化的时候保存到 Form 表单。

第三步，启用 bytemd 插件，代码很简单，大家一看就懂，value 就是编辑的时候显示原有的内容，plugins 就是支持的插件，locale 就是汉化，onChange 事件就是当编辑的时候预览区域能给着更新，同步 content 的内容，handleChange 就是内容变化的时候保存到 Form 表单。
```js
const plugins = [
	gfm(),
	highlight(),
	gemoji(),
	math(),
	mediumZoom(),
	// Add more plugins here
]

// 文章内容
const [content, setContent] = useState<string>('');


<Editor
    value={content}
    plugins={plugins}
    locale={zhHans}
    onChange={(v) => {
        // 右侧的预览更新
        setContent(v);
        handleChange({ content: v });
    }}
/>
```

第四步，传递到后端进行保存。
```js
// 编辑或者新增时提交数据到服务器端
const handleSubmit = async () => {
// 又从form中获取数据，需要转换格式的数据
const { articleId, cover, content, tagIds } = form;

const values = await formRef.validateFields();
console.log("handleSubmit 时看看form的值 values", values);

const { status: successStatus } = (await saveArticleApi(newValues)) || {};
const { code, msg } = successStatus || {};
if (code === 0) {
    message.success("成功");
    // 返回文章列表页
    goBack();
} else {
    message.error(msg || "失败");
}
};
```

ArticleSettingRestController 控制器的 save 接口。
```java
@Permission(role = UserRole.ADMIN)
@PostMapping(path = "save")
public ResVo<String> save(@RequestBody ArticlePostReq req) {
    if (NumUtil.nullOrZero(req.getArticleId())) {
        // 新增文章
        this.articleWriteService.saveArticle(req, ReqInfoContext.getReqInfo().getUserId());
    } else {
        this.articleWriteService.saveArticle(req, null);
    }
    return ResVo.ok("ok");
}
```

也就是说，保存的时候，我们会把原样的 markdown 格式的文本保存到数据库，content 的类型是一个 longtex，4294967295个字节，非常非常大了，我写一篇文章最多也就是几万字吧，一个中文 3 个字节，大家可以算一下。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031720743.png)

# 2、文章详情页

大家都知道，网页一般都是 HTML 格式，那 markdown 的格式如何在页面上正确显示出来呢？

## 2.1、flexmark

首先，我们的页面用的是 Thymeleaf，可以直接用 th:utext 属性，它会把变量内容作为 HTML 渲染，而不是仅仅作为文本。
```html
<div id="articleContent" class="article-content" th:utext="${article.content}"></div>
```

后端用 flexmark 对 markdown 文本进行 HTML 转义，放在 MarkdownConverter 类中。
```java
public class MarkdownConverter {
    // 定义一个静态方法，将 Markdown 文本转换为 HTML
    public static String markdownToHtml(String markdown) {
        // 创建一个 MutableDataSet 对象来配置 Markdown 解析器的选项
        MutableDataSet options = new MutableDataSet();

        // 添加各种 Markdown 解析器的扩展
        options.set(Parser.EXTENSIONS, Arrays.asList(
                AutolinkExtension.create(),     // 自动链接扩展，将URL文本转换为链接
                EmojiExtension.create(),        // 表情符号扩展，用于解析表情符号
                GitLabExtension.create(),       // GitLab特有的Markdown扩展
                FootnoteExtension.create(),     // 脚注扩展，用于添加和解析脚注
                TaskListExtension.create(),     // 任务列表扩展，用于创建任务列表
                TablesExtension.create()));     // 表格扩展，用于解析和渲染表格

        // 使用配置的选项构建一个 Markdown 解析器
        Parser parser = Parser.builder(options).build();
        // 使用相同的选项构建一个 HTML 渲染器
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // 解析传入的 Markdown 文本并将其渲染为 HTML
        return renderer.render(parser.parse(markdown));
    }
}
```

flexmark 是一个 markdown 解析器，可以把 markdown 转成 HTML，GitHub 开源地址如下：

> https://github.com/vsch/flexmark-java

用法也很简单，先引入到 pom.xml。
```xml
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
</dependency>
```

然后配置插件 MutableDataSet，比如说表格、任务列表等，还有很多很多，GitHub 的 readme 里写的很详细，大家可以去看看。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031722487.png)

## 2.2、KaTeX

比较遗憾的是，flexmark 对科学公式支持的不是很友好，尽管里面融合了 GitLab 特有的 Markdown 扩展，但依然支持有限。

> https://github.com/vsch/flexmark-java/issues/161

那为了让技术派支持更更好的支持科学公式，我把 katex 引入了进来，就现阶段来说，KaTeX 和 MathJax 是两种比较流行的科学公式库，一开始我打算使用 MathJax，结果发现比较难用，和 editormd 也不兼容。

KaTeX 通常被认为比 MathJax 更快，特别是在处理大量简单数学方程时。
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031722057.png)

> KaTeX官网：https://katex.org/

使用方式也非常简单，我这里帮大家简单介绍一下。

第一步，通过下面这个链接下载 KaTeX。

> https://github.com/KaTeX/KaTeX/releases

技术派是放在下面这个路径 paicoding/paicoding-ui/src/main/resources/static/katex 下：
![1.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202410031724881.png)

第二步，在页面上引入 css 和 js。
```js
<link href="/katex/katex.css" th:href="${global.siteInfo.oss + '/katex/katex.css'}" rel="stylesheet" >
<script defer src="/katex/katex.js" th:src="${global.siteInfo.oss + '/katex/katex.js'}"></script>
<script defer src="/katex/contrib/auto-render.js" th:src="${global.siteInfo.oss + '/katex/contrib/auto-render.js'}"></script>
<script src="/js/mathjax-config.js" th:src="${global.siteInfo.oss + '/js/mathjax-config.js'}" defer></script>
```

katex.css 和 katex.js 是必须的，auto-render.js 用来完成自动渲染，并且自定义特殊的分隔符，比如说，默认的 KaTeX 是不支持 行内的 `$$` 分隔符，我们可以通过这种方式自定义。

放在了 mathjax-config.js 这个文件中。
```js
const katexRender = function (tex, delimiter) {
    renderMathInElement(tex || document.body, {
        // 这里定义了一组自定义的定界符，用于识别和渲染数学公式
        delimiters: [
        // 每个定界符对象定义了左边界、右边界和显示模式
            {left: '$$', right: '$$', display: true},
            {left: '$', right: '$', display: false},
            {left: "\\[", right: "\\]", display: true}
        ],
        throwOnError : false
    });
}

document.addEventListener("DOMContentLoaded", function() {
    katexRender();
});
```

katexRender 是我们自定义的一个函数，用于在网页上渲染 KaTeX 数学公式，并在文档加载完成后自动执行这个函数。

参数是一个 DOM 元素，指定了在哪个元素内渲染数学公式。如果没有提供，将默认为 document.body。

delimiters 数组定义了一系列定界符规则，用于 KaTeX 解析数学公式。每个规则包含左边界 left、右边界 right 和是否以显示模式 display 渲染（true 的话通常会在新行显示）。

renderMathInElement 是 auto-render.js 自定义的函数，用于渲染页面上的数学公式。

那基本上现在，页面上的科学公式就能正常显示了，比如说[《二哥的 LeetCode 刷题笔记》](https://paicoding.com/column/7/5)中的时间复杂度，都可以正常显示了。

# 3、小结

到此为止，技术派的 markdown 渲染 HTML 的功能也就介绍完了，其中用到了很多开源的组件，比如说后端的 flexmark，前端的 editormd 和 bytemd，以及 katex 等。

有了这些开源组件，那基本上 markdown 常用的表格、自动识别连接、科学公式、流程图、emoji 表情等，都能正常显示了。

冲。



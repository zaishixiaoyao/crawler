# crawler

**这个项目是使用爬虫对某一个APP进行爬取，爬取内容通过数学建模提取保存到数据库，最后将APP的内容显示在网页**

**使用的框架**：

1. 基础架构：Spring Boot 2.x + MyBatis 3 + Tomcat + MySQL

2. 底层技术：Charles + OKHttp 3 + Gson 2 + OGNL

3. 表示层技术：LayUI + Thymeleaf + TCPlayer

**开发环境**：

1. 操作系统：Window 10 旗舰版

2. 开发包: Jdk 1.8.0_131

3. IDE: Idea 2017.3.2 Ultimate 、Navicat 12

4. 运行环境：Tomcat 8.x(Embed) + MySQL 5.7

5. 基础架构：Spring Boot 2.x + MyBatis 3.x
    
**实现功能**：

1. 对抓取的数据进行重构解析，保存到自己的数据库

2. 连接超时自动重试，最多10次

3. 每小时自动抓取一次APP最新数据

4. 提供后台管理界面，提供后台查询，随时可以删除违规的记录

5. 对后台的数据可以预览，界面模仿AP

**数据库建模**：

一共8张表，这里只是列举了其中一张表，表的数据都存在于 · /resource/sql · 文件夹中




**后台界面**：

![Aaron Swartz](https://raw.githubusercontent.com/zaishixiaoyao/MarkdownPhotos/master/%E5%86%85%E5%AE%B9%E7%AE%A1%E7%90%86%E5%90%8E%E5%8F%B0%E7%95%8C%E9%9D%A2.png)


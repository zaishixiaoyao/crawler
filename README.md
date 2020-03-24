# crawler

**当我在使用爬虫软件（Charles）对某一个手机APP进行爬取时，发现url中存有大量的json数据，这些数据都是APP中的内容。因此，我做了这个小项目将json数据中的APP内容在电脑端中表现出来。**

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

一共8张表，这里只是列举了其中一张表，表的数据都存在于 ```/resource/sql``` 文件夹中

![Aaron Swartz](https://raw.githubusercontent.com/zaishixiaoyao/MarkdownPhotos/master/%E6%95%B0%E6%8D%AE%E5%BA%93content%E8%A1%A8%E5%BB%BA%E6%A8%A1.png)

![Aaron Swartz](https://raw.githubusercontent.com/zaishixiaoyao/MarkdownPhotos/master/content%E8%A1%A8.png)

**后台界面**：

![Aaron Swartz](https://raw.githubusercontent.com/zaishixiaoyao/MarkdownPhotos/master/%E5%86%85%E5%AE%B9%E7%AE%A1%E7%90%86%E5%90%8E%E5%8F%B0%E7%95%8C%E9%9D%A2.png)

package com.zaishixiaoyao.crawlerbsbdj.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaishixiaoyao.crawlerbsbdj.common.OgnlUtils;
import com.zaishixiaoyao.crawlerbsbdj.entity.*;
import com.zaishixiaoyao.crawlerbsbdj.mapper.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service //处理核心业务逻辑
@Transactional //写在类上，默认所有方法均开启事务。方法执行成功，自动事务提交，如果遇到RuntimeException及其子类，则进行回滚
public class CrawlerService {

    @Resource //这里，依赖注入的本质是让service可以直接调用xml配置好的sourceMapper对象，如果不加这个注解，本质上程序只是定义了一个变量，这个变量的属性和方法都是接口自身的，没有经过xml进行方法上的扩展
    private SourceMapper sourceMapper;
    @Resource
    private ContentMapper contentMapper;
    @Resource
    private VideoMapper videoMapper;
    @Resource
    private ImageMapper imageMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private ForumMapper forumMapper;

    @Value("${app.crawler.urls}")
    private String urls;//爬虫地址

    Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    /**
     * 抓取source，并存入t_source表中
     */
    public void crawl(Map context , String template , String np , Integer channelId){

        String url = template.replace("{np}" , np);

        //使用OKHttp的方法很简单，只需要遵守3步
        //1. 创建OKHttp对象
        OkHttpClient client = new OkHttpClient();

        //2.构建请求,设置要访问的url
        Request.Builder builder = new Request.Builder().url(url);
        //增加请求头,模拟Chrome浏览器
        builder.addHeader("User-Agent" , "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");

        //3. 发送请求
        //创建已设置好的请求对象
        Request  request= builder.build();
        String retText = null;
        //考虑到抓取的失败，总共进行十次抓取，只要抓取成功一次就跳出循环，若抓取失败则进行下一次抓取直至抓满十次
        for(int i = 0 ; i < 10 ; i++) {
            try {
                Response response = client.newCall(request).execute();//发送请求,返回响应对象
                retText = response.body().string();//获取响应的数据
                break;//抓取成功后跳出循环
            } catch (IOException e) {
                logger.warn("爬虫连接超时，正在准备第{}次重试，URL:{}", (i+1) ,url);//报错打印日志
                continue;
            }
        }
        //10次抓取都失败，程序中断
        if(retText == null){
            logger.error("爬虫抓取失败，原因：连接超时,URL:" + url);
        }

        //至此，okhttp工作结束，通过url获得完整json数据，下面的工作是从json数据中获取到url的改变值

        //将json数据转换为Map对象
        Gson gson = new Gson();
        Map result = gson.fromJson(retText , new TypeToken<Map>(){}.getType());

        //从Map对象中获取url改变值
        Double dnp =   (Double)((Map)result.get("info")).get("np");//从Map对象中获取改变值，这里的Map是定义result.get()的类型
        String strNP = new DecimalFormat("###########").format(dnp);

        //保存原始数据数据，插入t_source表中
        Source source = new Source();
        source.setChannelId(channelId);//频道编号
        source.setCreateTime(new Date());//创建时间（国际时间）
        source.setResponseText(retText);//json数据
        source.setState("WAITING");//等待被处理的状态
        source.setUrl(url);//爬取的链接
        sourceMapper.insert(source);

        //当抓取成功的时候，给计数器加20条
        logger.info("数据抓取成功,NP:{},URL:{} " , strNP , url );
        int count = (int)context.get("count");
        count = count + 20;
        context.put("count" , count);
        //当累计抓取了100条数据的时候，即5次就终止爬虫
        if(count >= 100){
            return;
        }

        //只抓取最近的100条数据，每小时抓一次。大约百思每小时更新3~5条数据，所以抓最近的100条足够了
        //利用递归进行数据抓取
        crawl(context , template , strNP, channelId);
    }

    /**
     * 运行抓取source的程序
     */
    public void crawlRunner(){

        // 按需要抓取的模块url，进行分组（这里按推介、视频、图片、笑话、排行进行分类）
        String[] templates = urls.split(",");
        for(int i = 0 ; i < templates.length ; i ++) {
            logger.info("正在抓取第{}个模块的数据" , (i+1));
            String template = templates[i];//从templates数组中去取出对应模块的url

            Map context = new HashMap();
            context.put("count", 0);//计数器，记录抓取的总数

            this.crawl(context, template, "0" , (i+1));
        }
    }

    /**
     * 对数据进行etl处理：执行createContent方法
     */
    public void etl(){
        //查询所有等待处理的数据源
        List<Source> sources = sourceMapper.findByState("WAITING");
        for(Source source : sources){
            String json = source.getResponseText();
            //将字符串转换为Map对象
            Map<String,Object> root = new Gson().fromJson(json , new TypeToken<Map>(){}.getType());
            //获取每页20条数据的集合
            List<Map<String,Object>> list =  OgnlUtils.getListMap("list" , root);
            for(Map contentMap : list){
                //将每条内容保存到数据库
                createContent(source , contentMap);
            }

            //对处理完成的Source对象更新状态为Processed
            source.setState("PROCESSED");
            sourceMapper.updateByPrimaryKey(source);
        }
    }

    /**
     * 将t_source表的内容存入其他表
     */
    public void createContent(Source source , Map contentMap) {

        //内容编号
        Long contentId = OgnlUtils.getNumber("id", contentMap).longValue();

        //推介与图片、视频、笑话（文本）三个模块间可能发生内容冲突。因此，需要规定一个内容只允许插入一次，不允许重复
        if (contentMapper.selectByPrimaryKey(contentId) != null) {
            logger.info("Content ID:{} ，内容已存在，此内容被忽略导入", contentId);
            return;
        }

        //t_content表：common内容（这里有两个属性没设置：）
        Content content = new Content();
        //频道编号（1是推介、2是视频【video】、3是图片【image/gif】、4是笑话【text】，这个值不是url自带的）
        content.setChannelId(source.getChannelId().longValue());
        //状态 4代表通过
        content.setStatus(OgnlUtils.getNumber("status", contentMap).intValue());
        //评论总数
        content.setCommentCount(OgnlUtils.getNumber("comment", contentMap).intValue());
        //收藏数
        content.setBookmarkCount(OgnlUtils.getNumber("bookmark", contentMap).intValue());
        //正文或标题（text是每一个模块都有的）
        content.setContentText(OgnlUtils.getString("text", contentMap));
        //点赞数量
        content.setLikeCount(OgnlUtils.getNumber("up", contentMap).intValue());
        //踩 数量
        content.setHateCount(OgnlUtils.getNumber("down", contentMap).intValue());
        //分享链接（点击内容下方的分享后，生成的链接）
        content.setShareUrl(OgnlUtils.getString("share_url", contentMap));
        //分享数量
        content.setShareCount(OgnlUtils.getNumber("forward", contentMap).intValue());
        //过审时间
        content.setPasstime(OgnlUtils.getString("passtime", contentMap));
        //内容类型（video、image、gif）
        content.setContentType(OgnlUtils.getString("type", contentMap));
        //内容编号（APP内部记录的内容号码）
        content.setContentId(OgnlUtils.getNumber("id", contentMap).longValue());
        //数据源编号（t_source表中url的编号,主键，这个值不是url自带的）
        content.setSourceId(source.getSourceId());
        //创建时间（url的创建时间，这个值不是url自带的）
        content.setCreateTime(new Date());

        contentMapper.insert(content);//保存content：将content对象内容存入数据库（这里有两个属性没设置：uid、forum_id）

        //t_video表：video内容（这里有四个属性没设置：long_picture、thumbnail_link、thumbnail_height、thumbnail_width）
        if(content.getContentType().equals("video")){
            Video vid = new Video();
            //播放数量
            vid.setPlayfcount(OgnlUtils.getNumber("video.playfcount", contentMap).intValue() );
            //视频高
            vid.setHeight(OgnlUtils.getNumber("video.height", contentMap).intValue());
            //视频宽度
            vid.setWidth(OgnlUtils.getNumber("video.width", contentMap).intValue());
            //视频在线播放地址，默认以第一个为准（video中存在两个不同的链接，但是两个链接指向的视频内容相同）
            List<String> videoUrl = OgnlUtils.getListString("video.video", contentMap);
            vid.setVideoUrl(videoUrl.size() > 0 ? videoUrl.get(0) : null);
            //视频下载播放地址，默认以第一个为准
            List<String> downloadUrl = OgnlUtils.getListString("video.download", contentMap);
            vid.setDownloadUrl(downloadUrl.size() > 0 ? downloadUrl.get(0) : null);
            //持续时间（单位：秒）
            vid.setDuration(OgnlUtils.getNumber("video.duration", contentMap).intValue());
            //播放数量
            vid.setPlaycount(OgnlUtils.getNumber("video.playcount", contentMap).intValue());
            //视频缩略图地址，默认以第一个为准
            List<String> thumb = OgnlUtils.getListString("video.thumbnail", contentMap);
            vid.setThumb(thumb.size() > 0 ? thumb.get(0) : null);
            //视频小缩略图地址，默认以第一个为准
            List<String> thumbSmall = OgnlUtils.getListString("video.thumbnail_small", contentMap);
            vid.setThumbSmall(thumbSmall.size() > 0 ? thumbSmall.get(0) : null);
            //内容编号（APP内部记录的内容号码，这是属于common内容）
            vid.setContentId(content.getContentId());

            videoMapper.insert(vid);//保存video：将video对象内容存入数据库

        // t_image表：image内容（这里有五个属性没设置：long_picture、thumbnail_link、thumbnail_height、thumbnail_width、small）
        }else if(content.getContentType().equals("image")){
            Image image = new Image();
            //原始高度
            image.setRawHeight(OgnlUtils.getNumber("image.height", contentMap).intValue());
            //原始宽度
            image.setRawWidth(OgnlUtils.getNumber("image.width", contentMap).intValue());
            //下载地址，第一个为准
            List<String> downloadUrl = OgnlUtils.getListString("image.download_url", contentMap);
            image.setWatermarkerUrl(downloadUrl.size() > 0 ? downloadUrl.get(0) : null);
            //缩略图地址，第一个为准
            List<String> thumbSmall = OgnlUtils.getListString("image.thumbnail_small", contentMap);
            image.setThumbUrl(thumbSmall.size() > 0 ? thumbSmall.get(0) : null);
            //大图地址，第一个为准
            List<String> bigUrl = OgnlUtils.getListString("image.big", contentMap);
            image.setBigUrl(bigUrl.size() > 0 ? bigUrl.get(0) : null);
            //内容编号（APP内部记录的内容号码，这是属于common内容）
            image.setContentId(content.getContentId());

            imageMapper.insert(image);//保存image：将image对象内容存入数据库

        // t_image表：gif内容（这里有五个属性没设置：long_picture、thumbnail_link、thumbnail_height、thumbnail_width、small）
        }else if(content.getContentType().equals("gif")){
            Image gif = new Image();
            List<String> bigUrl = OgnlUtils.getListString("gif.images", contentMap);
            gif.setBigUrl(bigUrl.size() > 0 ? bigUrl.get(0) : null);
            gif.setRawHeight(OgnlUtils.getNumber("gif.height", contentMap).intValue());
            gif.setRawWidth(OgnlUtils.getNumber("gif.width", contentMap).intValue());
            List<String> downloadUrl = OgnlUtils.getListString("gif.download_url", contentMap);
            gif.setWatermarkerUrl(downloadUrl.size() > 0 ? downloadUrl.get(0) : null);
            List<String> thumb = OgnlUtils.getListString("gif.thumbnail_small", contentMap);
            gif.setThumbUrl(thumb.size() > 0 ? thumb.get(0) : null);
            gif.setContentId(content.getContentId());

            imageMapper.insert(gif);//保存gif：将image对象内容存入数据库
        }

        // t_user表：user内容（这里有一个属性没设置：relationship）
        Number nuid = OgnlUtils.getNumber("u.uid" , contentMap);
        if(nuid != null){ //小概率的情况下原始数据中是没有这个uid的，因此需要确保能正确得取到uid的值
            //在取到uid后，根据uid在数据库中，查询用户是否存在，存在则直接从数据库中获取它的所有信息，不存在则创建
            User user  = userMapper.selectByPrimaryKey(nuid.longValue());
            if(user == null){
                user = new User();
                //获取第一个头像
                List<String> header = OgnlUtils.getListString("u.header", contentMap);
                user.setHeader(header.size() > 0 ? header.get(0) : null);
                //用户编号
                user.setUid(OgnlUtils.getNumber("u.uid", contentMap).longValue());
                //是否vip 1 是 0 不是
                user.setIsVip(OgnlUtils.getBoolean("u.is_vip", contentMap) ? 1 : 0);
                //是否V
                user.setIsV(OgnlUtils.getBoolean("u.is_v", contentMap) ? 1 : 0);
                //房间url
                user.setRoomUrl(OgnlUtils.getString("u.room_url", contentMap));
                //房间名
                user.setRoomName(OgnlUtils.getString("u.room_name", contentMap));
                //房间角色
                user.setRoomRole(OgnlUtils.getString("u.room_role", contentMap));
                //房间图标
                user.setRoomIcon(OgnlUtils.getString("u.room_icon", contentMap));
                //昵称
                user.setNickname(OgnlUtils.getString("u.name", contentMap));

                userMapper.insert(user);//保存u：将user对象内容存入数据库
            }
            //更新userid：user和content表是用uid相互关联的，因此user增加了一个用户信息，在common相应的内容中要更新uid
            content.setUid(user.getUid());//从user对象中获取uid，加入content对象
            contentMapper.updateByPrimaryKey(content);//将content对象的uid更新到数据库
        }

        //t_comment表：comment内容（评论）
        List<Map<String,Object>> comments = OgnlUtils.getListMap("top_comments" , contentMap);
        if(comments != null){
            //遍历评论
            for(Map commentMap : comments){
                Comment comment = new Comment();
                //comment_id：评论对象
                comment.setCommentId(OgnlUtils.getNumber("id" , commentMap).longValue());
                //comment_text：评论内容
                comment.setCommentText(OgnlUtils.getString("content", commentMap));
                //passtime：过审时间
                comment.setPasstime(OgnlUtils.getString("passtime", commentMap));
                //content_id：设置内容编号
                comment.setContentId(content.getContentId());

                //uid:配置用户,有则加载,无则创建
                Long commentUid = OgnlUtils.getNumber("u.uid", commentMap).longValue();
                //评论用户（这里设置的是评论的用户，上面设置的是发内容的用户）
                User user = userMapper.selectByPrimaryKey(commentUid);
                //有则加载，无则创建（用户既可以是发起评论的人，也可以是评论的人）
                if (user == null) {
                    user = new User();
                    List<String> header = OgnlUtils.getListString("u.header", commentMap);
                    user.setHeader(header.size() > 0 ? header.get(0) : null);
                    user.setUid(OgnlUtils.getNumber("u.uid", commentMap).longValue());
                    user.setIsVip(OgnlUtils.getBoolean("u.is_vip", commentMap) ? 1 : 0);
                    user.setRoomUrl(OgnlUtils.getString("u.room_url", commentMap));
                    user.setRoomName(OgnlUtils.getString("u.room_name", commentMap));
                    user.setRoomRole(OgnlUtils.getString("u.room_role", commentMap));
                    user.setRoomIcon(OgnlUtils.getString("u.room_icon", commentMap));
                    user.setNickname(OgnlUtils.getString("u.name", commentMap));
                    userMapper.insert(user);
                }
                //设置用户编号
                comment.setUid(user.getUid());

                //插入数据
                commentMapper.insert(comment);

            }
        }

        //t_forum表：tags内容（社区）
        List<Map<String, Object>> tags = OgnlUtils.getListMap("tags", contentMap);
        //只获取第一个社区
        if (tags.size() > 0) {
            Map<String, Object> tag = tags.get(0);
            Long forumId = OgnlUtils.getNumber("id", tags.get(0)).longValue();
            Forum forum = forumMapper.selectByPrimaryKey(forumId);
            //查找社区,有则加载,无则创建
            if (forum == null) {
                forum = new Forum();
                //帖子数量
                forum.setPostCount(OgnlUtils.getNumber("post_number", tag).intValue());
                //logo地址
                forum.setLogo(OgnlUtils.getString("image_list", tag));
                //排序
                forum.setForumSort(OgnlUtils.getNumber("forum_sort", tag).intValue());
                //论坛状态
                forum.setForumStatus(OgnlUtils.getNumber("forum_status", tag).intValue());
                //论坛编号
                forum.setForumId(forumId);
                //论坛信息
                forum.setInfo(OgnlUtils.getString("info", tag));
                //论坛名称
                forum.setName(OgnlUtils.getString("name", tag));
                //用户总量
                forum.setUserCount(OgnlUtils.getNumber("sub_number", tag).intValue());
                //保存数据
                forumMapper.insert(forum);
            }

            //更新t_content中的社区编号：forum表和content表是用forum_id相互关联的，因此forum表增加了一个用户信息，在common相应的内容中要更新forum_id
            content.setForumId(forum.getForumId());
            contentMapper.updateByPrimaryKey(content);
        }


        logger.info("Content ID:{} ，内容成功导入" , contentId  );
    }


    /**
     *  页面中查询功能的实现，返回数据后用于页面的表格重载
     * @param page 你要查询第几页
     * @param rows 每页多少行
     */
    public Page<Map> findContents(Integer page , Integer rows , Integer channelId , String contentType , String keyword){

        //启用分页：PageHelper中自动帮我们生成limit分页语句，以及自动帮我们执行查询总数的countSQL
        Map params = new HashMap();
        if(channelId != null && channelId != -1){   //查询时，频道的选择不是“全部频道”，即用户查询时指定了查询频道是推介？视频？图片？笑话？
            params.put("channelId" , channelId);
        }

        if(contentType != null && !contentType.equals("-1")){   //查询时，类型的选择不是“全部类型”，即用户查询时指定了查询类型是视频？图片？动态图？文本？
            params.put("contentType" , contentType);
        }
        if(keyword != null && !keyword.trim().equals("")){  //查询时，关键词的输入不是空，即用户查询时输入了关键字
            params.put("keyword" , "%" + keyword + "%");
        }

        PageHelper.startPage(page , rows);
        Page<Map> list = (Page)contentMapper.findByParams(params);
        return list;
    }

    /**
     * 页面中单条内容的删除
     */
    public void delete(Long contentId){
        contentMapper.deleteByPrimaryKey(contentId);
    }

    /**
     * 页面中单条内容的预览
     */
    public Map getPreviewData(Long contentId){
        Content content = contentMapper.selectByPrimaryKey(contentId);
        User user = userMapper.selectByPrimaryKey(content.getUid());
        //有的content是没有Forum对象的
        Forum forum = null;
        if(content.getForumId() != null){
            forum = forumMapper.selectByPrimaryKey(content.getForumId());
        }
        List<Map> comments = commentMapper.findByContentId(contentId);
        Map previewData = new HashMap();
        previewData.put("content" , content);
        previewData.put("comments" , comments);
        previewData.put("forum" , forum);
        previewData.put("user" , user);
        if(content.getContentType().equals("video")){
            previewData.put("video" , videoMapper.findByContentId(contentId).get(0));
        }else if(content.getContentType().equals("gif") || content.getContentType().equals("image")){
            previewData.put("image" , imageMapper.findByContentId(contentId).get(0));
        }
        return previewData;
    }

}

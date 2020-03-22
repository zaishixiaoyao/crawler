package com.zaishixiaoyao.crawlerbsbdj.mapper;

import com.zaishixiaoyao.crawlerbsbdj.entity.Channel;

public interface ChannelMapper {
    int deleteByPrimaryKey(Integer channelId);

    int insert(Channel record);

    int insertSelective(Channel record);

    Channel selectByPrimaryKey(Integer channelId);

    int updateByPrimaryKeySelective(Channel record);

    int updateByPrimaryKey(Channel record);
}
package com.zaishixiaoyao.crawlerbsbdj.mapper;

import com.zaishixiaoyao.crawlerbsbdj.entity.Source;

import java.util.List;

public interface SourceMapper {
    int deleteByPrimaryKey(Long sourceId);

    int insert(Source record);

    int insertSelective(Source record);

    Source selectByPrimaryKey(Long sourceId);

    int updateByPrimaryKeySelective(Source record);

    int updateByPrimaryKey(Source record);

    public List<Source> findByState(String state);//查询state状态
}
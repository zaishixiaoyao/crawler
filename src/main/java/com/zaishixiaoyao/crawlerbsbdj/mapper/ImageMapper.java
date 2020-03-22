package com.zaishixiaoyao.crawlerbsbdj.mapper;

import com.zaishixiaoyao.crawlerbsbdj.entity.Image;

import java.util.List;

public interface ImageMapper {
    int deleteByPrimaryKey(Long imageId);

    int insert(Image record);

    int insertSelective(Image record);

    Image selectByPrimaryKey(Long imageId);

    int updateByPrimaryKeySelective(Image record);

    int updateByPrimaryKey(Image record);

    public List<Image> findByContentId(Long contentId);
}
package org.coan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.coan.crawler.Crawler;

@Mapper
public interface CrawlerMapper extends BaseMapper<Crawler>{
}

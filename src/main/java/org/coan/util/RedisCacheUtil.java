package org.coan.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

//redis缓存工具类
@Component
public class RedisCacheUtil {

    @Resource
    private RedisTemplate redisTemplate;

    public static RedisTemplate redis;

    /**
     * 初始化RedisTemplate
     */
    //实例化之后自动调用该方法，将依赖注入的redisTemplate赋值给redis
    @PostConstruct
    public void getRedisTemplate() {
        redis = this.redisTemplate;
    }

}

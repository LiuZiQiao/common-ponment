package com.lxk.project.zookeeperTest;

import com.lxk.project.Api.WatcherApi;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/6/19 11:53
 * @ClassName zkWatcherServer
 * @Remark
 */

public class zkWatcherServer extends WatcherApi {
    private static final Logger logger = LoggerFactory.getLogger(zkWatcherServer.class);
    /**
     * 实现WatcherApi，感知数据变动
     * @param watchedEvent
     */
    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.info("【Watcher监听事件】={}",watchedEvent.getState());
        logger.info("【监听路径为】={}",watchedEvent.getPath());
        //  三种监听类型： 创建，删除，更新
        logger.info("【监听的类型为】={}",watchedEvent.getType());
    }
}

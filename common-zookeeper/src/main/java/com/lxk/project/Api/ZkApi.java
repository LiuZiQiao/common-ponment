package com.lxk.project.Api;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/6/18 16:27
 * @ClassName ZkApi
 * @Remark
 */

@Component
public class ZkApi {
    private static final Logger logger = LoggerFactory.getLogger(ZkApi.class);

    @Autowired
    private ZooKeeper zkClient;

    /**
     * 判断指定节点是否存在
     *
     * @param path
     * @param needWatch 指定是否复用zookeeper中默认的Watcher
     * @return
     */
    public Stat exists(String path, boolean needWatch) {
        try {
            return zkClient.exists(path, needWatch);
        } catch (Exception e) {
            logger.error("【断指定节点是否存在异常】{},{}", path, e);
            return null;
        }
    }

    /**
     * 检测结点是否存在 并设置监听事件
     * 三种监听类型： 创建，删除，更新
     *
     * @param path
     * @param watcher 传入指定的监听类
     * @return
     */
    public Stat exists(String path, Watcher watcher) {
        try {
            return zkClient.exists(path, watcher);
        } catch (Exception e) {
            logger.error("【断指定节点是否存在异常】{},{}", path, e);
            return null;
        }
    }

    /**
     * 创建持久化节点
     *
     * @param path
     * @param data
     */
    public boolean createNode(String path, String data) {
        try {
            zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            return true;
        } catch (Exception e) {
            logger.error("【创建持久化节点异常】{},{},{}", path, data, e);
            return false;
        }
    }

    /**
     * 指定节点下创建临时顺序节点
     *
     * @param path
     * @param childPath
     * @return
     */
    public String createEphemeralSequential(String path, String childPath) {
        path = path + "/" + childPath;
        try {
            return zkClient.create(path, childPath.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.error("【创建临时顺序节点异常】{},{},{}", path, childPath, e);
        }
        return "";
    }


    /**
     * 修改持久化节点
     *
     * @param path
     * @param data
     */
    public boolean updateNode(String path, String data) {
        try {
            //zk的数据版本是从0开始计数的。如果客户端传入的是-1，则表示zk服务器需要基于最新的数据进行更新。如果对zk的数据节点的更新操作没有原子性要求则可以使用-1.
            //version参数指定要更新的数据的版本, 如果version和真实的版本不同, 更新操作将失败. 指定version为-1则忽略版本检查
            zkClient.setData(path, data.getBytes(), -1);
            return true;
        } catch (Exception e) {
            logger.error("【修改持久化节点异常】{},{},{}", path, data, e);
            return false;
        }
    }

    /**
     * 删除持久化节点
     *
     * @param path
     */
    public boolean deleteNode(String path) {
        try {
            //version参数指定要更新的数据的版本, 如果version和真实的版本不同, 更新操作将失败. 指定version为-1则忽略版本检查
            zkClient.delete(path, -1);
            return true;
        } catch (Exception e) {
            logger.error("【删除持久化节点异常】{},{}", path, e);
            return false;
        }
    }

    /**
     * 获取当前节点的子节点(不包含孙子节点)
     *
     * @param path 父节点path
     */
    public List<String> getChildren(String path){
        List<String> list = null;
        try {
            list = zkClient.getChildren(path, false);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.error("【获取当前节点的子节点】{}",path,e);
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 获取指定节点的值
     *
     * @param path
     * @return
     */
    public String getData(String path) {
        try {
            Stat stat = new Stat();
            byte[] bytes = zkClient.getData(path, true, stat);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取指定节点的值,并感知数据是否有变动
     *
     * @param path
     * @return
     */
    public String getData(String path, Watcher watcher) {
        try {
            Stat stat = new Stat();
            byte[] bytes = zkClient.getData(path, watcher, stat);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 启动测试连接，创建，获取和删除。
     * 这里Watcher使用默认的true，默认不再需要感知
     */
    @PostConstruct
    public void init() {
        String path = "/zk-watcher-test";
        String data = "zk初始化连接测试";
        createNode(path, data);
        logger.info("【执行初始化测试方法】---{}", path);
        String value = getData(path);
        logger.info("【执行初始化测试方法getData返回值】={}", value);
        // 删除节点出发 监听事件
        deleteNode(path);
    }


}


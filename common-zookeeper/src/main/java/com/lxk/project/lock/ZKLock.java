package com.lxk.project.lock;

import com.lxk.project.Api.ZkApi;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/6/18 16:27
 * @ClassName ZKLock
 * @Remark
 */
@Component
public class ZKLock implements Watcher {

    @Autowired
    private ZooKeeper zkClient;

    @Autowired
    private ZkApi zkApi;

    /**
     * 当前锁
     */
    private String currentLock;
    /**
     * 资源名称
     */
    private String lockName;
    /**
     * 锁根节点
     */
    @Value("${zookeeper.root_lock}")
    private String ROOT_LOCK;
    /**
     * 锁的各个资源根节点
     */
    private String tmpRootLock;
    /**
     * 由于zookeeper监听节点状态会立即返回，所以需要使用CountDownLatch(也可使用信号量等其他机制)
     */
    private CountDownLatch latch;

    private void init() {
        try {
            createZNode(ROOT_LOCK, CreateMode.PERSISTENT);
//            tmpRootLock = ROOT_LOCK + "/" + lockName;
//            /**
//             * zk临时节点下不能创建临时顺序节点
//             */
//            createZNode(tmpRootLock, CreateMode.PERSISTENT);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建根节点
     *
     * @param node
     * @param mode
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void createZNode(String node, CreateMode mode) throws KeeperException, InterruptedException {
        //获取根节点状态
        Stat stat = zkApi.exists(node, false);
        //如果根节点不存在，则创建根节点，根节点类型为永久节点
        if (stat == null) {
            zkClient.create(node, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        }
    }

    public void lock() {
        try {
            this.init();
            this.tmpRootLock = "" + System.nanoTime();
            //在根节点下创建临时顺序节点，返回值为创建的节点路径
            currentLock = zkClient.create(ROOT_LOCK + "/" + tmpRootLock, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            //获取根节点下的所有临时顺序节点，不设置监视器
            List<String> children = zkClient.getChildren(ROOT_LOCK, false);
            //对根节点下的所有临时顺序节点进行从小到大排序
            children.sort(null);
            //判断当前节点是否为最小节点，如果是则获取锁，若不是，则找到自己的前一个节点，监听其存在状态
            int curIndex = children.indexOf(currentLock.substring(currentLock.lastIndexOf("/") + 1));
            if (curIndex != 0) {
                //获取当前节点前一个节点的路径
                String prev = children.get(curIndex - 1);
                //监听当前节点的前一个节点的状态，null则节点不存在
                Stat stat = zkClient.exists("/"+tmpRootLock + "/" + prev, true);
                //此处再次判断该节点是否存在
                if (stat != null) {
                    latch = new CountDownLatch(1);
                    //进入等待锁状态
                    latch.await();
                    latch = null;
                }
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //释放锁
    public void unlock() {
        try {
            //删除创建的节点
            zkClient.delete(currentLock, -1);
            List<String> children = zkClient.getChildren(ROOT_LOCK, false);
            if (children.size() == 0) {
                zkClient.delete(ROOT_LOCK+"/"+tmpRootLock, -1);
                //关闭zookeeper连接
                zkClient.close();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
        currentLock = null;
    }

    @Override
    public void process(WatchedEvent event) {
        if (this.latch != null) {
            latch.countDown();
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 7; i++) {
            new Thread(() -> {
                ZKLock lock = new ZKLock();
                lock.lock();
            }).start();
        }
//        ZKLock lock = new ZKLock();
//        lock.lock();
//
//        ZKLock lock1 = new ZKLock();
//        lock1.lock();
//        lock1.unlock();
//
//        lock.unlock();
//        String uu = "";
    }
}
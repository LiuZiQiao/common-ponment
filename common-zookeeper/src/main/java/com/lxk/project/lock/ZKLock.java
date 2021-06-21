package com.lxk.project.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/6/18 16:27
 * @ClassName ZKLock
 * @Remark
 */
public class ZKLock implements Watcher {
    @Autowired
    private ZooKeeper zk;
    //当前锁
    private String currentLock;
    //资源名称
    private String lockName;
    //锁根节点
    private String ROOT_LOCK = "/root_lock";
    /* 锁的各个资源根节点 */
    private String tmpRootLock;
    /* 由于zookeeper监听节点状态会立即返回，所以需要使用CountDownLatch(也可使用信号量等其他机制) */
    private CountDownLatch latch;

    public ZKLock(String zkAddress, String lockName) {
        this.lockName = "" + System.nanoTime();
        try {
            zk = new ZooKeeper(zkAddress, 30000, this);
            createZNode(ROOT_LOCK, CreateMode.PERSISTENT);
            tmpRootLock = ROOT_LOCK + "/" + lockName;
            //****zk临时节点下不能创建临时顺序节点
            createZNode(tmpRootLock, CreateMode.PERSISTENT);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createZNode(String node, CreateMode mode) throws KeeperException, InterruptedException {
        //获取根节点状态
        Stat stat = zk.exists(node, false);
        //如果根节点不存在，则创建根节点，根节点类型为永久节点
        if (stat == null) {
            zk.create(node, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        }
    }

    public void lock() {
        try {
            //在根节点下创建临时顺序节点，返回值为创建的节点路径
            currentLock = zk.create(tmpRootLock + "/" + lockName, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            //获取根节点下的所有临时顺序节点，不设置监视器
            List<String> children = zk.getChildren(tmpRootLock, false);
            //对根节点下的所有临时顺序节点进行从小到大排序
            children.sort(null);
            //判断当前节点是否为最小节点，如果是则获取锁，若不是，则找到自己的前一个节点，监听其存在状态
            int curIndex = children.indexOf(currentLock.substring(currentLock.lastIndexOf("/") + 1));
            if (curIndex != 0) {
                //获取当前节点前一个节点的路径
                String prev = children.get(curIndex - 1);
                //监听当前节点的前一个节点的状态，null则节点不存在
                Stat stat = zk.exists(tmpRootLock + "/" + prev, true);
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
            zk.delete(currentLock, -1);
            List<String> children = zk.getChildren(tmpRootLock, false);
            if (children.size() == 0) {
                zk.delete(tmpRootLock, -1);
                //关闭zookeeper连接
                zk.close();
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
//        for (int i = 0; i < 7; i++) {
//            new Thread(() -> {
//                ZKLock lock = new ZKLock("127.0.0.1:2181", "lock");
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                lock.lock();
//            }).start();
//        }
        ZKLock lock = new ZKLock("127.0.0.1:2181", "L1");
        lock.lock();

        ZKLock lock1 = new ZKLock("127.0.0.1:2181", "L2");
        lock1.lock();
        lock1.unlock();

        lock.unlock();
        String uu = "";
    }
}
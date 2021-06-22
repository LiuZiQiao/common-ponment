package com.lxk.project.lock;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/6/21 15:57
 * @ClassName ZooKeeperLock
 * @Remark
 */

public interface ZooKeeperLock {

    boolean lock(String node);

    boolean release();

    boolean exists();

}

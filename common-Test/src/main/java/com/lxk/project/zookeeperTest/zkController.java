package com.lxk.project.zookeeperTest;

import com.lxk.project.Api.ZkApi;
import com.lxk.project.lock.ZKLock;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/6/18 16:46
 * @ClassName TestController
 * @Remark
 */

@RestController
@RequestMapping("/zookeeper")
public class zkController {
    private static final Logger logger = LoggerFactory.getLogger(zkController.class);

    @Autowired
    private ZkApi zkApi;

    @Autowired
    private ZKLock zkLock;

    @GetMapping("/init")
    public void init() {
        zkApi.init();
    }

    @PostMapping("/createNode")
    public boolean createNode(@RequestParam("path") String path, @RequestParam("data") String data) {
        boolean node = zkApi.createNode(path, data);
        zkApi.getData(path, new zkWatcherServer());
        return node;
    }

    @PostMapping("/getDataOnWatcher")
    public String getDataOnWatcher(@RequestParam("path") String path) {
        return zkApi.getData(path, new zkWatcherServer());
    }

    @PostMapping("/updateData")
    public void updateData(@RequestParam("path") String path, @RequestParam("data") String data) {
        zkApi.updateNode(path, data);
    }

    @PostMapping("/delete")
    public void delete(@RequestParam("path") String path) {
        zkApi.deleteNode(path);
    }

    @PostMapping("/getChildren")
    public List getChildren(@RequestParam("path") String path) {
        List<String> children = zkApi.getChildren(path);
        System.out.println(children);
        return children;
    }

    /**
     * 创建临时顺序节点
     *
     * @param path
     * @return
     */
    @PostMapping("/createEphemeralSequential")
    public String createEphemeralSequential(@RequestParam("path") String path) {
        return zkApi.createEphemeralSequential(path, "childNode");
    }

    @GetMapping("/getLock")
    public void getLock() throws InterruptedException {
        zkLock.lock();
        Thread.sleep(10000);
        zkLock.unlock();
    }


    @GetMapping("/exist")
    public Stat exist(@RequestParam("path") String path) {
        return zkApi.exists(path, false);
    }

}

package com.lxk.project.zookeeperTest;

import com.lxk.project.Api.ZkApi;
import org.apache.zookeeper.KeeperException;
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

    @Autowired
    ZkApi zkApi;

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
        try {
            List<String> children = zkApi.getChildren(path);
            System.out.println(children);
            return children;
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}

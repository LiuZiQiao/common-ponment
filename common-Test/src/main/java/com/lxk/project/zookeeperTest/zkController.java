package com.lxk.project.zookeeperTest;

import com.lxk.project.util.ZkApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public void init(){
        zkApi.init();
    }



}

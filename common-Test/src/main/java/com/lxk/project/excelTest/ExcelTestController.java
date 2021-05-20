package com.lxk.project.excelTest;

import com.lxk.project.common.po.ResultWrapper;
import com.lxk.project.common.utils.ExcelUtils;
import com.lxk.project.dbTest.entity.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/5/19 15:21
 * @ClassName ExcelTestController
 * @Remark
 */

@RestController
@RequestMapping("/excelTest")
public class ExcelTestController {

    @PostMapping("/parseFile")
    public ResultWrapper parseFile(MultipartFile file){
        return ExcelUtils.parseExcel(file);
    }
    @PostMapping("/exportExcel")
    public void exportExcel(HttpServletResponse response){

        List<User> users = new ArrayList<>();
        for (int i=0;i<10;i++){
            users.add(new User(1,"lxk"+i,"a"+i,i,i));
        }

        Map header = new HashMap<>();
        header.put("id","id");
        header.put("name","name");
        header.put("password","password");
        header.put("age","age");
        header.put("deleteFlag","deleteFlag");
        ExcelUtils.exportExcel("测试","测试",header,users,response);
    }
}

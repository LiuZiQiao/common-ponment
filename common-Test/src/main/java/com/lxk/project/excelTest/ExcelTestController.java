package com.lxk.project.excelTest;

import com.lxk.project.common.po.ResultWrapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    public ResultWrapper parseFile(MultipartFile file){

        return ResultWrapper.success();
    }
}

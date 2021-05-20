package com.lxk.project.excelTest;

import com.lxk.project.common.po.ResultWrapper;
import com.lxk.project.common.utils.ExcelUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

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

        ExcelUtils.exportExcel();
    }
}

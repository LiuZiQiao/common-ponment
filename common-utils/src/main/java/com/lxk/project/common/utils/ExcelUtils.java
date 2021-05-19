package com.lxk.project.common.utils;

import com.lxk.project.po.ResultWrapper;
import org.apache.commons.collections4.MapUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Author macos·lxk
 * @create 2021/5/19 上午10:58
 */

public class ExcelUtils {


    private static int DEFAULT_SIZE = 2*1024*1024;
    private static final String XLS = "xls";
    private static final String XLSX = "xlsx";
    private static int MAX_ROW = 2000;
    private static int MAX_CELL = 200;

    /**
     *
     * @param file
     * @return Map<String,Object>
     */
    public ResultWrapper parseExcel(MultipartFile file) {

        if (Objects.isNull(file)) {
            return ResultWrapper.error("文件不能为空");
        }
        if (file.getSize() > DEFAULT_SIZE){
            return ResultWrapper.error("文件不能超过2M");
        }

        Map result = new HashMap();
        result.put("title","11");
        result.put("body", Arrays.asList(234));
        return ResultWrapper.success(result);
    }

}

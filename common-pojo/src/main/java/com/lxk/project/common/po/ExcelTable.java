package com.lxk.project.common.po;

import java.util.List;
import java.util.Map;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/5/24 11:25
 * @ClassName ExcelTable
 * @Remark
 */

public class ExcelTable {

    /**
     * 表格头
     */
    private Map<String, String> header;

    /**
     * 表格数据
     */
    private List body;

    public Map<String, String> getHeader() {
        return header;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public List getBody() {
        return body;
    }

    public void setBody(List body) {
        this.body = body;
    }
}

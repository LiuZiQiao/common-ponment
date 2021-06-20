package com.lxk.project.common.po;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.util.SheetUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/5/24 11:26
 * @ClassName WorkBook
 * @Remark
 */

public class WorkBook {
    /**
     * Excel，单元格中最大字符长度为 32767，极端场景下超过这个长度会报错。
     * 为了兼顾使用体验，限制字符串最大长度。超过此长度的字符串将会被截取。
     */
    private static final int CELL_STRING_MAX_LENGTH = 4000;

    private static final int ROW_NUM_HEADER = 0;

    private static final int ROW_NUM_BODY = 1;

    /**
     * Apache POI 工作簿
     */
    private HSSFWorkbook hssfWorkbook = new HSSFWorkbook();


    /**
     * “年月日时分秒”格式单元格
     * 在开始导出时，创建
     */
    private HSSFCellStyle styleYyyyMmDdHhMmSs;

    /**
     * “年月日时分”格式单元格
     * 在开始导出时，创建
     */
    private HSSFCellStyle styleYyyyMmDdHhMm;

    /**
     * 工作簿数据
     * key      => 工作簿名称
     * value    => 工作簿数据 有三种类型 E6Table，E6Picture, E6TableExtension
     */
    private Map<String, Object> workbook = new LinkedHashMap<String, Object>();

    /**
     * 创建单元格格式
     */
    private void initCellStyle(){
        CreationHelper creationHelper = hssfWorkbook.getCreationHelper();
        if(Objects.isNull(styleYyyyMmDdHhMmSs)){
            styleYyyyMmDdHhMmSs = hssfWorkbook.createCellStyle();
            DataFormat dataFormat = creationHelper.createDataFormat();
            short format = dataFormat.getFormat("yyyy-MM-dd HH:mm:ss");
            styleYyyyMmDdHhMmSs.setDataFormat(format);
        }
        if(Objects.isNull(styleYyyyMmDdHhMm)){
            styleYyyyMmDdHhMm = hssfWorkbook.createCellStyle();
            DataFormat dataFormat = creationHelper.createDataFormat();
            short format = dataFormat.getFormat("yyyy-MM-dd HH:mm");
            styleYyyyMmDdHhMm.setDataFormat(format);
        }
    }

    /**
     * 增加一个工作表
     *
     * @param name   工作表名称
     * @param header 表格头
     *               * @param body   表格数据
     */
    public void addWorksheet(String name, Map<String, String> header, List body) {
        ExcelTable excelTable = new ExcelTable();
        excelTable.setHeader(header);
        excelTable.setBody(body);
        this.workbook.put(name, excelTable);
    }

    /**
     * 增加一个工作表
     *
     * @param name        工作表名称
     * @param picture     图片字节数组
     * @param pictureType 图片类型
     *                    Workbook.PICTURE_TYPE_EMF
     *                    Workbook.PICTURE_TYPE_WMF
     *                    Workbook.PICTURE_TYPE_PICT
     *                    Workbook.PICTURE_TYPE_JPEG
     *                    Workbook.PICTURE_TYPE_PNG
     *                    Workbook.PICTURE_TYPE_DIB
     */
    public void addWorksheet(String name, byte[] picture, int pictureType) {
        PictureTable pictureTable = new PictureTable();
        pictureTable.setPictureType(pictureType);
        pictureTable.setPicture(picture);
        this.workbook.put(name, pictureTable);
    }

    /**
     * 输出工作表
     *
     * @return
     */
    public void export(OutputStream outputStream) throws Exception {
        this.initCellStyle();
        for (Map.Entry<String, Object> entry : this.workbook.entrySet()) {
            String sheetName = entry.getKey();
            Object sheetData = entry.getValue();
            HSSFSheet sheet = this.hssfWorkbook.createSheet(sheetName);
            if (sheetData instanceof PictureTable) {
                this.fillSheet(sheet, (PictureTable) sheetData);
            } else if (sheetData instanceof ExcelTable) {
                this.fillSheet(sheet, (ExcelTable) sheetData);
            }
        }
        hssfWorkbook.write(outputStream);
        hssfWorkbook.close();
    }

    private void fillSheet(HSSFSheet sheet, PictureTable pictureTable) {
        int pictureIdx = hssfWorkbook.addPicture(pictureTable.getPicture(), pictureTable.getPictureType());
        CreationHelper helper = hssfWorkbook.getCreationHelper();
        HSSFPatriarch drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = helper.createClientAnchor();
        final int PICTURE_POSITION_COLUMN = 1;
        final int PICTURE_POSITION_ROW = 1;
        anchor.setCol1(PICTURE_POSITION_COLUMN);
        anchor.setRow1(PICTURE_POSITION_ROW);
        Picture pict = drawing.createPicture(anchor, pictureIdx);
        pict.resize();
    }

    private void fillSheet(HSSFSheet sheet, ExcelTable excelTable) throws Exception {
        //填充表格头
        Map<String, String> header = excelTable.getHeader();
        HSSFRow headerRow = sheet.createRow(ROW_NUM_HEADER);
        this.fillHeader(headerRow, header);
        //填充表格体
        List<Object> body = excelTable.getBody();

        int rowNumber = ROW_NUM_BODY;
        for (Object rowData : body) {
            HSSFRow bodyRow = sheet.createRow(rowNumber);
            int columnNumber = 0;
            boolean jsonFlag = rowData instanceof JSONObject;
            for (Map.Entry<String, String> entry : header.entrySet()) {
                String columnName = entry.getKey();
                Object value = jsonFlag?((JSONObject)rowData).getString(columnName): invokeGetMethodByAttribute(columnName, rowData);
                HSSFCell cell = bodyRow.createCell(columnNumber);
                this.setCellValue(cell, value);
                columnNumber++;
            }
            rowNumber++;
        }

        this.autoSizeColumn(sheet, header.size());
    }
    /**
     * 宽度自适应，该段代码重写了POI原有方法
     * @param sheet
     * @param MAX_COLUMN_NUMBER
     */
    private void autoSizeColumn(HSSFSheet sheet, final int MAX_COLUMN_NUMBER) {
        for (int columnNumber = 0; columnNumber < MAX_COLUMN_NUMBER; columnNumber++) {
            double width = SheetUtil.getColumnWidth(sheet, columnNumber, false);
            width += 1; //该行代码在原有代码上新增
            if (width != -1) {
                width *= 256;
                int maxColumnWidth = 255 * 256; // The maximum column width for an individual cell is 255 characters
                if (width > maxColumnWidth) {
                    width = maxColumnWidth;
                }
                sheet.setColumnWidth(columnNumber, (int) (width));
            }
        }
    }
    public Object invokeGetMethodByAttribute(String attribute, Object object) throws Exception {
        final String PREFIX = "get";
        String methodName = PREFIX + StringUtils.capitalize(attribute);
        Method getMethod = null;
        try {
            getMethod = object.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("无效属性。属性名：" + attribute);
        }
        Object value = null;
        try {
            value = getMethod.invoke(object);
        } catch (IllegalAccessException e) {
            throw new Exception("获取属性值失败。属性名：" + attribute);
        } catch (InvocationTargetException e) {
            throw new Exception("获取属性值失败。属性名：" + attribute);
        }
        return value;
    }
    /**
     * 填充表格头
     *
     * @param headerRow
     * @param headerData
     */
    private void fillHeader(HSSFRow headerRow, Map<String, String> headerData) {
        int columnNumber = 0;
        for (Map.Entry<String, String> entry : headerData.entrySet()) {
            String value = entry.getValue();
            HSSFCell cell = headerRow.createCell(columnNumber);
            this.setCellValue(cell, value);
            columnNumber++;
        }
    }
    private void setCellValue(HSSFCell cell, Object value) {
        if(Objects.isNull(value)){
            return;
        }

        if (value instanceof String) {
            this.setCellValueString(cell, (String)value);
        } else if (value instanceof Number) {
            this.setCellValueNumber(cell, (Number)value);
        }else if (value instanceof Date) {
            this.setCellValueDate(cell, (Date)value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
    private void setCellValueNumber(HSSFCell cell, Number value){
        cell.setCellValue(value.doubleValue());
    }
    private void setCellValueDate(HSSFCell cell, Date value) {
        cell.setCellStyle(styleYyyyMmDdHhMm);
        cell.setCellValue(value);
    }
    /**
     * 填充String格式的字段
     * @param cell 单元格
     * @param value 要填充进单元格的值
     */
    private void setCellValueString(HSSFCell cell, String value){
        HSSFCellStyle cellStyle = this.getStringCellStyle(value);
        if(Objects.nonNull(cellStyle)){
            cell.setCellStyle(cellStyle);
        }
        cell.setCellValue(value.length() > CELL_STRING_MAX_LENGTH ? value.substring(0, CELL_STRING_MAX_LENGTH) + " ..." : value);
    }    /**
     * 获取“时间格式”
     * @param value
     * @return
     */
    private HSSFCellStyle getStringCellStyle(String value){
        if(Objects.isNull(value)){
            return null;
        }

        final String PATTERN_YYYY_MM_DD_HH_MM_SS = "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}";
        final String PATTERN_YYYY_MM_DD_HH_MM = "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}";

        if(Pattern.matches(PATTERN_YYYY_MM_DD_HH_MM_SS, value)){
            return this.styleYyyyMmDdHhMmSs;
        }
        else if(Pattern.matches(PATTERN_YYYY_MM_DD_HH_MM, value)){
            return this.styleYyyyMmDdHhMm;
        }
        else {
            return null;
        }
    }

}

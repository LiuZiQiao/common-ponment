package com.lxk.project.common.utils;

import com.lxk.project.common.po.DateStyle;
import com.lxk.project.common.po.ResultWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * @Author macos·lxk
 * @create 2021/5/19 上午10:58
 */

public class ExcelUtils {

    static String DEFAULT_VALUE = "";
    private static int DEFAULT_SIZE = 2*1024*1024;
    private static final String XLS = "xls";
    private static final String XLSX = "xlsx";
    private static int MAX_ROW = 2000;
    private static int MAX_CELL = 200;

    /**
     * 默认解析第一个sheet
     * @param file
     * @return Map<String,Object>
     */
    public static ResultWrapper parseExcel(MultipartFile file) {

        if (Objects.isNull(file)) {
            return ResultWrapper.error("文件不能为空");
        }
        if (file.getSize() > DEFAULT_SIZE){
            return ResultWrapper.error("文件不能超过2M");
        }

        String filename = file.getOriginalFilename();
        if (!(filename.endsWith(XLS) || filename.endsWith(XLSX))) {
            return ResultWrapper.error("文件格式不正确");
        }

        try {
            int totalRowNum = getTotalRowNum(file,0,0);
            int totalCellNum = getTotalCellNum(file, 0, 0);
            if (totalRowNum > MAX_ROW) {
                return ResultWrapper.error("数据不能超过" + MAX_ROW + "行");
            }
            if (totalCellNum > MAX_CELL) {
                return ResultWrapper.error("数据不能超过" + MAX_ROW + "列");
            }
            List<String> excelHeader = ExcelUtils.getExcelHeader(file, 0, 0);
            List<Map<String, Object>> excelData = ExcelUtils.getExcelData(file, 0, 0, 1);
            Map<String, Object> map = new HashMap<>(16);
            map.put("title", excelHeader);
            map.put("body", excelData);
            return ResultWrapper.success(map);

        } catch (IOException e) {
            e.printStackTrace();
            return ResultWrapper.error("文件解析失败");
        }
    }

    /**
     * 解析多个sheet ，返回一个List<Map>
     * @param file
     * @param isSheets
     * @return
     */
    public static ResultWrapper parseExcel(MultipartFile file,boolean isSheets) {

        if (Objects.isNull(file)) {
            return ResultWrapper.error("文件不能为空");
        }
        if (file.getSize() > DEFAULT_SIZE){
            return ResultWrapper.error("文件不能超过2M");
        }

        String filename = file.getOriginalFilename();
        if (!(filename.endsWith(XLS) || filename.endsWith(XLSX))) {
            return ResultWrapper.error("文件格式不正确");
        }

        try {
            int totalRowNum = getTotalRowNum(file,0,0);
            int totalCellNum = getTotalCellNum(file, 0, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map result = new HashMap();

        result.put("title","11");
        result.put("body", Arrays.asList(234));
        return ResultWrapper.success(result);
    }

    public static int getTotalRowNum(MultipartFile file, int sheetNumber,int headerNumber) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(sheetNumber);
        // 获取最后一行行号
        int lastRowNum = sheet.getLastRowNum() + headerNumber;
        return lastRowNum;
    }

    public static int getTotalCellNum(MultipartFile file, int sheetNumber, int headerNumber) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(sheetNumber);
        Row headerRow = sheet.getRow(headerNumber);
        return headerRow.getLastCellNum();
    }

    private static List<Map<String, Object>> getExcelData(MultipartFile file,int sheetNumber, int headerNumber , int rowStart) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());

        Sheet sheet = workbook.getSheetAt(sheetNumber);
        Row headerRow = sheet.getRow(headerNumber);
        // 获取最后一行行号
        int lastRowNum = sheet.getLastRowNum();
        // 获取最后一列列号
        short lastCellNum = headerRow.getLastCellNum();

        List<Map<String, Object>> resultList = new ArrayList<>(lastRowNum);
        List<String> headList = new ArrayList<>(lastCellNum);
        // 获取标题头
        for (int i = 0;i <lastCellNum; i++) {
            Cell cell = headerRow.getCell(i);
            headList.add(cell.getStringCellValue());
        }

        for (int i = rowStart; i<= lastRowNum; i++) {
            //临时变量，记录是否有字段不为空
            boolean allCellIsBlank = true;
            Row currentRow = sheet.getRow(i);
            if (Objects.isNull(currentRow) || checkAllCellIsEmpty(currentRow)) {
                continue;
            }
            //short currentCellNum = currentRow.getLastCellNum();
            Map<String, Object> map = new HashMap<>(headList.size());
            map.put("excelRowId",i);
            for (int j = 0; j<headList.size(); j++) {
                Cell cell = currentRow.getCell(j);
                Object cellValue = getCellValue(cell);
                map.put(headList.get(j),cellValue);
                if(allCellIsBlank && !DEFAULT_VALUE.equals(cellValue)){
                    allCellIsBlank = false;
                }
            }
            //只有所有列都不为空，才加入结果集
            if(!allCellIsBlank) {
                resultList.add(map);
            }
        }
        return resultList;
    }

    private static List<String> getExcelHeader(MultipartFile file, int sheetNumber, int headerNumber) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(sheetNumber);
        Row headerRow = sheet.getRow(headerNumber);
        short headerLen = headerRow.getLastCellNum();
        List<String> resultList = new ArrayList<>(headerLen);
        for (int i = 0;i<headerLen;i++) {
            Cell cell = headerRow.getCell(i);
            resultList.add(cell.getStringCellValue());
        }
        return resultList;
    }

    private static Object getCellValue(Cell cell) {
        Object cellValue = DEFAULT_VALUE;
        if(cell!=null){
            if (cell.getCellType() == CellType.NUMERIC) {
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    cellValue = DateUtils.formatDateToString(cell.getDateCellValue(), DateStyle.YYYY_MM_DD_HH_MM_SS);
                } else {
                    cellValue = cell.getNumericCellValue();
                }
            } else if (cell.getCellType() == CellType.STRING) {
                cellValue = replaceBlank(cell.getStringCellValue());
            } else if (cell.getCellType() == CellType.FORMULA) {
                cellValue = cell.getCellFormula();
            } else if (cell.getCellType() == CellType.BOOLEAN) {
                cellValue = cell.getBooleanCellValue();
            } else if (cell.getCellType() == CellType.ERROR) {
                cellValue = cell.getErrorCellValue();
            }

        }
        return cellValue;
    }

    private static boolean checkAllCellIsEmpty(Row row) {
        if(Objects.isNull(row)){
            return true;
        }
        Iterator<Cell>  cellIterator = row.cellIterator();
        while(cellIterator.hasNext()){
            Cell cell = cellIterator.next();
            if(Objects.nonNull(getCellValue(cell))){
                return false;
            }
        }
        return true;
    }

    private static Object replaceBlank(String string) {
        if(StringUtils.isNotEmpty(string)){
            string = StringUtils.trim(string);
            Pattern p = compile("\t|\r|\n");
            Matcher m = p.matcher(string);
            string = m.replaceAll("");
        }
        return string;
    }


    /**
     * 数据大于3万行，则导出CSV格式
     */
    public static final Integer CSV_SIZE = 30000;

    /**
     *
     * @param list
     * @param title
     * @param sheetName
     * @param pojoClass
     * @param fileName
     * @param response
     */
    public static void exportExcel(List<?> list, String title, String sheetName, Class<?> pojoClass,String fileName, HttpServletResponse response){

    }

    /**
     * 导出Excel或CSV格式文件（大于30000行导出CSV格式，小于等于30000行导出xls格式）
     * @param response HttpServletResponse
     * @param filename 文件名前缀
     * @param sheetName 表格名称 csv格式文件，该项无效
     * @param header 表格头
     * @param body 表格数据
     */
    public static void export(HttpServletResponse response, String filename, String sheetName, Map<String, String> header, List body) {

    }
    /**
     *
     * @param fileName
     * @param response
     * @param workbook
     */
    public static void downLoadExcel(String fileName, HttpServletResponse response, Workbook workbook){

    }


    /**
     *
     * @param list
     * @param pojoClass
     * @param fileName
     * @param response
     * @param exportParams
     */
    private static void defaultExport(List<?> list, Class<?> pojoClass, String fileName, HttpServletResponse response, ExportParams exportParams) {


    }

    /**
     *
     * @param list
     * @param fileName
     * @param response
     */
    private static void defaultExport(List<Map<String, Object>> list, String fileName, HttpServletResponse response) {
        Workbook workbook = ExcelExportUtil.exportExcel(list, ExcelType.HSSF);
        if (workbook != null);
        downLoadExcel(fileName, response, workbook);
    }

}

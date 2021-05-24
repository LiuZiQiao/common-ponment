package com.lxk.project.common.po;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/5/24 11:30
 * @ClassName PictureTable
 * @Remark
 */

public class PictureTable {

    /**
     * 图片类型
     * Workbook.PICTURE_TYPE_EMF
     * Workbook.PICTURE_TYPE_WMF
     * Workbook.PICTURE_TYPE_PICT
     * Workbook.PICTURE_TYPE_JPEG
     * Workbook.PICTURE_TYPE_PNG
     * Workbook.PICTURE_TYPE_DIB
     */
    private int pictureType;

    /**
     * 图片字节数组
     */
    private byte[] picture;

    public int getPictureType() {
        return pictureType;
    }

    public void setPictureType(int pictureType) {
        this.pictureType = pictureType;
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }
}

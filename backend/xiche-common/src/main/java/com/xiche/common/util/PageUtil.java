package com.xiche.common.util;

/**
 * 分页工具：列表接口强制分页，禁止无上限全量返回。
 */
public class PageUtil {
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private int pageNum;
    private int pageSize;

    public PageUtil() {
        this(DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE);
    }

    public PageUtil(int pageNum, int pageSize) {
        this.pageNum = pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
        int size = pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
        this.pageSize = size > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : size;
    }

    public static PageUtil of(Integer pageNum, Integer pageSize) {
        return new PageUtil(pageNum == null ? DEFAULT_PAGE_NUM : pageNum,
                pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
    }

    /** MySQL LIMIT 偏移量 */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }

    public int getLimit() {
        return pageSize;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        int size = pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
        this.pageSize = size > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : size;
    }
}

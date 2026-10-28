package com.lemanty.aipet.model;

public enum TaskState {
    GENERATING("正在生成"), COMPLETED("回答完成"), FAILED("输出出错");
    private final String label;
    TaskState(String label) { this.label = label; }
    public String label() { return label; }
}

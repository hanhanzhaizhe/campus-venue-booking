package com.campus.venue.common.api;

public enum ErrorCode {

    OK("OK", "success", 200),

    PARAM_INVALID("PARAM_INVALID", "请求参数不合法", 400),
    TIME_INVALID("TIME_INVALID", "时间不合法", 400),
    TOO_EARLY_OR_TOO_LATE("TOO_EARLY_OR_TOO_LATE", "预约时间早于当前或超出可预约范围", 400),
    OUT_OF_OPEN_HOURS("OUT_OF_OPEN_HOURS", "不在场地开放时间内", 400),
    QUOTA_EXCEEDED("QUOTA_EXCEEDED", "未结束预约已达上限", 400),
    VENUE_DISABLED("VENUE_DISABLED", "场地已停用", 400),
    CANCEL_NOT_ALLOWED("CANCEL_NOT_ALLOWED", "当前不可取消", 400),
    RESCHEDULE_NOT_ALLOWED("RESCHEDULE_NOT_ALLOWED", "当前不可改约", 400),

    UNAUTHENTICATED("UNAUTHENTICATED", "未登录或登录已失效", 401),
    FORBIDDEN("FORBIDDEN", "无权限", 403),

    VENUE_NOT_FOUND("VENUE_NOT_FOUND", "场地不存在", 404),
    RESERVATION_NOT_FOUND("RESERVATION_NOT_FOUND", "预约不存在", 404),

    CONFLICT("CONFLICT", "与已有预约时间冲突", 409),
    DUPLICATE_RESERVATION("DUPLICATE_RESERVATION", "同一场地同一时段已有预约", 409),

    INTERNAL_ERROR("INTERNAL_ERROR", "系统异常", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

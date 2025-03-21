package com.alipay.sofa.koupleless.adapter.thread;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BizContext {
    private String bizName;
    private String bizVersion;
    private String bizUrl;
    private Map<String, Object> attachment;

    public BizContext cloneInstance() {
        return BizContext.builder().bizName(bizName)
                .bizVersion(bizVersion)
                .bizUrl(bizUrl)
                .attachment(attachment).build();
    }
}

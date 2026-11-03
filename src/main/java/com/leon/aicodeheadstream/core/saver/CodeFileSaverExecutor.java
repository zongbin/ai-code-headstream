package com.leon.aicodeheadstream.core.saver;

import com.leon.aicodeheadstream.ai.model.HtmlCodeResult;
import com.leon.aicodeheadstream.ai.model.MultiFileCodeResult;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {

    private static final MultiFileCodeFileSaverTemplate MULTI_FILE_CODE_FILE_SAVER_TEMPLATE = new MultiFileCodeFileSaverTemplate();
    private static final HtmlCodeFileSaverTemplate HTML_CODE_FILE_SAVER_TEMPLATE = new HtmlCodeFileSaverTemplate();

    public static File execute(Object result, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case MULTI_FILE -> MULTI_FILE_CODE_FILE_SAVER_TEMPLATE.save((MultiFileCodeResult) result);
            case HTML -> HTML_CODE_FILE_SAVER_TEMPLATE.save((HtmlCodeResult) result);
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unknown codeGenTypeEnum: " + codeGenTypeEnum);
        };

    }
}

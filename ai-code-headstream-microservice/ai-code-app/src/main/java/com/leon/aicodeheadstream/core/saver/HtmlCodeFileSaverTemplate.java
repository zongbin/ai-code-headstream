package com.leon.aicodeheadstream.core.saver;

import cn.hutool.core.io.FileUtil;
import com.leon.aicodeheadstream.ai.model.HtmlCodeResult;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeGenTypeEnum() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void validate(HtmlCodeResult result) {
        super.validate(result);
        if (result.getHtmlCode() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "result.htmlCode must not be null");
        }
    }

    @Override
    protected void saveFile(String dirPath, HtmlCodeResult result) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
    }
}

package com.leon.aicodeheadstream.core.saver;

import cn.hutool.core.io.FileUtil;
import com.leon.aicodeheadstream.ai.model.HtmlCodeResult;
import com.leon.aicodeheadstream.ai.model.MultiFileCodeResult;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeGenTypeEnum() {
        return CodeGenTypeEnum.MULTI_FILE;
    }


    @Override
    protected void validate(MultiFileCodeResult result) {
        super.validate(result);
        if (result.getHtmlCode() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "result.htmlCode must not be null");
        }
    }

    @Override
    protected void saveFile(String dirPath, MultiFileCodeResult result) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());
    }
}

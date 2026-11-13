package com.leon.aicodeheadstream.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.leon.aicodeheadstream.constant.AppConstant;
import com.leon.aicodeheadstream.exception.BusinessException;
import com.leon.aicodeheadstream.exception.ErrorCode;
import com.leon.aicodeheadstream.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public abstract class CodeFileSaverTemplate<T> {

    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;


    public final File save(T result, Long appId) {
        // 1、校验参数
        validate(result);
        // 2、构建唯一目录路径
        String baseDirPath = buildUniqueDir(appId);
        // 3、写入文件
        saveFile(baseDirPath, result);
        return new File(baseDirPath);

    }

    protected final String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "appId must not be blank");
        }
        String uniqueDirName = StrUtil.format("{}_{}", getCodeGenTypeEnum().getValue(), appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    protected final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isBlank(content)) {
            return;
        }
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }


    protected void validate(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "result must not be null");
        }
    }

    protected abstract CodeGenTypeEnum getCodeGenTypeEnum();

    protected abstract void saveFile(String dirPath, T result);

}

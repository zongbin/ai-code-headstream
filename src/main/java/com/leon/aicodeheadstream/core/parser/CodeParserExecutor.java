package com.leon.aicodeheadstream.core.parser;

import com.leon.aicodeheadstream.model.enums.CodeGenTypeEnum;

public class CodeParserExecutor {

    private static final HtmlCodeParser HTML_CODE_PARSER = new HtmlCodeParser();
    private static final MultiFileCodeParser MULTI_FILE_CODE_PARSER = new MultiFileCodeParser();

    public static Object execute(String content, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case HTML -> HTML_CODE_PARSER.parse(content);
            case MULTI_FILE -> MULTI_FILE_CODE_PARSER.parse(content);
            default -> throw new IllegalArgumentException("Unknown code gen type: " + codeGenTypeEnum);
        };
    }
}

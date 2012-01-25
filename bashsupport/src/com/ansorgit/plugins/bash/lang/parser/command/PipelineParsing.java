/*******************************************************************************
 * Copyright 2011 Joachim Ansorg, mail@ansorg-it.com
 * File: PipelineParsing.java, Class: PipelineParsing
 * Last modified: 2011-04-30 16:33
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.ansorgit.plugins.bash.lang.parser.command;

import com.ansorgit.plugins.bash.lang.parser.BashPsiBuilder;
import com.ansorgit.plugins.bash.lang.parser.Parsing;
import com.ansorgit.plugins.bash.lang.parser.ParsingTool;
import com.ansorgit.plugins.bash.lang.parser.util.ParserUtil;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;

/**
 * Date: 25.03.2009
 * Time: 11:37:48
 *
 * @author Joachim Ansorg
 */
public class PipelineParsing implements ParsingTool {
    private enum ParseState {
        OK_PIPELINE,
        OK_NO_PIPELINE,
        ERROR,
    }

    public boolean isPipelineCommand(BashPsiBuilder builder) {
        final PsiBuilder.Marker start = builder.mark();
        try {
            if (isPipeline(builder)) {
                return true;
            }

            final IElementType firstToken = ParserUtil.getTokenAndAdvance(builder);
            final IElementType secondToken = builder.getTokenType();

            if ((firstToken == TIME_KEYWORD && secondToken == BANG_TOKEN) || (firstToken == BANG_TOKEN && secondToken == TIME_KEYWORD)) {
                builder.advanceLexer();
                return isPipeline(builder);
            }

            if (firstToken == TIME_KEYWORD) {
                return Parsing.list.isListTerminator(secondToken) || isPipeline(builder);
            }

            return firstToken == BANG_TOKEN && isPipeline(builder);

        } finally {
            start.rollbackTo();
        }
    }

    boolean isPipeline(BashPsiBuilder builder) {
        return Parsing.command.isValid(builder);
    }

    /*
   pipeline_command:
           pipeline
       |	BANG pipeline
       |	timespec pipeline
       |	timespec BANG pipeline
       |	BANG timespec pipeline
       |	timespec list_terminator
       ;

   pipeline:	command2 ('|' newline_list command2)*
       ;

    */

    public boolean parsePipelineCommand(BashPsiBuilder builder) {
        PsiBuilder.Marker pipelineCommandMarker = builder.mark();

        boolean hasBang = false;

        final IElementType firstToken = builder.getTokenType();
        if (firstToken == BANG_TOKEN) { //the bang is optional
            builder.advanceLexer();
            hasBang = true;
        }

        parseOptionalTimespec(builder);

        if (!hasBang && builder.getTokenType() == BANG_TOKEN) {
            builder.advanceLexer(); //after the bang token we found
        }

        if (Parsing.list.isListTerminator(builder.getTokenType())) {
            builder.advanceLexer();
            pipelineCommandMarker.drop();
            return true;
        }

        ParseState parseState = parsePipleline(builder);
        switch (parseState) {
            case OK_PIPELINE:
                pipelineCommandMarker.done(PIPELINE_COMMAND);
                return true;
            case OK_NO_PIPELINE:
                pipelineCommandMarker.drop();
                return true;
            case ERROR:
                pipelineCommandMarker.drop();
                return false;
            default:
                throw new IllegalStateException("Invalie switch/case value");
        }
    }

    /**
     * Parses a pipeline command. Marks if at least one pipeline is found. Does NOT mark
     * if just one command or an error has been found.
     *
     * @param builder The builder to use
     * @return True if no errors occured
     */
    ParseState parsePipleline(BashPsiBuilder builder) {
        if (!Parsing.command.parse(builder)) {
            return ParseState.ERROR;
        }

        boolean withPipeline = pipeTokens.contains(builder.getTokenType());
        if (!withPipeline) {
            return ParseState.OK_NO_PIPELINE;
        }

        boolean result = true;
        while (result && pipeTokens.contains(builder.getTokenType())) {
            builder.advanceLexer(); //eat the pipe token
            builder.eatOptionalNewlines();

            result = Parsing.command.parse(builder);
        }

        if (!result) {
            return ParseState.ERROR;
        }

        return ParseState.OK_PIPELINE;
    }

    boolean isTimespec(BashPsiBuilder builder) {
        return builder.getTokenType() == TIME_KEYWORD;
    }

    boolean parseTimespec(BashPsiBuilder builder) {
        final PsiBuilder.Marker time = builder.mark();

        final IElementType timeToken = ParserUtil.getTokenAndAdvance(builder);
        if (timeToken != TIME_KEYWORD) {
            ParserUtil.error(time, "parser.time.expected.time");
            return false;
        }

        if (ParserUtil.isWordToken(builder.getTokenType()) && "-p".equals(builder.getTokenText())) {
            builder.advanceLexer();
        }

        time.done(TIME_COMMAND);
        return true;
    }

    private boolean parseOptionalTimespec(BashPsiBuilder builder) {
        return isTimespec(builder) && parseTimespec(builder);
    }
}

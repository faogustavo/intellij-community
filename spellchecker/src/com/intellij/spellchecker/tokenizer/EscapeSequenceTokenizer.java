// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spellchecker.tokenizer;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import org.jetbrains.annotations.NotNull;

public abstract class EscapeSequenceTokenizer<T extends PsiElement> extends Tokenizer<T> {
  private static final Key<int[]> ESCAPE_OFFSETS = Key.create("escape.tokenizer.offsets");

  public static void processTextWithOffsets(PsiElement element, TokenConsumer consumer, StringBuilder unescapedText,
                                            int[] offsets, int startOffset) {
    if (element != null) element.putUserData(ESCAPE_OFFSETS, offsets);
    final String text = unescapedText.toString();
    consumer.consumeToken(element, text, false, startOffset, TextRange.allOf(text), PlainTextSplitter.getInstance());
    if (element != null) element.putUserData(ESCAPE_OFFSETS, null);
  }

  @Override
  public @NotNull TextRange getHighlightingRange(@NotNull PsiElement element, int offset, @NotNull TextRange range) {
    final int[] offsets = element.getUserData(ESCAPE_OFFSETS);
    if (offsets != null) {
      int start = offsets[range.getStartOffset()];
      int end = offsets[range.getEndOffset()];

      return new TextRange(offset + start, offset + end);
    }
    return super.getHighlightingRange(element, offset, range);
  }
}

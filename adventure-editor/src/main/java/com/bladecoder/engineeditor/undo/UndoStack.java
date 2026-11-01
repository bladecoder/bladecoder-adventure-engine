package com.bladecoder.engineeditor.undo;

import com.bladecoder.engineeditor.common.EditorLogger;
import java.util.ArrayDeque;
import java.util.Deque;

public class UndoStack {
  Deque<UndoOp> stack = new ArrayDeque<UndoOp>();

  public void add(UndoOp op) {
    stack.push(op);
  }

  public void undo() {
    if (!stack.isEmpty()) {
      UndoOp op = stack.pop();
      op.undo();

      EditorLogger.debug("UNDO: " + op);
    }
  }

  public void clear() {
    stack.clear();
  }
}

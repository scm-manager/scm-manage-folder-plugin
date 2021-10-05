package com.cloudogu.scm.folder;

import sonia.scm.ContextEntry;
import sonia.scm.ExceptionWithContext;

import java.util.List;

public class PathIsNotADirectoryException extends ExceptionWithContext {
  protected PathIsNotADirectoryException(List<ContextEntry> context, String message) {
    super(context, message);
  }

  @Override
  public String getCode() {
    return "B0Skx4uOG1";
  }
}

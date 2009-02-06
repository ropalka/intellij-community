/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.pom;

/**
 * @author peter
 */
public interface PomTarget extends Navigatable {
  PomTarget[] EMPTY_ARRAY = new PomTarget[0];

  boolean isValid();
}

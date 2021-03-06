/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.profile.codeInspection.ui;

import com.intellij.codeInspection.ex.NewInspectionProfile;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO should replace current implementation
public class InspectionProfileImporter implements SchemeImporter<NewInspectionProfile> {
  @Override
  public String @NotNull [] getSourceExtensions() {
    return new String[] {"xml"};
  }

  @Nullable
  @Override
  public NewInspectionProfile importScheme(@NotNull Project project,
                                           @NotNull VirtualFile selectedFile,
                                           @NotNull NewInspectionProfile currentScheme,
                                           @NotNull SchemeFactory<? extends NewInspectionProfile> schemeFactory) throws SchemeImportException {
    throw new UnsupportedOperationException();
  }
}

// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.diff.editor

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface DiffEditorTabFilesManager {

  fun isEditorDiffAvailable(): Boolean = AdvancedSettings.getBoolean(SHOW_DIFF_IN_EDITOR_SETTING)

  fun showDiffFile(diffFile: ChainDiffVirtualFile, focusEditor: Boolean): Array<out FileEditor>

  companion object {
    const val SHOW_DIFF_IN_EDITOR_SETTING = "show.diff.as.editor.tab"

    @JvmStatic
    fun isDiffOpenedInNewWindow(file: VirtualFile): Boolean = DIFF_OPENED_IN_NEW_WINDOW.get(file, false)

    @JvmStatic
    fun getInstance(project: Project): DiffEditorTabFilesManager = project.service()
  }
}

@JvmField
val DIFF_OPENED_IN_NEW_WINDOW = Key<Boolean>("DIFF_OPENED_IN_NEW_WINDOW")

class DefaultDiffTabFilesManager(private val project: Project) : DiffEditorTabFilesManager {

  override fun showDiffFile(diffFile: ChainDiffVirtualFile, focusEditor: Boolean): Array<out FileEditor> {
    return FileEditorManager.getInstance(project).openFile(diffFile, true)
  }
}

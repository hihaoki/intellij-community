// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.feedback

import com.intellij.CommonBundle
import com.intellij.ide.actions.AboutDialog
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.impl.ZenDeskForm
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.LicensingFacade
import com.intellij.ui.PopupBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.dialog
import com.intellij.ui.layout.*
import com.intellij.util.BooleanFunction
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

data class ZenDeskComboOption(val displayName: @Nls String, val id: String) {
  override fun toString(): String = displayName
}

private val topicOptions = listOf(
  ZenDeskComboOption(ApplicationBundle.message("feedback.form.topic.bug"), "ij_bug"),
  ZenDeskComboOption(ApplicationBundle.message("feedback.form.topic.howto"), "ij_howto"),
  ZenDeskComboOption(ApplicationBundle.message("feedback.form.topic.problem"), "ij_problem"),
  ZenDeskComboOption(ApplicationBundle.message("feedback.form.topic.suggestion"), "ij_suggestion"),
  ZenDeskComboOption(ApplicationBundle.message("feedback.form.topic.misc"), "ij_misc")
)

class FeedbackForm(
  private val project: Project?,
  val form: ZenDeskForm,
  val isEvaluation: Boolean
) : DialogWrapper(project, false) {
  private var details = ""
  private var email = LicensingFacade.INSTANCE.getLicenseeEmail().orEmpty()
  private var needSupport = false
  private var shareSystemInformation = false
  private var ratingComponent: RatingComponent? = null
  private var missingRatingTooltip: JComponent? = null
  private var topic: ZenDeskComboOption? = null

  init {
    title = ApplicationBundle.message("feedback.form.title")
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label(if (isEvaluation) ApplicationBundle.message("feedback.form.evaluation.prompt") else ApplicationBundle.message("feedback.form.prompt")).also {
          it.component.font = JBFont.h3().asBold()
        }
      }
      if (isEvaluation) {
        row {
          label(ApplicationBundle.message("feedback.form.comment")).also {
            it.component.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            it.component.minimumSize = Dimension(it.component.preferredSize.width, it.component.minimumSize.height)
          }
        }
        row {
          label(ApplicationBundle.message("feedback.form.rating", ApplicationNamesInfo.getInstance().fullProductName))
        }
        row {
          cell {
            ratingComponent = RatingComponent().also {
              it.addPropertyChangeListener { evt ->
                if (evt.propertyName == RatingComponent.RATING_PROPERTY) {
                  missingRatingTooltip?.isVisible = false
                }
              }
              it()
            }

            missingRatingTooltip = JBLabel(ApplicationBundle.message("feedback.form.rating.required")).apply {
              border = JBUI.Borders.compound(PopupBorder.Factory.createColored(JBUI.CurrentTheme.Validator.errorBorderColor()),
                                             JBUI.Borders.empty(4, 8))
              background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
              isVisible = false
              isOpaque = true
              this().withLargeLeftGap()
            }
          }
        }
      }
      else {
        row {
          label(ApplicationBundle.message("feedback.form.topic"))
        }
        row {
          comboBox(CollectionComboBoxModel(topicOptions), { topic }, { topic = it})
            .withErrorOnApplyIf(ApplicationBundle.message("feedback.form.topic.required")) {
              it.selectedItem == null
            }
        }
      }
      row {
        label(if (isEvaluation) ApplicationBundle.message("feedback.form.evaluation.details") else ApplicationBundle.message("feedback.form.details"))
      }
      row {
        scrollableTextArea(::details, rows = 5)
          .focused()
          .withErrorOnApplyIf(ApplicationBundle.message("feedback.form.details.required")) {
            it.text.isBlank()
          }
          .also {
            it.component.emptyText.text = ApplicationBundle.message("feedback.form.details.emptyText")
            it.component.putClientProperty(JBTextArea.STATUS_VISIBLE_FUNCTION,
                                           BooleanFunction<JBTextArea> { textArea -> textArea.text.isEmpty() })

            it.component.addKeyListener(object : KeyAdapter() {
              override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_TAB) {
                  if ((e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK) != 0) {
                    it.component.transferFocusBackward()
                  }
                  else {
                    it.component.transferFocus()
                  }
                  e.consume()
                }
              }
            })
          }
      }
      row {
        label(ApplicationBundle.message("feedback.form.email"))
      }
      row {
        textField(::email, columns = 25)
          .withErrorOnApplyIf(ApplicationBundle.message("feedback.form.email.required")) { it.text.isBlank() }

      }
      row {
        checkBox(ApplicationBundle.message("feedback.form.need.support"), ::needSupport)
      }
      row {
        cell {
          checkBox(ApplicationBundle.message("feedback.form.share.system.information"), ::shareSystemInformation)
          HyperlinkLabel(ApplicationBundle.message("feedback.form.share.system.information.link"))().also {
            it.component.addHyperlinkListener {
              showSystemInformation()
            }
          }
        }
      }
      row {
        label(ApplicationBundle.message("feedback.form.consent")).also {
          UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, it.component)
          it.component.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
          it.component.minimumSize = Dimension(it.component.preferredSize.width, it.component.minimumSize.height)
        }
      }
    }
  }

  private fun showSystemInformation() {
    val systemInfo = AboutDialog(project).extendedAboutText
    val scrollPane = JBScrollPane(JBTextArea(systemInfo))
    dialog(ApplicationBundle.message("feedback.form.system.information.title"), scrollPane, createActions = {
      listOf(object : AbstractAction(CommonBundle.getCloseButtonText()) {
        init {
          putValue(DEFAULT_ACTION, true)
        }

        override fun actionPerformed(event: ActionEvent) {
          val wrapper = findInstance(event.source as? Component)
          wrapper?.close(OK_EXIT_CODE)
        }
      })
    }).show()
  }

  override fun getOKAction(): Action {
    return object : DialogWrapper.OkAction() {
      init {
        putValue(Action.NAME, ApplicationBundle.message("feedback.form.ok"))
      }

      override fun doAction(e: ActionEvent) {
        val ratingComponent = ratingComponent
        missingRatingTooltip?.isVisible = ratingComponent?.rating == 0
        if (ratingComponent == null || ratingComponent.rating != 0) {
          super.doAction(e)
        }
        else {
          enabled = false
        }
      }
    }
  }

  override fun getCancelAction(): Action {
    return super.getCancelAction().apply {
      putValue(Action.NAME, ApplicationBundle.message("feedback.form.cancel"))
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    val systemInfo = if (shareSystemInformation) AboutDialog(project).extendedAboutText else ""
    ApplicationManager.getApplication().executeOnPooledThread {
      ZenDeskRequests().submit(
        form,
        email,
        ApplicationNamesInfo.getInstance().fullProductName + " Feedback",
        details.ifEmpty { "No details" },
        mapOf(
          "systeminfo" to systemInfo,
          "needsupport" to needSupport
        ) + (ratingComponent?.let { mapOf("rating" to it.rating) } ?: mapOf()) + (topic?.let { mapOf("topic" to it.id)} ?: emptyMap() )
      ) {
        ApplicationManager.getApplication().invokeLater {
          var message = ApplicationBundle.message("feedback.form.thanks", ApplicationNamesInfo.getInstance().fullProductName)
          if (isEvaluation) {
            message += "<br/>" + ApplicationBundle.message("feedback.form.share.later")
          }
          Notification("feedback.form",
                       ApplicationBundle.message("feedback.form.thanks.title"),
                       message,
                       NotificationType.INFORMATION).notify(project)
        }
      }
    }
  }

  override fun doCancelAction() {
    super.doCancelAction()
    Notification("feedback.form", ApplicationBundle.message("feedback.form.share.later"), NotificationType.INFORMATION).notify(project)
  }
}

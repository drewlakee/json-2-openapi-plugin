package io.github.drewlakee.idea.plugin.jooas

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.EditorTextField
import com.intellij.ui.EditorTextFieldProvider
import com.intellij.ui.components.Label
import com.intellij.util.ui.UIUtil
import java.awt.event.HierarchyEvent
import javax.swing.JPanel

class HighlightTextEditPanel(
    language: Language,
    project: Project,
) : JPanel() {
    private var textField = editorTextField(language, project)

    init {
        layout =
            VerticalFlowLayout().apply {
                verticalFill = true
                horizontalFill = true
            }
        add(Label(language.displayName))
        add(textField)
    }

    fun getText() = textField.text

    fun setText(value: String) {
        textField.text = value
    }

    private fun editorTextField(
        language: Language,
        project: Project,
    ): EditorTextField =
        EditorTextFieldProvider.getInstance().getEditorField(language, project, emptyList()).apply {
            preferredSize = size

            val backgroundColor = UIUtil.getTableBackground()
            val brightness = 0.2126 * backgroundColor.red + 0.7152 * backgroundColor.green + 0.0722 * backgroundColor.blue
            background =
                if (brightness > 128) {
                    background.brighter()
                } else {
                    background.darker()
                }

            addHierarchyListener {
                if (it.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && it.changed.isShowing) {
                    updateSettings(this)
                }
            }
        }

    private fun updateSettings(editorTextField: EditorTextField) {
        editorTextField.editor?.settings.apply {
            this?.isFoldingOutlineShown = true
            this?.isLineNumbersShown = true
        }
    }
}

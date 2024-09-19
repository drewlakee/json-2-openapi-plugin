package io.github.drewlakee.plugins.json.openapi

import com.intellij.lang.Language
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.MockDocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.EditorTextField
import com.intellij.ui.EditorTextFieldProvider
import com.intellij.ui.components.Label
import java.awt.event.HierarchyEvent
import javax.swing.JPanel

class HighlightTextEditPanel(
    language: Language,
    project: Project,
    onTextChangeFunction: ((eventSource: EditorTextField) -> Unit)? = null,
) : JPanel() {
    private var textField = editorTextField(language, project, onTextChangeFunction)

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

    fun invokeTextChange() {
        textField.documentChanged(
            MockDocumentEvent(
                textField.document,
                0,
            ),
        )
    }

    private fun editorTextField(
        language: Language,
        project: Project,
        onTextChangeFunction: ((eventSource: EditorTextField) -> Unit)? = null,
    ): EditorTextField =
        EditorTextFieldProvider.getInstance().getEditorField(language, project, emptyList()).apply {
            preferredSize = size

            val thisTextEditor = this
            onTextChangeFunction?.let {
                addDocumentListener(
                    object : DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            it.invoke(thisTextEditor)
                        }
                    },
                )
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

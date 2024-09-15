package io.github.drewlakee.idea.plugin.jooas

import com.intellij.lang.Language
import com.intellij.openapi.observable.util.whenStateChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.EditorTextField
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import io.github.jooas.adapters.Features
import io.github.jooas.adapters.JsonOpenApiSchemaAdapter
import io.github.jooas.adapters.exceptions.JsonEmptyObjectException
import io.github.jooas.adapters.exceptions.JsonIsNotAnObjectException
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JComponent

class ConvertingToolWindow(
    project: Project,
    adapters: Map<Set<Features.Feature>, JsonOpenApiSchemaAdapter>,
) : SimpleToolWindowPanel(true) {
    private val featureCheckBoxes: Map<Features.Feature, JBCheckBox> =
        Features.Feature.entries
            .asSequence()
            .map {
                it to
                    JBCheckBox().apply {
                        text = it.name
                        isSelected = true
                        whenStateChanged { jsonTextEditor.invokeTextChange() }
                    }
            }.toMap()

    private val yamlTextEditor = HighlightTextEditPanel(Language.findLanguageByID("yaml")!!, project)
    private val jsonTextEditor =
        HighlightTextEditPanel(Language.findLanguageByID("JSON")!!, project) { eventSource: EditorTextField ->
            try {
                val sourceText = eventSource.getText()
                if (sourceText.isBlank()) return@HighlightTextEditPanel
                val adapter =
                    adapters[
                        featureCheckBoxes
                            .asSequence()
                            .filter { it.value.isSelected }
                            .map { it.key }
                            .toSet(),
                    ]!!
                yamlTextEditor.setText(adapter.convert(sourceText))
            } catch (e: Exception) {
                var errorMessage = e.localizedMessage
                if (e is JsonIsNotAnObjectException || e is JsonEmptyObjectException) {
                    errorMessage = "Input string expected to be valid JSON Object"
                }

                if (errorMessage == null) {
                    val stackTraceBuilder = StringBuilder()
                    e.stackTrace.forEach { stackTraceBuilder.append(it).append("\n") }
                    errorMessage = stackTraceBuilder.toString()
                }
                yamlTextEditor.setText(errorMessage)
            }
        }
    private val clearJsonButton =
        JButton().apply {
            text = "Clear"
            addActionListener { jsonTextEditor.setText("") }
        }
    private val copySchemaButton =
        JButton().apply {
            text = "Copy"
            addActionListener {
                val stringSelection = StringSelection(yamlTextEditor.getText())
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
                clipboard.setContents(stringSelection, null)
            }
        }

    init {
        add(editorPanel())
    }

    fun editorPanel(): JComponent =
        panel {
            row {
                cell(TitledSeparator("Features"))
                featureCheckBoxes
                    .asSequence()
                    .map { cell(it.value).align(AlignX.LEFT).align(AlignY.TOP) }
                    .toList()
            }
            row {
                cell(clearJsonButton)
                    .align(AlignX.LEFT)
                    .align(AlignY.TOP)

                cell(copySchemaButton)
                    .align(AlignX.CENTER)
                    .align(AlignY.TOP)
            }
            row {
                cell(jsonTextEditor)
                    .align(AlignX.LEFT)
                    .align(Align.CENTER)
                    .align(Align.FILL)
                    .resizableColumn()
                    .align(AlignY.FILL)

                cell(yamlTextEditor)
                    .align(AlignX.RIGHT)
                    .align(Align.CENTER)
                    .align(Align.FILL)
                    .resizableColumn()
                    .align(AlignY.FILL)
            }.resizableRow()
        }
}

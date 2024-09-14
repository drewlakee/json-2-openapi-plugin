package io.github.drewlakee.idea.plugin.jooas

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.WindowManagerImpl
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import io.github.jooas.adapters.AdaptersFactory
import io.github.jooas.adapters.Features
import io.github.jooas.adapters.exceptions.JsonIsNotAnObjectException
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JComponent

class ConvertingDialogWrapper(
    project: Project,
) : DialogWrapper(true) {
    private val jsonTextEditor = HighlightTextEditPanel(Language.findLanguageByID("JSON")!!, project)
    private val yamlTextEditor = HighlightTextEditPanel(Language.findLanguageByID("yaml")!!, project)
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
    private val featureCheckBoxes: Map<Features.Feature, JBCheckBox> =
        mapOf(
            Features.Feature.WITH_EXAMPLE to
                JBCheckBox().apply {
                    text = "WITH_EXAMPLE"
                    isSelected = true
                },
            Features.Feature.OBJECT_REFERENCE to
                JBCheckBox().apply {
                    text = "OBJECT_REFERENCE"
                    isSelected = true
                },
        )

    init {
        init()
        title = "Json Object -> OpenAPI Schema"
        val size = (WindowManager.getInstance() as WindowManagerImpl).getIdeFrame(project)?.component?.size!!
        setSize((size.width / 1.1).toInt(), (size.height / 1.1).toInt())
        setAutoAdjustable(false)
        setOKButtonText("Convert")
        setCancelButtonText("Close")
    }

    override fun createCenterPanel(): JComponent =
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
                    .align(AlignX.RIGHT)
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

    override fun doOKAction() {
        try {
            val sourceText = jsonTextEditor.getText()
            if (sourceText.isBlank()) return
            val adapter =
                AdaptersFactory.createObjectAdapter(
                    *featureCheckBoxes
                        .asSequence()
                        .map { Pair(it.key, it.value.isSelected) }
                        .toList()
                        .toTypedArray(),
                )
            yamlTextEditor.setText(adapter.convert(sourceText))
        } catch (e: Exception) {
            var errorMessage = e.localizedMessage
            if (e is JsonIsNotAnObjectException) {
                errorMessage = "Input string expected to be valid JSON"
            }

            if (errorMessage == null) {
                val stackTraceBuilder = StringBuilder()
                e.stackTrace.forEach { stackTraceBuilder.append(it).append("\n") }
                errorMessage = stackTraceBuilder.toString()
            }
            yamlTextEditor.setText(errorMessage)
        }
    }
}

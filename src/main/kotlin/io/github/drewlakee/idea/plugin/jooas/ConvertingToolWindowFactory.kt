package io.github.drewlakee.idea.plugin.jooas

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import io.github.jooas.adapters.AdaptersFactory
import io.github.jooas.adapters.Features
import io.github.jooas.adapters.JsonOpenApiSchemaAdapter
import java.util.LinkedList

class ConvertingToolWindowFactory :
    ToolWindowFactory,
    DumbAware {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val contentManager = toolWindow.contentManager

        val adapters = collectAdaptersPool()
        val view = ConvertingToolWindow(project, adapters)
        val tab = contentManager.factory.createContent(view, "Converter", false)
        contentManager.addContent(tab)
    }

    private fun collectAdaptersPool(): Map<Set<Features.Feature>, JsonOpenApiSchemaAdapter> {
        val featurePossibleCombinations = mutableListOf<List<Features.Feature>>()
        for (i in 0..Features.Feature.entries.size) {
            featurePossibleCombinations.addAll(combination(Features.Feature.entries.toList(), i))
        }

        val adapters =
            featurePossibleCombinations
                .asSequence()
                .map {
                    it.toSet() to
                        AdaptersFactory.createObjectAdapter(
                            *it
                                .asSequence()
                                .map { it to true }
                                .toList()
                                .toTypedArray(),
                        )
                }.toMap()
        return adapters
    }

    private fun <T> combination(
        values: List<T>,
        size: Int,
    ): List<List<T>> {
        if (0 == size) {
            return listOf(emptyList())
        }

        if (values.isEmpty()) {
            return emptyList()
        }

        val combination: MutableList<List<T>> = LinkedList()

        val actual = values.iterator().next()

        val subSet: MutableList<T> = LinkedList(values)
        subSet.remove(actual)

        val subSetCombination = combination(subSet, size - 1)

        for (set in subSetCombination) {
            val newSet: MutableList<T> = LinkedList(set)
            newSet.add(0, actual)
            combination.add(newSet)
        }

        combination.addAll(combination(subSet, size))

        return combination
    }
}

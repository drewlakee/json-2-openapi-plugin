package io.github.drewlakee.idea.plugin.jooas

import com.intellij.ide.impl.DataManagerImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class OpenJson2OpenapiAction : AnAction("Json2Openapi") {
    override fun actionPerformed(p0: AnActionEvent) {
        val project =
            DataManagerImpl
                .getInstance()
                .dataContextFromFocusAsync
                .blockingGet(0)
                ?.getData(CommonDataKeys.PROJECT)
        if (project != null) {
            ConvertingDialogWrapper(project).show()
        }
    }
}

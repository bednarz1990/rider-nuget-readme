package io.github.bednarz1990.nuget.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public final class ReadmeToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override public void createToolWindowContent(Project project, ToolWindow window) {
        ReadmePanel panel = new ReadmePanel();
        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        content.setDisposer(panel);
        window.getContentManager().addContent(content);
    }
}

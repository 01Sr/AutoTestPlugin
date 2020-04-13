import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateClassAction;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.JavaCreateFromTemplateHandler;
import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * @author cb
 * @date 2019/10/31
 */
public class NewPageClassAction extends CreateClassAction {

    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        // copy from CreateClassAction
        builder.setTitle(IdeBundle.message("action.create.new.class"))
                .addKind("Class", PlatformIcons.CLASS_ICON, JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME);

        builder.setValidator(new InputValidatorEx() {
            @Override
            public String getErrorText(String inputString) {
                if (inputString.length() > 0 && !PsiNameHelper.getInstance(project).isQualifiedName(inputString)) {
                    return "This is not a valid Java qualified name";
                }

                LanguageLevel level = PsiUtil.getLanguageLevel(directory);
                if (level.isAtLeast(LanguageLevel.JDK_10) && PsiKeyword.VAR.equals(StringUtil.getShortName(inputString))) {
                    return "var cannot be used for type declarations";
                }

                if(!inputString.endsWith("Page")) {
                    return "Must end with \"Page\"";
                }

                if(isLocatorFileExist(project, directory, inputString)) {
                    return inputString + ".xml has exist.";
                }
                return null;
            }

            @Override
            public boolean checkInput(String inputString) {
                return true;
            }

            @Override
            public boolean canClose(String inputString) {
                return !StringUtil.isEmptyOrSpaces(inputString) && getErrorText(inputString) == null;
            }
        });
    }

    @Nullable
    @Override
    protected PsiClass createFile(String name, String templateName, PsiDirectory dir) {
        final Project project = dir.getProject();
        PsiClass clazz = super.createFile(name, templateName, dir);
        for(PsiElement element : clazz.getChildren()) {
            if(element instanceof PsiJavaTokenImpl) {
                final PsiJavaTokenImpl psiJavaToken = (PsiJavaTokenImpl) element;
                if(psiJavaToken.getText().equals("{")) {
                    int offset = psiJavaToken.getTextOffset() + 1;
                    Document document = PsiDocumentManager.getInstance(dir.getProject()).getDocument(clazz.getContainingFile());
                    WriteCommandAction.runWriteCommandAction(dir.getProject(), () -> {
                        document.insertString(offset, "\n   private static final String " + name + "LocatorFile = \"locator/" + name + ".xml\";\n ");
                        createLocatorFile(project, dir, name);
                    });
                    break;
                }
            }
        }
        return clazz;
    }

    private boolean isLocatorFileExist(Project project, PsiDirectory directory, String name) {
        final String locatorFilePath = getLocatorFilePath(project, directory, name);
        return LocalFileSystem.getInstance().findFileByPath(locatorFilePath) != null;
    }

    private void createLocatorFile(Project project, PsiDirectory directory, String name) {
        final String locatorFilePath = getLocatorFilePath(project, directory, name);
        VirtualFile locatorVirtualFile = LocalFileSystem.getInstance().findFileByPath(locatorFilePath);
        if (locatorVirtualFile == null) {
            locatorVirtualFile = Util.createFile(locatorFilePath);
            XmlFile xmlFile = (XmlFile) PsiManager.getInstance(project).findFile(locatorVirtualFile);
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag == null || !rootTag.getLocalName().equals("Locator")) {
                if (!rootTag.getLocalName().equals("Locator")) {
                    rootTag.delete();
                }
                Document xmlDocument = PsiDocumentManager.getInstance(project).getDocument(xmlFile);
                xmlDocument.insertString(0, "<Locator>\n</Locator>");
            }
        }
    }

    private String getLocatorFilePath(Project project, PsiDirectory directory, String name) {
        return  ProjectRootManager.getInstance(project).getFileIndex()
                .getContentRootForFile(directory.getVirtualFile()).getPath()
                + File.separator +"src" + File.separator + "main" + File.separator + "resources"
                + File.separator + "locator" + File.separator + name + ".xml";
    }
}

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * @author cb
 * @date 2019/10/31
 */
class CreateElementInLocatorHandler implements InsertHandler<LookupElement> {
    public static final CreateElementInLocatorHandler INSTANCE = new CreateElementInLocatorHandler();

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        final String fileName = Util.getLocatorFileName(item);
        final String id = Util.getLocatorId(item);
        final String type = Util.getLocatorType(item);
        final String value = Util.getLocatorValue(item);
        final Editor editor = context.getEditor();
        final Project project = context.getEditor().getProject();

        final String locatorFilePath = fileName;
        VirtualFile locatorVirtualFile = LocalFileSystem.getInstance().findFileByPath(locatorFilePath);
        if(locatorVirtualFile == null) {
            locatorVirtualFile = Util.createFile(locatorFilePath);
        }
        if(locatorVirtualFile == null) {
            // todo toast fail message
        } else {
            XmlFile xmlFile = (XmlFile) PsiManager.getInstance(project).findFile(locatorVirtualFile);
            XmlTag rootTag = xmlFile.getRootTag();
            if(rootTag == null || !rootTag.getLocalName().equals("Locator")) {
                if(!rootTag.getLocalName().equals("Locator")) {
                    rootTag.delete();
                }
                Document xmlDocument = PsiDocumentManager.getInstance(project).getDocument(xmlFile);
                xmlDocument.insertString(0, "<Locator>\n    <Entry Id=\"" + id + "\" Type=\"" + type + "\">" + value + "</Entry>\n</Locator>");
            } else {
                XmlTag newTag = rootTag.createChildTag("Entry", rootTag.getNamespace(), value, false);
                newTag.setAttribute("Id", id);
                newTag.setAttribute("Type", type);
                xmlFile.getRootTag().addSubTag(newTag, false);
            }
        }


        final Document document = editor.getDocument();
        int start = context.getStartOffset();
        int end = context.getTailOffset();
        document.replaceString(start, end, "\"" + id + "\"");
    }
}


import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

/**
 * @author cb
 * @date 2019/10/31
 */
class InsertTypeHandler implements InsertHandler<LookupElement> {
    public static final InsertTypeHandler INSTANCE = new InsertTypeHandler();

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();
        int start = context.getStartOffset();
        int end = context.getTailOffset();
        WriteCommandAction.runWriteCommandAction(context.getProject(), () -> {
            document.replaceString(start, end, item.getLookupString());
        });
    }
}

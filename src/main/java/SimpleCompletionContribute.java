import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.psiReferenceExpression;

/**
 * @author cb
 * @date 2019/10/28
 */
public class SimpleCompletionContribute extends CompletionContributor {
    public SimpleCompletionContribute() {
        extend(CompletionType.BASIC, psiElement(PsiIdentifier.class).withParent(
                psiReferenceExpression().with(new PatternCondition<PsiReferenceExpression>("withMethodNameAndIndexOnLast") {
                    @Override
                    public boolean accepts(@NotNull PsiReferenceExpression psiReferenceExpression, ProcessingContext context) {
                        // 判断是方法中的最后一个参数
                        PsiElement literal = psiReferenceExpression;
                        do {
                            literal = literal.getNextSibling();
                            if(literal instanceof PsiExpression) {
                                return false;
                            }
                        } while(literal != null);

                        // 判断方法名是否以"locateElement"开头, 类名以"Page"结尾
                        PsiElement parent = psiReferenceExpression.getParent();
                        if(parent instanceof PsiExpressionList) {
                            parent = parent.getParent();
                            if(parent instanceof PsiMethodCallExpression) {
                                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) parent).getMethodExpression();
                                for(JavaResolveResult result : methodExpression.multiResolve(false)) {
                                    PsiMethod method = (PsiMethod) result.getElement();
                                    final PsiClass clazz = method.getContainingClass();
                                    return method.getName().startsWith("locateElement") && clazz.getName().endsWith("Page");
                                }
                            }
                        }
                        return false;
                    }
                })
                )
                , new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                        boolean isFullMatched = false;
                        final Project project = parameters.getEditor().getProject();
                        final String separator = "$$"; // "id;;value;;type"
                        String matchString = result.getPrefixMatcher().getPrefix();
                        final PsiFile psiFile = parameters.getOriginalFile();
                        if(!(psiFile instanceof PsiJavaFile)) {
                            return;
                        }

                        String locatorFilePath = null;
                        for(PsiClass psiClass : ((PsiJavaFile) psiFile).getClasses()) {
                            if(psiClass.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
                                for(PsiElement element : psiClass.getChildren()) {
                                    ProgressManager.checkCanceled();
                                    if(element instanceof  PsiField) {
                                        PsiField psiField = (PsiField) element;
                                        if(psiField.getTypeElement().getText().equals("String") && psiField.getName().equals(psiClass.getName() + "LocatorFile")) {
                                            PsiExpression psiExpression = psiField.getInitializer();
                                            if(psiExpression instanceof PsiLiteralExpression) {
                                                PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) psiExpression;
                                                String name = psiLiteralExpression.getText();
                                                name = name.substring(Integer.min(1, name.length() - 1), Integer.max(0, name.length() - 1));
                                                String path = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(psiFile.getVirtualFile()).getPath();
                                                locatorFilePath = path + File.separator +"src" + File.separator + "main" + File.separator + "resources" + File.separator + name;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if(locatorFilePath != null) {
                                break;
                            }
                        }
                        if(locatorFilePath == null) {
                            return;
                        }

                        String[] matchComponents = null;
                        if(matchString.contains(separator)) {
                            String[] tmp = StringUtil.split(matchString, separator).toArray(new String[0]);
                            if(tmp.length > 1) {
                                matchComponents = tmp;
                            } else {
                                matchString = matchString.substring(0, matchString.length() - 2);
                                result = result.withPrefixMatcher(matchString);
                            }
                        }

                        if(matchComponents != null && matchComponents.length > 2 && !matchComponents[2].isEmpty()) {
                            CompletionResultSet matchTypeResultSet = result.withPrefixMatcher(matchComponents[2]);
                            String[] types = {"ById", "ByAccessibilityId", "ByXPath", "ByClassName", "ByName", "ByIosNsPredicate"};
                            for(String availableType : types) {
                                matchTypeResultSet.addElement(LookupElementBuilder
                                        .create(availableType)
                                        .withCaseSensitivity(false)
                                        .withTypeText("TYPE")
                                        .withInsertHandler(InsertTypeHandler.INSTANCE));
                            }
                        }

                        // 自动在xml文件创建node
                        CompletionResultSet customResultSet = result.withPrefixMatcher(new PrefixMatcher(matchString) {
                            @Override
                            public boolean prefixMatches(@NotNull String name) {
                                return true;
                            }

                            @NotNull
                            @Override
                            public PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
                                return this;
                            }
                        });
                        String id;
                        String value;
                        String type;
                        if(matchComponents != null) {
                            id = matchComponents[0];
                            value = matchComponents[1];
                            if(matchComponents.length > 2) {
                                type = matchComponents[2];
                            } else {
                                type = "ByAccessibilityId";
                            }
                        } else {
                            id = matchString;
                            value = matchString;
                            type = "ByAccessibilityId";
                        }
                        LookupElement lookupCreateNodeElement = LookupElementBuilder
                                .create("$$Create element with ID: " + id + " with type: " + type + " with value: " + value + " in file: ")
                                .withTypeText("VALUE")
                                .withInsertHandler(CreateElementInLocatorHandler.INSTANCE);
                        Util.setLocatorData(lookupCreateNodeElement, locatorFilePath, id, type, value);
                        customResultSet.addElement(lookupCreateNodeElement);

                        VirtualFile locatorVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(locatorFilePath);
                        if(locatorVirtualFile == null) {
                            return;
                        }
                        ProgressManager.checkCanceled();

                        XmlFile xmlFile = (XmlFile) PsiManager.getInstance(parameters.getEditor().getProject()).findFile(locatorVirtualFile);
                        XmlTag[] tags = xmlFile.getRootTag().getSubTags();
                        for (XmlTag tag : tags) {
                            ProgressManager.checkCanceled();
                            id = tag.getAttributeValue("Id");
                            type = tag.getAttributeValue("Type");
                            value = tag.getValue().getText();
                            System.out.println("id: " + id + " type: " + type + " value: " + value);
                            LookupElement lookupID =  LookupElementBuilder
                                    .create(id)
                                    .withCaseSensitivity(false)
                                    .withTailText(type, true)
                                    .withTypeText("ID")
                                    .withInsertHandler(InsertIdHandler.INSTANCE);
                            Util.setLocatorData(lookupID, locatorFilePath, id, type, value);
                            result.addElement(lookupID);


                            LookupElement lookupValue = LookupElementBuilder
                                    .create(value)
                                    .withCaseSensitivity(false)
                                    .withTailText(type, true)
                                    .withTypeText("VALUE")
                                    .withInsertHandler(InsertIdHandler.INSTANCE);
                            Util.setLocatorData(lookupValue, locatorFilePath, id, type, value);
                            result.addElement(lookupValue);

                            // 没有在locator中匹配到元素或者是手动触发
                            if(matchComponents != null) {
                                if(matchComponents[0].equals(lookupID.getLookupString().toLowerCase())) {
                                    isFullMatched = true;
                                }
                            } else if(matchString.equals(lookupID.getLookupString().toLowerCase()) || matchString.equals(lookupValue.getLookupString().toLowerCase())) {
                                isFullMatched = true;
                            }
                        }
                    }
        });
    }
}

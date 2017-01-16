package wangyi.plugin.singleton.builder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * author WangYi
 * created on 2017/1/15.
 * 饿汉式
 */
public class HungryPatternBuilder extends BaseBuilder {
    public HungryPatternBuilder(AnActionEvent event) {
        super(event);
    }

    @Override
    public void build() {
        PsiFile psiFile = getActionEvent().getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) return;

        new WriteCommandAction.Simple(getActionEvent().getProject(), psiFile) {
            @Override
            protected void run() throws Throwable {
                Editor editor = getActionEvent().getData(PlatformDataKeys.EDITOR);
                if (editor == null) return;
                Project project = editor.getProject();
                if (project == null) return;

                PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
                PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                if (psiClass == null) return;

                if (psiClass.getNameIdentifier() == null) return;
                String className = psiClass.getNameIdentifier().getText();

                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);

                if (psiClass.getConstructors().length == 0) {
                    PsiMethod constructor = elementFactory.createConstructor();
                    constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
                    psiClass.add(constructor);
                }

                PsiType psiType = PsiType.getTypeByName(className, project
                        , GlobalSearchScope.EMPTY_SCOPE);
                PsiField psiField = elementFactory.createField(buildFiledText(), psiType);

                if (!containFiled(psiClass, psiField)) {
                    PsiModifierList modifierList = psiField.getModifierList();
                    if (modifierList == null) return;
                    modifierList.setModifierProperty(PsiModifier.STATIC, true);

                    PsiExpression psiInitializer = elementFactory.createExpressionFromText(buildInitializerText(className), psiField);
                    psiField.setInitializer(psiInitializer);
                    psiClass.add(psiField);
                }

                String methodText = buildMethodText(className);
                PsiMethod psiMethod = elementFactory.createMethodFromText(methodText, psiClass);
                if (!containMethod(psiClass, psiMethod)) {
                    psiClass.add(psiMethod);
                }

                CodeStyleManager.getInstance(project).reformat(psiClass);
            }
        }.execute();
    }

    private String buildMethodText(String className) {
        return "public static " + className + " getInstance() {\n" +
                "        return " + buildFiledText() + ";\n" +
                "    }";
    }

    private String buildFiledText() {
        return "INSTANCE";
    }

    private String buildInitializerText(String className) {
        return "new " + className + "()";
    }
}
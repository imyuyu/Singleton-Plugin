package wangyi.plugin.singleton.builder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * author WangYi
 * created on 2017/1/15.
 */
public class EnumPatternBuilder extends BaseBuilder {
    public EnumPatternBuilder(AnActionEvent event) {
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

                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
                PsiEnumConstant enumConstant = elementFactory.createEnumConstantFromText("INSTANCE", psiClass);

                if (!containFiled(psiClass, enumConstant)) {
                    psiClass.add(enumConstant);
                }

                if (!psiClass.isEnum()) {
                    CodeStyleManager.getInstance(project).reformat(psiClass);
                    String text = " class " + className;
                    int index = editor.getDocument().getText().indexOf(text);
                    if (index != -1) {
                        editor.getDocument().replaceString(index, index + text.length(), " enum " + className);
                    }
                }
            }
        }.execute();
    }
}
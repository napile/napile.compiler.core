package protobuf.lang.psi.impl.auxiliary;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import protobuf.lang.psi.api.auxiliary.PbNamedElement;
import protobuf.lang.psi.impl.PbPsiElementImpl;

/**
 * @author Nikolay Matveev
 */

public abstract class PbNamedElementImpl extends PbPsiElementImpl implements PbNamedElement {

    public PbNamedElementImpl(ASTNode node) {
        super(node);
    }

    @Override
    public abstract PsiElement getNameElement();

    @Override
    public final PsiElement setName(@NotNull String newName) throws IncorrectOperationException{
        throw new UnsupportedOperationException();
    }

    @Override
    public final String getName() {
        PsiElement nameElement = getNameElement();
        if (nameElement != null) {
            return nameElement.getText();
        }
        return null;
    }

    @Override
    public final int getTextOffset() {
        PsiElement nameElement = getNameElement();
        if(nameElement != null){
            return super.getTextOffset() + nameElement.getStartOffsetInParent();
        }
        return super.getTextOffset();
    }
}

package protobuf.lang;

import com.intellij.psi.tree.IElementType;
import protobuf.PbLanguage;


/**
 * @author Nikolay Matveev
 * Date: Mar 5, 2010
 */
public class PbElementType extends IElementType {
    public PbElementType(String debugName){
        super(debugName, PbLanguage.INSTANCE);
    }
}

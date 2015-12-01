package com.ansorgit.plugins.bash.lang.psi.stubs.elements;

import com.ansorgit.plugins.bash.lang.parser.BashElementTypes;
import com.ansorgit.plugins.bash.lang.psi.BashStubElementType;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVar;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVarDef;
import com.ansorgit.plugins.bash.lang.psi.impl.vars.BashVarDefImpl;
import com.ansorgit.plugins.bash.lang.psi.impl.vars.BashVarImpl;
import com.ansorgit.plugins.bash.lang.psi.stubs.api.BashVarDefStub;
import com.ansorgit.plugins.bash.lang.psi.stubs.api.BashVarStub;
import com.ansorgit.plugins.bash.lang.psi.stubs.impl.BashVarDefStubImpl;
import com.ansorgit.plugins.bash.lang.psi.stubs.impl.BashVarStubImpl;
import com.ansorgit.plugins.bash.lang.psi.stubs.index.BashVarDefIndex;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author jansorg
 */
public class BashVarElementType extends BashStubElementType<BashVarStub, BashVar> {
    public BashVarElementType() {
        super("var-use-element");
    }

    @NotNull
    @Override
    public String getExternalId() {
        return "bash.var";
    }

    public void serialize(@NotNull BashVarStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
    }

    @NotNull
    public BashVarStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef ref = dataStream.readName();
        return new BashVarStubImpl(parentStub, ref, this);
    }

    public BashVar createPsi(@NotNull BashVarStub stub) {
        return new BashVarImpl(stub, BashElementTypes.VAR_ELEMENT);
    }

    public BashVarStub createStub(@NotNull BashVar psi, StubElement parentStub) {
        return new BashVarStubImpl(parentStub, StringRef.fromString(psi.getName()), BashElementTypes.VAR_ELEMENT);
    }

    @Override
    public void indexStub(@NotNull BashVarStub stub, @NotNull IndexSink sink) {
        final String name = stub.getName();
        if (name != null) {
            sink.occurrence(BashVarDefIndex.KEY, name);
        }
    }
}

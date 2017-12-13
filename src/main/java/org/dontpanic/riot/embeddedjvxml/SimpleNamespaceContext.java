package org.dontpanic.riot.embeddedjvxml;

import javax.xml.namespace.NamespaceContext;
import java.util.Collections;
import java.util.Iterator;

public class SimpleNamespaceContext implements NamespaceContext {

    private String prefix;
    private String namespaceURI;

    public SimpleNamespaceContext(String prefix, String namespaceURI) {
        this.prefix = prefix;
        this.namespaceURI = namespaceURI;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return namespaceURI;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return prefix;
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        return Collections.singletonList(prefix).iterator();
    }
}

/*
 *
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2014 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.dontpanic.riot.embeddedjvxml;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.jvoicexml.*;
import org.jvoicexml.documentserver.JVoiceXmlDocumentServer;
import org.jvoicexml.documentserver.schemestrategy.FileSchemeStrategy;
import org.jvoicexml.documentserver.schemestrategy.HttpSchemeStrategy;
import org.jvoicexml.implementation.PlatformFactory;
import org.jvoicexml.implementation.dtmf.BufferedDtmfInput;
import org.jvoicexml.implementation.jvxml.JVoiceXmlImplementationPlatformFactory;
import org.jvoicexml.implementation.text.TextPlatformFactory;
import org.jvoicexml.interpreter.DialogFactory;
import org.jvoicexml.interpreter.GrammarProcessor;
import org.jvoicexml.interpreter.datamodel.DataModel;
import org.jvoicexml.interpreter.datamodel.ecmascript.EcmaScriptDataModel;
import org.jvoicexml.interpreter.dialog.*;
import org.jvoicexml.interpreter.grammar.GrammarIdentifier;
import org.jvoicexml.interpreter.grammar.JVoiceXmlGrammarProcessor;
import org.jvoicexml.interpreter.grammar.identifier.SrgsXmlGrammarIdentifier;
import org.jvoicexml.profile.Profile;
import org.jvoicexml.profile.TagStrategyFactory;
import org.jvoicexml.profile.vxml21.VoiceXml21Profile;
import org.jvoicexml.profile.vxml21.VoiceXml21TagStrategyFactory;
import org.jvoicexml.xml.vxml.Form;
import org.jvoicexml.xml.vxml.Menu;

/**
 * Dummy implementation of a configuration object.
 * Copied from JVoiceXML 0.7.7.GA embedded demo with minor modifications.
 *
 * TODO: Replace with Spring based config
 */
public class EmbeddedTextConfiguration implements Configuration {

    private static final Logger LOGGER = Logger.getLogger(EmbeddedTextConfiguration.class);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> loadObjects(final Class<T> baseClass,
                                         final String root) {
        final Collection<T> col = new java.util.ArrayList<T>();
        if (baseClass == TagStrategyFactory.class) {
            try {
                T value = (T) new EmbeddedTagStrategyFactory();
                col.add(value);
            } catch (Exception e) {
                return null;
            }
        } else if (baseClass == PlatformFactory.class) {
            final TextPlatformFactory factory = new TextPlatformFactory();
            factory.setInstances(1);
            col.add((T) factory);
        } else if (baseClass == GrammarIdentifier.class) {
            final GrammarIdentifier identifier = new SrgsXmlGrammarIdentifier();
            col.add((T) identifier);
        } else if (baseClass == Profile.class) {
            final VoiceXml21Profile profile = new VoiceXml21Profile();
            try {
                profile.setInitializationTagStrategyFactory(new EmbeddedTagStrategyFactory());
                profile.setTagStrategyFactory(new EmbeddedTagStrategyFactory());
            } catch (Exception e) {
                LOGGER.error("Failed to initialize profile tag strategy factory:", e);
            }
            col.add((T) profile);
        } else if (baseClass == DataModel.class) {
            final EcmaScriptDataModel model = new EcmaScriptDataModel();
            col.add((T) model);
        }
        return col;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T loadObject(final Class<T> baseClass, final String key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T loadObject(final Class<T> baseClass) {
        if (baseClass == TagStrategyFactory.class) {
            try {
                return (T) new EmbeddedTagStrategyFactory();
            } catch (Exception e) {
                return null;
            }
        } else if (baseClass == TagStrategyFactory.class) {
            return (T) new VoiceXml21TagStrategyFactory();
        } else if (baseClass == DocumentServer.class) {
            final JVoiceXmlDocumentServer server =
                    new JVoiceXmlDocumentServer();
            server.addSchemeStrategy(new HttpSchemeStrategy());
            server.addSchemeStrategy(new FileSchemeStrategy());
            return (T) server;
        } else if (baseClass == ImplementationPlatformFactory.class) {
            final JVoiceXmlImplementationPlatformFactory factory = new JVoiceXmlImplementationPlatformFactory();
            try {
                factory.init(this);
            } catch (ConfigurationException e) {
                LOGGER.warn("Failed to initialize JVoiceXmlImplementationPlatformFactory");
            }
            return (T) factory;
        } else if (baseClass == SpeechRecognizerProperties.class) {
            return (T) new SpeechRecognizerProperties();
        } else if (baseClass == DtmfRecognizerProperties.class) {
            final DtmfRecognizerProperties props = new DtmfRecognizerProperties();
            props.setInterdigittimeout("1s");
            return (T) props;
        } else if (baseClass == DialogFactory.class) {
            final JVoiceXmlDialogFactory factory = new JVoiceXmlDialogFactory();
            factory.addDialogMapping(Form.TAG_NAME, new ExecutablePlainForm());

            final ExecutableMenuForm executableMenuForm = new ExecutableMenuForm();
            final ChoiceConverter choiceConverter = new SrgsXmlChoiceConverter();
            executableMenuForm.setChoiceConverter(choiceConverter);
            factory.addDialogMapping(Menu.TAG_NAME, executableMenuForm);
            return (T) factory;
        } else if (baseClass == GrammarProcessor.class) {
            return (T) new JVoiceXmlGrammarProcessor();
        } else if (baseClass == BufferedDtmfInput.class) {
            return (T) new BufferedDtmfInput();
        }
        return null;
    }
}

/*
Copyright (C) 2013 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.irscrutinizer.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.girr.RemoteSet;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class imports Girr files. Only Girr files having remotes as root element are presently supported.
 *
 */
public class GirrImporter extends RemoteSetImporter implements IReaderImporter {
    public static final String homeUrl = "http://www.harctoolbox.org/girr";

    private transient Schema schema;
    private URL url;
    private boolean validate;

    public GirrImporter(boolean validate, URL url) {
        super();
        schema = null;
        this.url = url;
        this.validate = validate;
    }

    /**
     * @return the schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * @param url the url to set
     */

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }


    private void loadSchema() throws SAXException {
        if (validate && schema == null && url != null)
            schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url);
    }

    private void load(Document doc, String origin) throws ParseException, NotGirrRemoteSetException {
        prepareLoad(origin);
        remoteSet = null;
        loadIncremental(doc, origin);
        setupCommands();
    }

    private void loadIncremental(Document doc, String origin) throws ParseException, NotGirrRemoteSetException {
        if (!doc.getDocumentElement().getTagName().equals("remotes")) {
            throw new NotGirrRemoteSetException();
        }
        RemoteSet rs = new RemoteSet(doc);
        if (remoteSet == null)
            remoteSet = rs;
        else
            remoteSet.append(rs);
    }

    /**
     *
     * @param inputStream
     * @param origin
     * @param charsetName ignored; taken from input file encoding.
     * @throws IOException
     * @throws ParseException
     */
    @Override
    public void load(InputStream inputStream, String origin, String charsetName /* ignored */) throws IOException, ParseException {
        try {
            loadSchema();
            load(XmlUtils.openXmlStream(inputStream, validate ? schema : null, false, false), origin);
        } catch (SAXParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getLineNumber());
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        } catch (NotGirrRemoteSetException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    public void load(InputStream inputStream, String origin) throws IOException, ParseException {
        load(inputStream, origin, null);
    }


    @Override
    public void load(Reader reader, String origin) throws IOException, ParseException {
        try {
            loadSchema();
            load(XmlUtils.openXmlReader(reader, validate ? schema : null, false, false), origin);
        } catch (SAXParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getLineNumber());
        } catch (SAXException | NotGirrRemoteSetException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     *
     * @param file
     * @param origin
     * @param charsetName ignored, instead taken from file encoding field.
     * @throws IOException
     */
    @Override
    public void load(File file, String origin, String charsetName /* ignored */) throws IOException {
        try {
            loadSchema();
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
        prepareLoad(origin);
        remoteSet = null;
        loadRecursive(file, origin);
        setupCommands();
    }

    private void loadRecursive(File fileOrDirectory, String origin) throws IOException {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")
                        || file.getName().endsWith(".gif") || file.getName().endsWith(".html")) {
                    continue;
                }
                loadRecursive(file, file.getCanonicalPath());
            }
        } else {
            try {
                loadIncremental(XmlUtils.openXmlFile(fileOrDirectory, validate ? schema : null, true, true), origin);
            } catch (ParseException | SAXException | NotGirrRemoteSetException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    @Override
    public boolean canImportDirectories() {
        return true;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{new String[]{"Girr files (*.girr)", "girr" }, new String[]{"XML files (*.xml)", "xml" }};
    }

    @Override
    public String getFormatName() {
        return "Girr";
    }
    private static class NotGirrRemoteSetException extends Exception {
        NotGirrRemoteSetException() {
            super("Not a Girr file with root element \"remotes\".");
        }
    }
}

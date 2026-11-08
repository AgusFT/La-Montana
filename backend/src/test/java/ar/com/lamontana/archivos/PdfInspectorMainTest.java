package ar.com.lamontana.archivos;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.HexFormat;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the real isolated inspector, including rendering and its rejection exit codes. */
class PdfInspectorMainTest {
    @TempDir Path directory;
    private interface Edit { void apply(PDDocument document) throws Exception; }
    private int inspect(Edit edit) throws Exception {
        Path input=directory.resolve("input.pdf"),output=directory.resolve("pages.txt");
        try(var doc=new PDDocument()) {
            doc.addPage(new PDPage());edit.apply(doc);doc.save(input.toFile());
        }
        return new InspectorPdf("").inspeccionar(input,output);
    }
    private COSDictionary credentials(PDDocument doc) throws IOException {
        byte[] bytes=HexFormat.of().parseHex("0000002e6a756d620000001e6a756d646332706100110010800000aa00389b710363327061000000000866726565");
        COSStream stream=doc.getDocument().createCOSStream();
        stream.setName(COSName.TYPE,"EmbeddedFile");stream.setName(COSName.SUBTYPE,"application/c2pa");
        try(var out=stream.createOutputStream()){out.write(bytes);}
        var files=new COSDictionary();files.setItem(COSName.F,stream);
        var spec=new COSDictionary();spec.setName(COSName.TYPE,"Filespec");
        spec.setName(COSName.getPDFName("AFRelationship"),"C2PA_Manifest");spec.setItem(COSName.EF,files);
        spec.setString(COSName.F,"Content Credentials");
        var attached=new COSArray();attached.add(spec);doc.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("AF"),attached);
        var names=new COSArray();names.add(new COSString("Content Credentials"));names.add(spec);
        var tree=new COSDictionary();tree.setItem(COSName.NAMES,names);
        var catalogNames=new COSDictionary();catalogNames.setItem(COSName.getPDFName("EmbeddedFiles"),tree);
        doc.getDocumentCatalog().getCOSObject().setItem(COSName.NAMES,catalogNames);
        return spec;
    }
    private COSStream stream(COSDictionary spec){return (COSStream)((COSDictionary)spec.getDictionaryObject(COSName.EF)).getDictionaryObject(COSName.F);}
    private void rejects(Edit edit) {
        var error=assertThrows(InspectorPdf.Rechazado.class,()->inspect(edit));
        assertEquals("CONTENIDO_ACTIVO",error.codigo);
    }
    @Test void acceptsOrdinaryPdf()throws Exception{assertEquals(1,inspect(doc->{}));}
    @Test void acceptsPassiveContentCredentialsInNamesAndAssociatedFiles()throws Exception{assertEquals(1,inspect(this::credentials));}
    @Test void acceptsLocalOpeningDestinationAndWebLinks()throws Exception{
        assertEquals(1,inspect(doc->{
            credentials(doc);var destination=new COSArray();destination.add(doc.getPage(0));destination.add(COSName.getPDFName("Fit"));
            var root=doc.getDocumentCatalog().getCOSObject();root.setItem(COSName.OPEN_ACTION,destination);
            var link=new COSDictionary();link.setName(COSName.S,"URI");link.setString(COSName.getPDFName("URI"),"https://example.org");root.setItem(COSName.getPDFName("TestLink"),link);
        }));
    }
    @Test void rejectsOrdinaryAttachmentsEvenWhenNamedContentCredentials(){rejects(doc->stream(credentials(doc)).setName(COSName.SUBTYPE,"application/octet-stream"));}
    @Test void rejectsManifestWithoutProvenanceRelationship(){rejects(doc->credentials(doc).removeItem(COSName.getPDFName("AFRelationship")));}
    @Test void rejectsFakeC2paPayload(){rejects(doc->{try(var out=stream(credentials(doc)).createOutputStream()){out.write("not a manifest".getBytes());}});}
    @Test void rejectsTruncatedJumbfContainer(){rejects(doc->{var s=stream(credentials(doc));byte[] bytes;try(var in=s.createInputStream()){bytes=in.readAllBytes();}ByteBuffer.wrap(bytes).putInt(bytes.length+1);try(var out=s.createOutputStream()){out.write(bytes);}});}
    @Test void rejectsJavascriptAlongsideValidCredentials(){rejects(doc->{credentials(doc);var action=new COSDictionary();action.setName(COSName.S,"JavaScript");action.setString(COSName.getPDFName("JS"),"app.alert('x')");doc.getDocumentCatalog().getCOSObject().setItem(COSName.OPEN_ACTION,action);});}
    @Test void rejectsAdditionalActionsAlongsideValidCredentials(){rejects(doc->{credentials(doc);doc.getPage(0).getCOSObject().setItem(COSName.getPDFName("AA"),new COSDictionary());});}
    @Test void rejectsFileAttachmentAnnotations(){rejects(doc->{credentials(doc);var annotation=new COSDictionary();annotation.setName(COSName.SUBTYPE,"FileAttachment");var annotations=new COSArray();annotations.add(annotation);doc.getPage(0).getCOSObject().setItem(COSName.ANNOTS,annotations);});}
    @Test void rejectsExternalLaunchActions(){rejects(doc->{credentials(doc);var action=new COSDictionary();action.setName(COSName.S,"Launch");doc.getDocumentCatalog().getCOSObject().setItem(COSName.OPEN_ACTION,action);});}
    @Test void rejectsUnassociatedEmbeddedStream(){rejects(doc->{var spec=credentials(doc);var root=doc.getDocumentCatalog().getCOSObject();root.removeItem(COSName.NAMES);root.removeItem(COSName.getPDFName("AF"));root.setItem(COSName.getPDFName("UnexpectedStream"),stream(spec));});}
}

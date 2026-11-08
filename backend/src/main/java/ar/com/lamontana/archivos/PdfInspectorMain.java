//#region ENCABEZADO · PdfInspectorMain.java
/*
 * ========================================================================
 * ARCHIVO: PdfInspectorMain.java
 * ========================================================================
 * FUNCIÓN
 * Es el punto de entrada del proceso aislado que analiza PDF con PDFBox o renderiza una página.
 * Rechaza documentos cifrados, contenido activo y límites inválidos mediante códigos de salida.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [paquete] Rechazo :: Rechazo(int code)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public, static] void main(String[] args)
 * - [private, static] void render(PDDocument doc, PDFRenderer renderer, int index, int longest,
 *   Path output) throws IOException, Rechazo
 * - [private, static] void inspectObjects(PDDocument document) throws IOException, Rechazo
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PdfInspectorMain (clase).
 * - PdfInspectorMain.Rechazo (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.archivos;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.*;

/** Entry point without Spring. The parent bounds memory, runtime and concurrent processes. */
public final class PdfInspectorMain {
    private static final Set<String> UNSAFE_KEYS=Set.of("JS","JavaScript","AA","XFA","RichMediaContent","RichMediaSettings");
    private static final Set<String> UNSAFE_ACTIONS=Set.of("JavaScript","Launch","SubmitForm","ImportData","GoToR","GoToE","Rendition","Movie","Sound");
    public static void main(String[] args) {
        int result=22;
        try {
            Path input=Path.of(args[1]), output=Path.of(args[2]);
            var parser=new PDFParser(new RandomAccessReadBufferedFile(input));
            try(PDDocument document=parser.parse(false)) {
                if(document.isEncrypted())throw new Rechazo(24);
                int pages=document.getNumberOfPages();if(pages<1||pages>10000)throw new Rechazo(25);
                inspectObjects(document);
                PDFRenderer renderer=new PDFRenderer(document);renderer.setSubsamplingAllowed(true);
                if(args[0].equals("inspect")) {
                    for(int page=0;page<pages;page++) render(document,renderer,page,96,null);
                    Files.writeString(output,Integer.toString(pages),StandardOpenOption.CREATE_NEW);
                } else if(args[0].equals("preview")) {
                    int page=Integer.parseInt(args[3]);if(page<0||page>=pages)throw new Rechazo(25);
                    render(document,renderer,page,1200,output);
                } else throw new Rechazo(25);
                result=0;
            }
        } catch(InvalidPasswordException e){result=24;}
        catch(Rechazo e){result=e.code;}
        catch(Exception|OutOfMemoryError e){result=22;}
        System.exit(result);
    }
    private static void render(PDDocument doc,PDFRenderer renderer,int index,int longest,Path output)throws IOException,Rechazo{
        var page=doc.getPage(index);var box=page.getCropBox();float width=box.getWidth(),height=box.getHeight();
        if(!Float.isFinite(width)||!Float.isFinite(height)||width<=0||height<=0||width>14400||height>14400||page.getUserUnit()!=1)throw new Rechazo(25);
        var image=renderer.renderImage(index,Math.min(2f,longest/Math.max(width,height)),ImageType.RGB,RenderDestination.PRINT);
        try {if(output!=null&&!ImageIO.write(image,"png",output.toFile()))throw new IOException();}
        finally {image.flush();}
    }
    private static void inspectObjects(PDDocument document)throws IOException,Rechazo{
        var seen=Collections.newSetFromMap(new IdentityHashMap<COSBase,Boolean>());var queue=new ArrayDeque<COSBase>();
        var manifests=Collections.newSetFromMap(new IdentityHashMap<COSStream,Boolean>());
        var embedded=Collections.newSetFromMap(new IdentityHashMap<COSStream,Boolean>());
        queue.add(document.getDocument().getTrailer());long decoded=0;byte[] buffer=new byte[8192];
        while(!queue.isEmpty()) {
            COSBase obj=queue.removeFirst();if(!seen.add(obj))continue;if(seen.size()>300000)throw new Rechazo(25);
            if(obj instanceof COSObject ref){if(ref.getObject()!=null)queue.add(ref.getObject());}
            else if(obj instanceof COSArray array){for(COSBase v:array)if(v!=null)queue.add(v);}
            else if(obj instanceof COSDictionary dict){
                for(COSName key:dict.keySet())if(UNSAFE_KEYS.contains(key.getName()))throw new Rechazo(23);
                if(dict.containsKey(COSName.EF))allowContentCredentials(dict,manifests);
                COSBase opening=dict.getDictionaryObject(COSName.OPEN_ACTION);
                if(opening!=null && !(opening instanceof COSArray) && !(opening instanceof COSDictionary action && "GoTo".equals(action.getNameAsString(COSName.S))))throw new Rechazo(23);
                if(UNSAFE_ACTIONS.contains(dict.getNameAsString(COSName.S,""))||"FileAttachment".equals(dict.getNameAsString(COSName.SUBTYPE)))throw new Rechazo(23);
                boolean attached="EmbeddedFile".equals(dict.getNameAsString(COSName.TYPE));
                if(attached&&!(dict instanceof COSStream))throw new Rechazo(23);
                if(dict instanceof COSStream stream){
                    if(stream.containsKey(COSName.F))throw new Rechazo(23);
                    try(InputStream in=stream.createInputStream()){
                        long size=0,expected=0;
                        if(attached){
                            embedded.add(stream);
                            if(!"application/c2pa".equals(stream.getNameAsString(COSName.SUBTYPE)))throw new Rechazo(23);
                            byte[] header=in.readNBytes(38);size=header.length;
                            // JUMBF superbox + C2PA description UUID, flags and label. These are
                            // passive provenance bytes, never executed or treated as verified claims.
                            byte[] description=HexFormat.of().parseHex("6a756d646332706100110010800000aa00389b71036332706100");
                            if(header.length!=38||!Arrays.equals(Arrays.copyOfRange(header,4,8),new byte[]{'j','u','m','b'})||ByteBuffer.wrap(header,8,4).getInt()!=30||!Arrays.equals(Arrays.copyOfRange(header,12,38),description))throw new Rechazo(23);
                            expected=Integer.toUnsignedLong(ByteBuffer.wrap(header,0,4).getInt());
                        }
                        int n;while((n=in.read(buffer))!=-1){size+=n;if(decoded+size>128L*1024*1024)throw new Rechazo(25);}
                        decoded+=size;if(decoded>128L*1024*1024)throw new Rechazo(25);
                        if(attached&&size!=expected)throw new Rechazo(23);
                    }
                }
                for(COSBase v:dict.getValues())if(v!=null)queue.add(v);
            }
        }
        if(!manifests.containsAll(embedded))throw new Rechazo(23);
    }
    private static void allowContentCredentials(COSDictionary spec,Set<COSStream> manifests)throws Rechazo{
        if(!"Filespec".equals(spec.getNameAsString(COSName.TYPE))||!"C2PA_Manifest".equals(spec.getNameAsString(COSName.getPDFName("AFRelationship")))||!(spec.getDictionaryObject(COSName.EF) instanceof COSDictionary files)||files.size()==0)throw new Rechazo(23);
        for(COSName key:files.keySet()){
            if(!Set.of("F","UF").contains(key.getName())||!(files.getDictionaryObject(key) instanceof COSStream stream)||!"EmbeddedFile".equals(stream.getNameAsString(COSName.TYPE))||!"application/c2pa".equals(stream.getNameAsString(COSName.SUBTYPE)))throw new Rechazo(23);
            manifests.add(stream);
        }
    }
    private static final class Rechazo extends Exception {final int code;Rechazo(int code){this.code=code;}}
}

package ar.com.lamontana.identidad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
    private final JavaMailSender sender;
    private final String remitente;
    public CorreoService(JavaMailSender sender, @Value("${lamontana.correo.remitente}") String remitente) {
        this.sender=sender; this.remitente=remitente;
    }
    public void enviarCodigo(String destino, String asunto, String token) {
        var mensaje=new SimpleMailMessage();
        mensaje.setFrom(remitente); mensaje.setTo(destino); mensaje.setSubject("La Montaña · "+asunto);
        mensaje.setText(asunto+"\n\nCódigo de un solo uso:\n"+token+"\n\nCopialo en el formulario de La Montaña. Vence en 15 minutos.\nSi no solicitaste esta operación, ignorá este mensaje.");
        try { sender.send(mensaje); }
        catch (MailException ex) { throw new CorreoNoDisponible(); }
    }
    public static class CorreoNoDisponible extends RuntimeException {
        public CorreoNoDisponible() { super("No se pudo entregar el correo al servidor configurado."); }
    }
}

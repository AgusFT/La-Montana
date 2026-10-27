package ar.com.lamontana.seguridad;

import ar.com.lamontana.identidad.IdentidadService;
import ar.com.lamontana.identidad.UsuarioSesion;
import jakarta.servlet.Filter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Recorre la cadena real de seguridad, no sólo el servicio de recuperación. */
class RecuperacionImpresorasSecurityTest {
    private static final String RUTA="/api/admin/configuracion/borradores/11111111-1111-1111-1111-111111111111/impresoras/recuperar-vigentes";
    private AnnotationConfigWebApplicationContext context;
    private Filter seguridad;

    @TestConfiguration
    @EnableWebSecurity
    static class Dependencias {
        @Bean IdentidadService identidad(){return mock(IdentidadService.class);}
        @Bean JdbcTemplate jdbc(){
            var jdbc=mock(JdbcTemplate.class);
            when(jdbc.queryForObject(contains("SELECT EXISTS"),eq(Boolean.class),anyString(),anyLong())).thenReturn(true);
            when(jdbc.queryForObject(startsWith("SELECT debe_cambiar_contrasena"),eq(Boolean.class),anyString())).thenReturn(false);
            return jdbc;
        }
    }

    @BeforeEach void iniciar(){
        context=new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Dependencias.class,SeguridadConfiguration.class);
        context.refresh();
        seguridad=context.getBean("springSecurityFilterChain",Filter.class);
    }

    @AfterEach void cerrar(){SecurityContextHolder.clearContext();if(context!=null)context.close();}

    @Test void administradorConCsrfAlcanzaLaRecuperacion()throws Exception{
        var sesion=sesion("ADMIN_ADMIN");
        comprobar("POST",RUTA,sesion,token(sesion),200,true);
    }

    @Test void empleadoYClienteNoPuedenRecuperar()throws Exception{
        for(String rol:new String[]{"EMPLEADO","CLIENTE"}){
            var sesion=sesion(rol);
            comprobar("POST",RUTA,sesion,token(sesion),403,false);
        }
    }

    @Test void sinSesionNoPuedeRecuperarAunqueTengaCsrf()throws Exception{
        var sesion=new MockHttpSession(context.getServletContext());
        comprobar("POST",RUTA,sesion,token(sesion),401,false);
    }

    @Test void administradorSinCsrfOTokenInvalidoNoPuedeRecuperar()throws Exception{
        comprobar("POST",RUTA,sesion("ADMIN_ADMIN"),null,403,false);
        var sesion=sesion("ADMIN_ADMIN");token(sesion);
        comprobar("POST",RUTA,sesion,"token-invalido",403,false);
    }

    @Test void noHabilitaOtrosMetodosNiRutasVecinas()throws Exception{
        for(String metodo:new String[]{"GET","DELETE","PATCH"}){
            var sesion=sesion("ADMIN_ADMIN");
            comprobar(metodo,RUTA,sesion,token(sesion),403,false);
        }
        var sesion=sesion("ADMIN_ADMIN");
        comprobar("POST",RUTA+"-otra",sesion,token(sesion),403,false);
    }

    @Test void sesionRevocadaSigueBloqueada()throws Exception{
        var sesion=sesion("ADMIN_ADMIN");var csrf=token(sesion);
        when(context.getBean(JdbcTemplate.class).queryForObject(contains("SELECT EXISTS"),eq(Boolean.class),anyString(),anyLong())).thenReturn(false);
        comprobar("POST",RUTA,sesion,csrf,401,false);
    }

    private MockHttpSession sesion(String rol){
        var usuario=new UsuarioSesion(rol.toLowerCase()+"@example.test","hash-no-utilizado",rol,true,1L);
        var auth=UsernamePasswordAuthenticationToken.authenticated(usuario,null,usuario.getAuthorities());
        var securityContext=SecurityContextHolder.createEmptyContext();securityContext.setAuthentication(auth);
        var sesion=new MockHttpSession(context.getServletContext());
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,securityContext);
        return sesion;
    }

    private MockHttpServletRequest request(String metodo,String ruta,MockHttpSession sesion){
        var request=new MockHttpServletRequest(context.getServletContext(),metodo,ruta);
        request.setServletPath(ruta);request.setSession(sesion);return request;
    }

    private String token(MockHttpSession sesion)throws Exception{
        var resultado=new AtomicReference<String>();
        seguridad.doFilter(request("GET","/api/auth/csrf",sesion),new MockHttpServletResponse(),(req,res)->{
            resultado.set(((CsrfToken)req.getAttribute(CsrfToken.class.getName())).getToken());
        });
        assertThat(resultado.get()).isNotBlank();return resultado.get();
    }

    private void comprobar(String metodo,String ruta,MockHttpSession sesion,String token,int estado,boolean pasa)throws Exception{
        var request=request(metodo,ruta,sesion);if(token!=null)request.addHeader("X-CSRF-TOKEN",token);
        var response=new MockHttpServletResponse();var invocada=new AtomicBoolean();
        seguridad.doFilter(request,response,(req,res)->invocada.set(true));
        assertThat(response.getStatus()).as("%s %s: %s",metodo,ruta,response.getContentAsString()).isEqualTo(estado);
        assertThat(invocada.get()).isEqualTo(pasa);
    }
}

package ar.com.lamontana.configuracion;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ar.com.lamontana.organizacion.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class CasaMatrizOrganizacionTest {
    @Test void bajaLogicaNoPuedeDesactivarMatrizVigenteOProgramada(){
        UUID codigo=UUID.randomUUID();
        var ficha=new SucursalController.Sucursal(codigo,"MATRIZ","Casa matriz","Calle","1","Ciudad","Buenos Aires","1234",null,null,"UTC","ACTIVA",3L,List.of(),false,true);
        JdbcTemplate jdbc=mock(JdbcTemplate.class,call->{
            if(call.getMethod().getName().equals("query"))return List.of(ficha);
            if(call.getMethod().getName().equals("queryForObject")&&call.getArgument(1)==Boolean.class)return true;
            return RETURNS_DEFAULTS.answer(call);
        });
        var servicio=new OrganizacionService(jdbc,mock(PasswordEncoder.class),mock(ar.com.lamontana.identidad.CredencialService.class));
        assertThatThrownBy(()->servicio.eliminarSucursal(codigo,3L,"admin")).hasMessageContaining("casa matriz vigente o programada");
        verify(jdbc,never()).update(anyString(),any(Object[].class));
        verify(jdbc).execute("SELECT pg_advisory_xact_lock(764003)");
        verify(jdbc).execute("SELECT pg_advisory_xact_lock(764001)");
    }
}

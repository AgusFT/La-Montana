package ar.com.lamontana.mobile

import ar.com.lamontana.mobile.model.MobileDestination
import ar.com.lamontana.mobile.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileRoutingTest {
    @Test
    fun pointAccountOpensPointDesk() {
        assertEquals(MobileDestination.POINT, destinationFor(profile(point = true)))
    }

    @Test
    fun regularEmployeeDoesNotBecomeRiderAutomatically() {
        assertEquals(MobileDestination.RIDER_PENDING, destinationFor(profile()))
    }

    @Test
    fun initialPasswordMustBeChangedOnWeb() {
        assertEquals(MobileDestination.UNSUPPORTED, destinationFor(profile(changePassword = true, point = true)))
    }

    private fun profile(changePassword: Boolean = false, point: Boolean = false) = Profile(
        nombre = "Persona",
        apellido = "Prueba",
        correo = "persona@prueba.local",
        rol = "EMPLEADO",
        debeCambiarContrasena = changePassword,
        puntoRetiro = point,
    )
}

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
    fun employeeChecksBranchPermissionsInsteadOfBecomingRider() {
        assertEquals(MobileDestination.BRANCH, destinationFor(profile()))
    }

    @Test
    fun enabledRiderOpensAssignedRouteExperience() {
        assertEquals(MobileDestination.RIDER, destinationFor(profile(rider = true)))
    }

    @Test
    fun initialPasswordMustBeChangedOnWeb() {
        assertEquals(MobileDestination.UNSUPPORTED, destinationFor(profile(changePassword = true, point = true)))
    }

    @Test fun administratorCanUseBranchDesk() {
        assertEquals(MobileDestination.BRANCH, destinationFor(profile().copy(rol = "ADMIN_ADMIN")))
    }

    @Test fun customerDoesNotEnterInternalDesk() {
        assertEquals(MobileDestination.UNSUPPORTED, destinationFor(profile().copy(rol = "CLIENTE")))
    }

    @Test fun pointKeepsItsOwnDeskEvenWithOtherFlags() {
        assertEquals(MobileDestination.POINT, destinationFor(profile(point = true, rider = true)))
    }

    private fun profile(changePassword: Boolean = false, point: Boolean = false, rider: Boolean = false) = Profile(
        nombre = "Persona",
        apellido = "Prueba",
        correo = "persona@prueba.local",
        rol = "EMPLEADO",
        debeCambiarContrasena = changePassword,
        puntoRetiro = point,
        repartidor = rider,
    )
}

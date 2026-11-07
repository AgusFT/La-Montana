package ar.com.lamontana.mobile

import ar.com.lamontana.mobile.model.MobileDestination
import ar.com.lamontana.mobile.model.Profile

// El acceso a sucursal se comprueba después con los permisos reales del servidor.
internal fun destinationFor(profile: Profile): MobileDestination = when {
    profile.debeCambiarContrasena -> MobileDestination.UNSUPPORTED
    profile.rol == "ADMIN_ADMIN" -> MobileDestination.BRANCH
    profile.puntoRetiro -> MobileDestination.POINT
    profile.repartidor -> MobileDestination.RIDER
    profile.rol in listOf("ADMIN_ADMIN", "EMPLEADO") -> MobileDestination.BRANCH
    else -> MobileDestination.UNSUPPORTED
}

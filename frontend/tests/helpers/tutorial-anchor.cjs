// Comprueba lo que el usuario ve antes de mover el puntero o desplazar la página.
async function expectTutorialAnchor(page, target, { group = false } = {}) {
  const { expect } = require("@playwright/test");
  await expect(target).toBeVisible();
  await expect
    .poll(
      () =>
        target.evaluate((control, allowGroup) => {
          const card = document.querySelector(".tutorial-callout");
          const halo = document.querySelector(".tutorial-highlight");
          const line = document.querySelector(".tutorial-arrow line");
          const anchor = document.querySelector(
            '[aria-describedby~="tutorial-indicacion"]',
          );
          if (!card || !halo || !line || !anchor)
            return "Falta el cuadro, la luz, la flecha o su destino.";
          if (anchor !== control && !(allowGroup && anchor.contains(control)))
            return (
              "La flecha describe otro elemento: " +
              anchor.outerHTML.slice(0, 180)
            );
          const field = control.getBoundingClientRect(),
            light = halo.getBoundingClientRect();
          const box = card.getBoundingClientRect(),
            to = anchor.getBoundingClientRect();
          const x = field.left + field.width / 2,
            y = field.top + field.height / 2;
          if (
            field.left < -1 ||
            field.right > innerWidth + 1 ||
            field.top < -1 ||
            field.bottom > innerHeight + 1
          )
            return "El control queda fuera de pantalla.";
          if (
            light.left > field.left + 2 ||
            light.right < field.right - 2 ||
            light.top > field.top + 2 ||
            light.bottom < field.bottom - 2
          )
            return "El control no está iluminado por completo.";
          if (
            box.left < -1 ||
            box.top < -1 ||
            box.right > innerWidth + 1 ||
            box.bottom > innerHeight + 1
          )
            return "El cuadro queda fuera de pantalla.";
          if (
            box.left < field.right &&
            box.right > field.left &&
            box.top < field.bottom &&
            box.bottom > field.top
          )
            return "El cuadro tapa el control.";
          const svg = line.ownerSVGElement.getBoundingClientRect();
          const tx = svg.x + line.x2.baseVal.value,
            ty = svg.y + line.y2.baseVal.value;
          if (
            tx < to.left - 10 ||
            tx > to.right + 10 ||
            ty < to.top - 10 ||
            ty > to.bottom + 10
          )
            return "La punta de la flecha termina fuera de su destino.";
          const hit = document.elementFromPoint(x, y);
          if (
            !control.contains(hit) &&
            !(hit instanceof HTMLLabelElement && hit.control === control)
          )
            return (
              "El puntero alcanzaría otro elemento: " +
              (hit?.outerHTML ?? "").slice(0, 180)
            );
          return "ok";
        }, group),
      { message: "La indicación debe señalar y dejar libre el control real." },
    )
    .toBe("ok");
}
module.exports = { expectTutorialAnchor };

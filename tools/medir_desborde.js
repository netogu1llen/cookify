const R = [];
document.querySelectorAll('.hoja, .lamina').forEach((h, i) => {
  const esHoja = h.classList.contains('hoja');
  const limite = h.getBoundingClientRect().bottom - (esHoja ? 0.62 : 0.34) * 96;
  let peor = 0, culpable = '';
  h.querySelectorAll('*').forEach(e => {
    // el pie y el encabezado corridos viven a proposito en la zona baja
    if (e.closest('.folio, .pie, .corrido, .halo')) return;
    const c = e.getBoundingClientRect();
    if (c.height === 0) return;
    const exceso = c.bottom - limite;
    if (exceso > peor) { peor = exceso; culpable = e.tagName + '.' + (e.className || '').split(' ')[0]; }
  });
  if (peor > 2) R.push(`p${i + 1}: +${Math.round(peor)}px (${culpable})`);
});
document.title = R.length ? 'DESBORDA >> ' + R.join(' | ') : 'TODO CABE';

# -*- coding: utf-8 -*-
"""Vista previa de los glifos en HTML, para revisarlos sin recompilar la app."""
import io
from glifos import GLIFOS, PESCADO

todos = dict(GLIFOS); todos['pescado'] = PESCADO
orden = ['pollo','vacuno','cerdo','pescado','marisco','huevo',
         'legumbre','verdura','pasta','arroz','queso','tofu']

celdas = []
for n in orden:
    d, eo = todos[n]
    regla = 'evenodd' if eo else 'nonzero'
    celdas.append(
        '<figure><svg viewBox="0 0 24 24" width="120" height="120">'
        '<path d="%s" fill="#E6E1E5" fill-rule="%s"/></svg>'
        '<svg viewBox="0 0 24 24" width="34" height="34">'
        '<path d="%s" fill="#E6E1E5" fill-rule="%s"/></svg>'
        '<figcaption>%s</figcaption></figure>' % (d, regla, d, regla, n))

io.open('previa.html','w',encoding='utf-8').write(
    '<style>body{background:#191920;margin:0;padding:16px;'
    'font:13px system-ui;color:#9b96a0}'
    'main{display:grid;grid-template-columns:repeat(6,1fr);gap:8px}'
    'figure{margin:0;text-align:center}'
    'figcaption{margin-top:2px}</style><main>' + ''.join(celdas) + '</main>')
print('ok')
